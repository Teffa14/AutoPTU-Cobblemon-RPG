package io.autoptu.cobblemon.battlecore;

/**
 * Adapter-neutral identity binding between one authoritative battle combatant and one presentation entity.
 * The presentation entity identifier is opaque to the battle integration and carries no PTU authority.
 */
public record PresentationEntityBinding(String combatantId, String presentationEntityId) {
    public PresentationEntityBinding {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId is required");
        }
        if (presentationEntityId == null || presentationEntityId.isBlank()) {
            throw new IllegalArgumentException("presentationEntityId is required");
        }
        combatantId = combatantId.strip();
        presentationEntityId = presentationEntityId.strip();
    }
}
