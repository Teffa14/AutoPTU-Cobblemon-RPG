package io.autoptu.cobblemon.battlecore;

/**
 * Adapter-neutral presentation command bound to the one entity registered for its authoritative combatant subject.
 * The opaque presentation entity identifier carries no PTU authority and cannot replace the command subject.
 */
public record EntityBoundBattlePresentationCommand(
        BattlePresentationCommand command,
        String presentationEntityId
) implements EntityBoundPresentationOutput {
    public EntityBoundBattlePresentationCommand {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (presentationEntityId == null || presentationEntityId.isBlank()) {
            throw new IllegalArgumentException("presentationEntityId is required");
        }
        presentationEntityId = presentationEntityId.strip();
    }

    @Override
    public long sequence() {
        return command.sequence();
    }

    @Override
    public int ordinal() {
        return command.ordinal();
    }

    public String combatantId() {
        return command.subjectId();
    }
}
