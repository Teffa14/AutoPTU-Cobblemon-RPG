package io.autoptu.cobblemon.authority;

/**
 * Server-resolved PTU usage rule for a canonical Trainer Feature/action.
 *
 * <p>This value is an internal authority input. Network clients must submit only an action/feature
 * selection. Server code must resolve frequency and maxUses from trusted PTU data before creating
 * this rule.</p>
 */
public record CanonicalTrainerActionRule(
        String actionId,
        TrainerActionFrequency frequency,
        int maxUses
) {
    public CanonicalTrainerActionRule {
        if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId must not be blank");
        actionId = actionId.trim();
        if (frequency == null) throw new IllegalArgumentException("frequency is required");
        if (maxUses < 1) throw new IllegalArgumentException("maxUses must be positive");
    }
}
