package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only server-owned projection of canonical RPG inventory that is currently available to use.
 *
 * <p>The query intentionally reads only durable canonical item stacks. Reserved stacks are omitted
 * by the repository, so callers never present inventory locked by an in-flight authoritative
 * transaction as spendable. Minecraft and Cobblemon inventories are not consulted.</p>
 */
public final class CanonicalBagQueryService {
    private final FileCanonicalItemReservationRepository itemRepository;
    private final Set<String> authoredTemplates;

    public CanonicalBagQueryService(
            FileCanonicalItemReservationRepository itemRepository,
            WorldTaskCatalogue catalogue
    ) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
        Objects.requireNonNull(catalogue, "catalogue");
        TreeSet<String> templates = new TreeSet<>();
        for (WorldTaskRecipeDefinition recipe : catalogue.allRecipes()) {
            recipe.ingredients().forEach(ingredient -> templates.add(ingredient.itemTemplateId()));
            recipe.outputsByQuality().values().forEach(output -> templates.add(output.itemTemplateId()));
        }
        this.authoredTemplates = Set.copyOf(templates);
    }

    public BagSnapshot readAvailable(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId is required");
        }
        String owner = playerId.strip();
        List<Entry> entries = new ArrayList<>();
        for (String templateId : new TreeSet<>(authoredTemplates)) {
            List<CanonicalItemInstance> stacks = itemRepository.findReservableItems(owner, templateId);
            long available = 0;
            for (CanonicalItemInstance stack : stacks) {
                available += stack.quantity();
            }
            if (available <= 0) continue;
            if (available > Integer.MAX_VALUE) {
                throw new IllegalStateException("canonical bag quantity overflow for " + templateId);
            }
            entries.add(new Entry(templateId, (int) available, stacks.size()));
        }
        return new BagSnapshot(owner, List.copyOf(entries));
    }

    public record BagSnapshot(String playerId, List<Entry> entries) {
        public BagSnapshot {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        }

        public boolean empty() {
            return entries.isEmpty();
        }
    }

    public record Entry(String itemTemplateId, int availableQuantity, int stackCount) {
        public Entry {
            if (itemTemplateId == null || itemTemplateId.isBlank()) {
                throw new IllegalArgumentException("itemTemplateId is required");
            }
            if (availableQuantity <= 0) throw new IllegalArgumentException("availableQuantity must be positive");
            if (stackCount <= 0) throw new IllegalArgumentException("stackCount must be positive");
        }
    }
}
