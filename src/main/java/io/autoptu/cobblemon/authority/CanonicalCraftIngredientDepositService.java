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

    public DepositResult deposit(String playerId, String itemTemplateId, int quantity) {
        if (playerId == null || playerId.isBlank()) return DepositResult.invalid("playerId is required");
        if (itemTemplateId == null || itemTemplateId.isBlank()) return DepositResult.invalid("itemTemplateId is required");
        if (quantity <= 0) return DepositResult.invalid("quantity must be positive");

        String owner = playerId.strip();
        String template = itemTemplateId.strip();
        if (!supportedIngredientTemplates.contains(template)) {
            return DepositResult.unsupported(template);
        }

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

    public boolean supports(String itemTemplateId) {
        return itemTemplateId != null && supportedIngredientTemplates.contains(itemTemplateId.strip());
    }

    static String stableStackId(String playerId, String templateId) {
        UUID stable = UUID.nameUUIDFromBytes((playerId + "\u0000" + templateId).getBytes(StandardCharsets.UTF_8));
        return "craft-ingredient-" + stable;
    }

    public record DepositResult(Outcome outcome, String itemTemplateId, int deposited, int canonicalQuantity, String detail) {
        public enum Outcome { APPLIED, UNSUPPORTED, INVALID, CONCURRENT_WRITE }

        static DepositResult applied(String template, int deposited, int canonicalQuantity) {
            return new DepositResult(Outcome.APPLIED, template, deposited, canonicalQuantity, "");
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
            return outcome == Outcome.APPLIED;
        }
    }
}
