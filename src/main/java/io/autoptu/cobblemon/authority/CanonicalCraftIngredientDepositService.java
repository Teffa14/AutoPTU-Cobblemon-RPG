package io.autoptu.cobblemon.authority;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Server-owned bridge that accepts authored crafting ingredients into durable canonical inventory.
 *
 * <p>The caller must establish the acquisition source and quantity from authoritative server state.
 * This service never accepts arbitrary item templates: only ingredient templates present in the
 * server-owned crafting catalogue can enter canonical inventory through this boundary.</p>
 */
public final class CanonicalCraftIngredientDepositService {
    private final FileCanonicalItemReservationRepository itemRepository;
    private final Set<String> supportedIngredientTemplates;

    public CanonicalCraftIngredientDepositService(
            FileCanonicalItemReservationRepository itemRepository,
            WorldTaskCatalogue catalogue
    ) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
        Objects.requireNonNull(catalogue, "catalogue");
        this.supportedIngredientTemplates = catalogue.allRecipes().stream()
                .flatMap(recipe -> recipe.ingredients().stream())
                .map(WorldTaskRecipeDefinition.IngredientRequirement::itemTemplateId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Legacy aggregate-stack deposit used by non-cross-store callers. */
    public DepositResult deposit(String playerId, String itemTemplateId, int quantity) {
        Validation validation = validate(playerId, itemTemplateId, quantity);
        if (validation.result() != null) return validation.result();
        String owner = validation.owner();
        String template = validation.template();

        String itemInstanceId = stableStackId(owner, template);
        for (int attempt = 0; attempt < 8; attempt++) {
            CanonicalItemInstance existing = itemRepository.findItem(itemInstanceId).orElse(null);
            if (existing == null) {
                CanonicalItemInstance initial = new CanonicalItemInstance(
                        itemInstanceId, owner, template, quantity, 0L);
                if (itemRepository.createItemIfAbsent(initial)) {
                    return DepositResult.applied(template, quantity, quantity);
                }
                continue;
            }
            if (!existing.ownerPlayerId().equals(owner) || !existing.templateId().equals(template)) {
                return DepositResult.invalid("stable canonical ingredient identity collision");
            }
            if (existing.quantity() > Integer.MAX_VALUE - quantity) {
                return DepositResult.invalid("canonical ingredient quantity overflow");
            }
            CanonicalItemInstance replacement = new CanonicalItemInstance(
                    existing.itemInstanceId(), existing.ownerPlayerId(), existing.templateId(),
                    existing.quantity() + quantity, existing.revision() + 1);
            if (itemRepository.replaceItemIfRevision(itemInstanceId, existing.revision(), replacement)) {
                return DepositResult.applied(template, quantity, replacement.quantity());
            }
        }
        return DepositResult.concurrent(template);
    }

    /**
     * Applies one durable Minecraft-to-canonical handoff exactly once.
     *
     * <p>The handoff ID becomes the canonical item-instance identity. Retrying the same journal entry
     * therefore observes the already-created item instead of incrementing a shared stack twice. The
     * crafting inventory assessor already aggregates multiple canonical stacks by template, so this
     * remains transparent to recipe eligibility and consumption.</p>
     */
    public DepositResult depositHandoff(
            String handoffId,
            String playerId,
            String itemTemplateId,
            int quantity
    ) {
        if (handoffId == null || handoffId.isBlank()) return DepositResult.invalid("handoffId is required");
        Validation validation = validate(playerId, itemTemplateId, quantity);
        if (validation.result() != null) return validation.result();
        String owner = validation.owner();
        String template = validation.template();
        String itemInstanceId = handoffStackId(handoffId.strip(), owner, template);

        CanonicalItemInstance existing = itemRepository.findItem(itemInstanceId).orElse(null);
        if (existing != null) {
            if (!matchesHandoffItem(existing, owner, template, quantity)) {
                return DepositResult.invalid("canonical craft-deposit handoff identity collision");
            }
            return DepositResult.alreadyApplied(template, quantity, availableQuantity(owner, template));
        }

        CanonicalItemInstance handoffItem = new CanonicalItemInstance(
                itemInstanceId, owner, template, quantity, 0L);
        if (!itemRepository.createItemIfAbsent(handoffItem)) {
            existing = itemRepository.findItem(itemInstanceId).orElse(null);
            if (existing != null && matchesHandoffItem(existing, owner, template, quantity)) {
                return DepositResult.alreadyApplied(template, quantity, availableQuantity(owner, template));
            }
            return DepositResult.concurrent(template);
        }
        return DepositResult.applied(template, quantity, availableQuantity(owner, template));
    }

    /** True when this exact handoff already has a canonical receipt item, even if later consumed. */
    public boolean isHandoffApplied(String handoffId, String playerId, String itemTemplateId, int quantity) {
        if (handoffId == null || handoffId.isBlank()
                || playerId == null || playerId.isBlank()
                || itemTemplateId == null || itemTemplateId.isBlank()
                || quantity <= 0) return false;
        String owner = playerId.strip();
        String template = itemTemplateId.strip();
        CanonicalItemInstance existing = itemRepository.findItem(
                handoffStackId(handoffId.strip(), owner, template)).orElse(null);
        return existing != null && matchesHandoffItem(existing, owner, template, quantity);
    }

    public boolean supports(String itemTemplateId) {
        return itemTemplateId != null && supportedIngredientTemplates.contains(itemTemplateId.strip());
    }

    static String stableStackId(String playerId, String templateId) {
        UUID stable = UUID.nameUUIDFromBytes((playerId + "\u0000" + templateId).getBytes(StandardCharsets.UTF_8));
        return "craft-ingredient-" + stable;
    }

    static String handoffStackId(String handoffId, String playerId, String templateId) {
        UUID stable = UUID.nameUUIDFromBytes(
                (handoffId + "\u0000" + playerId + "\u0000" + templateId).getBytes(StandardCharsets.UTF_8));
        return "craft-deposit-" + stable;
    }

    private static boolean matchesHandoffItem(
            CanonicalItemInstance item,
            String owner,
            String template,
            int originalQuantity
    ) {
        return item.ownerPlayerId().equals(owner)
                && item.templateId().equals(template)
                && item.revision() >= 0L
                && item.quantity() >= 0
                && item.quantity() <= originalQuantity;
    }

    private Validation validate(String playerId, String itemTemplateId, int quantity) {
        if (playerId == null || playerId.isBlank()) return new Validation("", "", DepositResult.invalid("playerId is required"));
        if (itemTemplateId == null || itemTemplateId.isBlank()) return new Validation("", "", DepositResult.invalid("itemTemplateId is required"));
        if (quantity <= 0) return new Validation("", "", DepositResult.invalid("quantity must be positive"));
        String owner = playerId.strip();
        String template = itemTemplateId.strip();
        if (!supportedIngredientTemplates.contains(template)) {
            return new Validation(owner, template, DepositResult.unsupported(template));
        }
        return new Validation(owner, template, null);
    }

    private int availableQuantity(String owner, String template) {
        long total = itemRepository.findReservableItems(owner, template).stream()
                .mapToLong(CanonicalItemInstance::quantity)
                .sum();
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private record Validation(String owner, String template, DepositResult result) {}

    public record DepositResult(Outcome outcome, String itemTemplateId, int deposited, int canonicalQuantity, String detail) {
        public enum Outcome { APPLIED, ALREADY_APPLIED, UNSUPPORTED, INVALID, CONCURRENT_WRITE }

        static DepositResult applied(String template, int deposited, int canonicalQuantity) {
            return new DepositResult(Outcome.APPLIED, template, deposited, canonicalQuantity, "");
        }

        static DepositResult alreadyApplied(String template, int deposited, int canonicalQuantity) {
            return new DepositResult(Outcome.ALREADY_APPLIED, template, deposited, canonicalQuantity, "handoff already applied");
        }

        static DepositResult unsupported(String template) {
            return new DepositResult(Outcome.UNSUPPORTED, template, 0, 0, "item is not an authored crafting ingredient");
        }

        static DepositResult invalid(String detail) {
            return new DepositResult(Outcome.INVALID, "", 0, 0, detail);
        }

        static DepositResult concurrent(String template) {
            return new DepositResult(Outcome.CONCURRENT_WRITE, template, 0, 0, "canonical ingredient inventory changed concurrently");
        }

        public boolean applied() {
            return outcome == Outcome.APPLIED || outcome == Outcome.ALREADY_APPLIED;
        }
    }
}
