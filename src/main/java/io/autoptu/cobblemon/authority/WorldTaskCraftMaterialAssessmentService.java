package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Side-effect-free canonical material assessment for one world-task recipe.
 *
 * <p>This service reads only the durable AutoPTU item store. It does not trust Minecraft inventory,
 * does not reserve ingredients, and does not roll a craft outcome.</p>
 */
public final class WorldTaskCraftMaterialAssessmentService {
    private final FileCanonicalItemReservationRepository items;

    public WorldTaskCraftMaterialAssessmentService(FileCanonicalItemReservationRepository items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    public Assessment assess(String playerId, WorldTaskRecipeDefinition recipe, int craftQuantity) {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
        Objects.requireNonNull(recipe, "recipe");
        if (craftQuantity <= 0) throw new IllegalArgumentException("craftQuantity must be > 0");

        List<IngredientAvailability> ingredients = new ArrayList<>();
        boolean ready = true;
        for (WorldTaskRecipeDefinition.IngredientRequirement requirement : recipe.ingredients()) {
            int required = Math.multiplyExact(requirement.quantity(), craftQuantity);
            int available = 0;
            for (CanonicalItemInstance stack : items.findReservableItems(playerId, requirement.itemTemplateId())) {
                available = Math.addExact(available, stack.quantity());
                if (available >= required) break;
            }
            int missing = Math.max(0, required - available);
            if (missing > 0) ready = false;
            ingredients.add(new IngredientAvailability(
                    requirement.itemTemplateId(),
                    required,
                    Math.min(available, required),
                    missing
            ));
        }
        return new Assessment(ready, List.copyOf(ingredients));
    }

    public record Assessment(boolean ready, List<IngredientAvailability> ingredients) {
        public Assessment {
            ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
        }
    }

    public record IngredientAvailability(String itemTemplateId, int required, int available, int missing) {
        public IngredientAvailability {
            if (itemTemplateId == null || itemTemplateId.isBlank()) {
                throw new IllegalArgumentException("itemTemplateId must not be blank");
            }
            if (required <= 0) throw new IllegalArgumentException("required must be > 0");
            if (available < 0 || available > required) {
                throw new IllegalArgumentException("available must be between 0 and required");
            }
            if (missing != required - available) {
                throw new IllegalArgumentException("missing must equal required - available");
            }
        }
    }
}
