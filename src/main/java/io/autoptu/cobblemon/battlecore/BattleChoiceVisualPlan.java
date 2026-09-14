package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Presentation-only plan for an authoritative battle choice set.
 *
 * <p>The highlighted choice must be one of the choices in the supplied set. The grid window is a
 * bounded visual viewport around server-authored coordinates; cells inside it are not advertised as
 * legal unless they also appear in {@link #shiftDestinations()} or {@link #attackTargets()}.</p>
 */
public record BattleChoiceVisualPlan(
        GridWindow gridWindow,
        Set<BattleGridCoordinate> shiftDestinations,
        Set<BattleGridCoordinate> attackTargets,
        Highlight highlight
) {
    private static final int GRID_PADDING = 1;
    private static final int MAX_GRID_CELLS_PER_AXIS = 12;

    public BattleChoiceVisualPlan {
        shiftDestinations = Set.copyOf(Objects.requireNonNull(shiftDestinations, "shiftDestinations"));
        attackTargets = Set.copyOf(Objects.requireNonNull(attackTargets, "attackTargets"));
    }

    public static BattleChoiceVisualPlan from(BattleCoreLegalChoiceSet choices, String highlightedStableKey) {
        Objects.requireNonNull(choices, "choices");
        String normalizedHighlight = highlightedStableKey == null || highlightedStableKey.isBlank()
                ? null
                : highlightedStableKey.strip();

        LinkedHashSet<BattleGridCoordinate> shifts = new LinkedHashSet<>();
        LinkedHashSet<BattleGridCoordinate> attacks = new LinkedHashSet<>();
        BattleCoreLegalChoice highlighted = null;
        for (BattleCoreLegalChoice choice : choices.choices()) {
            if (choice instanceof BattleCoreLegalChoice.Shift shift) {
                shifts.add(shift.destination());
            } else if (choice instanceof BattleCoreLegalChoice.Move move && hasWorldAnchor(move)) {
                attacks.add(move.targetAnchor());
            }
            if (normalizedHighlight != null && choice.stableKey().equals(normalizedHighlight)) {
                highlighted = choice;
            }
        }

        if (normalizedHighlight != null && highlighted == null) {
            throw new IllegalArgumentException("highlighted choice is not present in the authoritative choice set");
        }
        Highlight highlight = highlighted == null ? null : highlight(highlighted);
        LinkedHashSet<BattleGridCoordinate> allAnchors = new LinkedHashSet<>(shifts);
        allAnchors.addAll(attacks);
        if (highlight != null && highlight.anchor() != null) allAnchors.add(highlight.anchor());

        BattleGridCoordinate focus = highlight != null && highlight.anchor() != null
                ? highlight.anchor()
                : allAnchors.stream().findFirst().orElse(null);
        return new BattleChoiceVisualPlan(
                GridWindow.enclosing(allAnchors, focus, GRID_PADDING, MAX_GRID_CELLS_PER_AXIS),
                shifts,
                attacks,
                highlight
        );
    }

    public BattleChoiceVisualPlan withExecutedHighlight(BattleCoreLegalChoice executed) {
        Objects.requireNonNull(executed, "executed");
        Highlight executedHighlight = highlight(executed);
        LinkedHashSet<BattleGridCoordinate> anchors = new LinkedHashSet<>(shiftDestinations);
        anchors.addAll(attackTargets);
        if (executedHighlight.anchor() != null) anchors.add(executedHighlight.anchor());
        BattleGridCoordinate focus = executedHighlight.anchor() != null
                ? executedHighlight.anchor()
                : anchors.stream().findFirst().orElse(null);
        GridWindow executedWindow = anchors.isEmpty()
                ? null
                : GridWindow.enclosing(anchors, focus, GRID_PADDING, MAX_GRID_CELLS_PER_AXIS);
        return new BattleChoiceVisualPlan(executedWindow, shiftDestinations, attackTargets, executedHighlight);
    }

    private static boolean hasWorldAnchor(BattleCoreLegalChoice.Move move) {
        return move.targetMode() == BattleClientActionRequest.Target.Mode.TILE
                || move.targetMode() == BattleClientActionRequest.Target.Mode.COMBATANT;
    }

    private static Highlight highlight(BattleCoreLegalChoice choice) {
        if (choice instanceof BattleCoreLegalChoice.Shift shift) {
            return new Highlight(HighlightKind.MOVEMENT, shift.destination(), shift.stableKey(), null);
        }
        BattleCoreLegalChoice.Move move = (BattleCoreLegalChoice.Move) choice;
        return new Highlight(
                HighlightKind.ATTACK,
                hasWorldAnchor(move) ? move.targetAnchor() : null,
                move.stableKey(),
                move.moveId()
        );
    }

    public enum HighlightKind { MOVEMENT, ATTACK }

    public record Highlight(
            HighlightKind kind,
            BattleGridCoordinate anchor,
            String stableKey,
            String moveId
    ) {
        public Highlight {
            kind = Objects.requireNonNull(kind, "kind");
            stableKey = normalize(stableKey, "stableKey");
            moveId = moveId == null ? null : normalize(moveId, "moveId");
            if (kind == HighlightKind.MOVEMENT && anchor == null) {
                throw new IllegalArgumentException("movement highlight requires an anchor");
            }
            if (kind == HighlightKind.MOVEMENT && moveId != null) {
                throw new IllegalArgumentException("movement highlight cannot carry moveId");
            }
            if (kind == HighlightKind.ATTACK && moveId == null) {
                throw new IllegalArgumentException("attack highlight requires moveId");
            }
        }
    }

    public record GridWindow(int minX, int maxX, int minY, int maxY) {
        public GridWindow {
            if (maxX < minX || maxY < minY) throw new IllegalArgumentException("invalid grid window");
        }

        public int width() { return maxX - minX + 1; }
        public int height() { return maxY - minY + 1; }

        static GridWindow enclosing(
                Set<BattleGridCoordinate> anchors,
                BattleGridCoordinate focus,
                int padding,
                int maxCellsPerAxis
        ) {
            if (anchors.isEmpty()) return null;
            if (padding < 0 || maxCellsPerAxis < 1) throw new IllegalArgumentException("invalid grid viewport limits");

            int minX = anchors.stream().mapToInt(BattleGridCoordinate::x).min().orElseThrow() - padding;
            int maxX = anchors.stream().mapToInt(BattleGridCoordinate::x).max().orElseThrow() + padding;
            int minY = anchors.stream().mapToInt(BattleGridCoordinate::y).min().orElseThrow() - padding;
            int maxY = anchors.stream().mapToInt(BattleGridCoordinate::y).max().orElseThrow() + padding;
            BattleGridCoordinate viewportFocus = Objects.requireNonNull(focus, "focus");
            int[] x = clamp(minX, maxX, viewportFocus.x(), maxCellsPerAxis);
            int[] y = clamp(minY, maxY, viewportFocus.y(), maxCellsPerAxis);
            return new GridWindow(x[0], x[1], y[0], y[1]);
        }

        private static int[] clamp(int min, int max, int focus, int limit) {
            if (max - min + 1 <= limit) return new int[]{min, max};
            int before = (limit - 1) / 2;
            return new int[]{focus - before, focus - before + limit - 1};
        }
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
