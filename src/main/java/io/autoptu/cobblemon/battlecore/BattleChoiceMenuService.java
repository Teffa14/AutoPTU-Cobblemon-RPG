package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;

/**
 * Read/select boundary for Minecraft battle UI.
 *
 * The service never derives legal actions. It reads one fresh legal-choice snapshot from
 * AutoPTU-Java and executes only the exact choice identified by its authoritative stable key.
 */
public final class BattleChoiceMenuService {
    private final BattleAuthoritativeLegalChoiceSource legalChoiceSource;
    private final BattleAuthoritativeChoiceExecutor executor;

    public BattleChoiceMenuService(
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        this.legalChoiceSource = Objects.requireNonNull(legalChoiceSource, "legalChoiceSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public List<Entry> choices(String reservationId, String actorId) {
        BattleCoreLegalChoiceSet set = legalChoiceSource.legalChoices(reservationId, actorId);
        requireScope(set, reservationId, actorId);
        return set.choices().stream().map(BattleChoiceMenuService::entry).toList();
    }

    public Entry choose(String reservationId, String actorId, String stableKey) {
        BattleCoreLegalChoice selected = requireChoice(reservationId, actorId, stableKey);
        executor.execute(reservationId, selected);
        return entry(selected);
    }

    public BattleCoreLegalChoice requireChoice(String reservationId, String actorId, String stableKey) {
        if (stableKey == null || stableKey.isBlank()) {
            throw new IllegalArgumentException("stableKey must not be blank");
        }
        String normalizedKey = stableKey.strip();
        BattleCoreLegalChoiceSet set = legalChoiceSource.legalChoices(reservationId, actorId);
        requireScope(set, reservationId, actorId);
        BattleCoreLegalChoice selected = set.choices().stream()
                .filter(choice -> choice.stableKey().equals(normalizedKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "choice is no longer legal in the authoritative action space"));
        return selected;
    }

    /** Revalidates every field the player saw, including the anchor, before executing. */
    public Entry chooseExact(String reservationId, String actorId, BattleCoreLegalChoice expected) {
        Objects.requireNonNull(expected, "expected");
        BattleCoreLegalChoice current = requireChoice(reservationId, actorId, expected.stableKey());
        if (!current.equals(expected)) {
            throw new IllegalArgumentException("choice changed after preview; select it again");
        }
        executor.execute(reservationId, current);
        return entry(current);
    }

    private static void requireScope(BattleCoreLegalChoiceSet set, String reservationId, String actorId) {
        Objects.requireNonNull(set, "authoritative legal choice set");
        if (!set.reservationId().equals(reservationId) || !set.actorId().equals(actorId)) {
            throw new IllegalStateException("authoritative legal choice source returned a different battle scope");
        }
    }

    private static Entry entry(BattleCoreLegalChoice choice) {
        if (choice instanceof BattleCoreLegalChoice.Shift shift) {
            return new Entry(shift.stableKey(), "Shift to " + coordinate(shift.destination()),
                    EntryKind.MOVEMENT, shift.destination(), "", "");
        }
        BattleCoreLegalChoice.Move move = (BattleCoreLegalChoice.Move) choice;
        String target = switch (move.targetMode()) {
            case COMBATANT -> "combatant " + move.targetId();
            case TILE -> "tile " + coordinate(move.targetAnchor());
            case SELF -> "self";
            case FIELD -> "field";
        };
        boolean anchored = move.targetMode() == BattleClientActionRequest.Target.Mode.COMBATANT
                || move.targetMode() == BattleClientActionRequest.Target.Mode.TILE;
        return new Entry(move.stableKey(), move.moveId() + " -> " + target, EntryKind.ATTACK,
                anchored ? move.targetAnchor() : null, move.moveId(), move.targetId() == null ? "" : move.targetId());
    }

    private static String coordinate(BattleGridCoordinate coordinate) {
        return coordinate.x() + "," + coordinate.y();
    }

    public enum EntryKind { MOVEMENT, ATTACK, OTHER }

    public record Entry(String choiceId, String label, EntryKind kind, BattleGridCoordinate anchor,
                        String moveId, String targetId) {
        public Entry(String choiceId, String label) {
            this(choiceId, label, EntryKind.OTHER, null, "", "");
        }
        public Entry {
            if (choiceId == null || choiceId.isBlank()) throw new IllegalArgumentException("choiceId must not be blank");
            if (label == null || label.isBlank()) throw new IllegalArgumentException("label must not be blank");
            kind = Objects.requireNonNull(kind, "kind");
            moveId = Objects.requireNonNull(moveId, "moveId");
            targetId = Objects.requireNonNull(targetId, "targetId");
            if (kind == EntryKind.MOVEMENT && anchor == null) throw new IllegalArgumentException("movement needs a destination");
            if (kind == EntryKind.ATTACK && moveId.isBlank()) throw new IllegalArgumentException("attack needs a move identifier");
        }
    }
}
