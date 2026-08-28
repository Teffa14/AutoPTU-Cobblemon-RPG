package io.autoptu.cobblemon.authority;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-owned material/output contract for an Ouros crafting task.
 *
 * <p>Template ids are canonical RPG inventory identifiers. Minecraft/Cobblemon inventory is not
 * trusted as ownership or quantity truth by this contract. A later atomic craft transaction may
 * map supported world materials into canonical inventory only through an explicit server-owned
 * bridge.</p>
 */
public record WorldTaskRecipeDefinition(
        WorldTaskDefinition task,
        String workstationId,
        List<IngredientRequirement> ingredients,
        Map<CraftQuality, CraftOutput> outputsByQuality
) {
    public WorldTaskRecipeDefinition {
        task = Objects.requireNonNull(task, "task");
        workstationId = requireText(workstationId, "workstationId");
        Objects.requireNonNull(ingredients, "ingredients");
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("ingredients must not be empty");
        }
        ingredients = List.copyOf(ingredients);

        Objects.requireNonNull(outputsByQuality, "outputsByQuality");
        EnumMap<CraftQuality, CraftOutput> outputs = new EnumMap<>(CraftQuality.class);
        outputsByQuality.forEach((quality, output) -> outputs.put(
                Objects.requireNonNull(quality, "craft quality"),
                Objects.requireNonNull(output, "craft output")
        ));
        if (outputs.size() != CraftQuality.values().length) {
            throw new IllegalArgumentException("outputsByQuality must define every craft quality");
        }
        for (CraftQuality quality : CraftQuality.values()) {
            if (!outputs.containsKey(quality)) {
                throw new IllegalArgumentException("missing output for quality " + quality);
            }
        }
        outputsByQuality = Map.copyOf(outputs);
    }

    public String taskId() {
        return task.taskId();
    }

    public CraftOutput outputFor(CraftQuality quality) {
        return outputsByQuality.get(Objects.requireNonNull(quality, "quality"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public enum CraftQuality {
        IMPROVISED,
        STANDARD,
        EXCELLENT
    }

    public record IngredientRequirement(String itemTemplateId, int quantity) {
        public IngredientRequirement {
            itemTemplateId = requireText(itemTemplateId, "itemTemplateId");
            if (quantity <= 0) {
                throw new IllegalArgumentException("ingredient quantity must be > 0");
            }
        }
    }

    public record CraftOutput(String itemTemplateId, int quantity) {
        public CraftOutput {
            itemTemplateId = requireText(itemTemplateId, "itemTemplateId");
            if (quantity <= 0) {
                throw new IllegalArgumentException("output quantity must be > 0");
            }
        }
    }
}
