package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts stable upstream semantic event contracts into project-owned rendering commands.
 * No PTU rule is evaluated here: values are copied from the authoritative event stable key.
 */
public final class BattlePresentationProjector {
    public BattlePresentationBatch project(BattlePlaybackBatch batch) {
        if (batch == null) throw new IllegalArgumentException("batch is required");
        ArrayList<BattlePresentationCommand> commands = new ArrayList<>();
        for (BattleEventPlaybackEnvelope event : batch.events()) {
            commands.addAll(project(event));
        }
        return new BattlePresentationBatch(batch.reservationId(), commands);
    }

    public List<BattlePresentationCommand> project(BattleEventPlaybackEnvelope event) {
        if (event == null) throw new IllegalArgumentException("event is required");
        String[] parts = event.stableKey().split("\\|", -1);
        return switch (event.kind()) {
            case "move_resolved" -> projectMove(event.sequence(), parts);
            case "shift_resolved" -> List.of(projectShift(event.sequence(), parts));
            case "status_skip" -> List.of(projectStatusSkip(event.sequence(), parts));
            case "trainer_feature" -> List.of(projectTrainerFeature(event.sequence(), parts));
            case "rule_effect" -> List.of(projectRuleEffect(event.sequence(), parts));
            case "field_effect" -> List.of(projectFieldEffect(event.sequence(), parts));
            case "phase" -> List.of(projectLifecycleCue(event.sequence(), parts, "phase", BattlePresentationCommand.Kind.PHASE_CUE));
            case "turn_start" -> List.of(projectTurnStart(event.sequence(), parts));
            case "turn_end" -> List.of(projectLifecycleCue(event.sequence(), parts, "turn_end", BattlePresentationCommand.Kind.TURN_END_CUE));
            default -> throw new IllegalArgumentException("unsupported battle event kind: " + event.kind());
        };
    }

    private static List<BattlePresentationCommand> projectMove(long sequence, String[] parts) {
        requireParts(parts, 9, "move_resolved");
        boolean hit = parseBoolean(parts[5], "hit");
        boolean crit = parseBoolean(parts[6], "crit");
        int damage = parseNonNegativeInt(parts[7], "damage");
        int targetHp = parseNonNegativeInt(parts[8], "targetHp");
        if (!hit && (crit || damage != 0)) {
            throw new IllegalArgumentException("missed move stable key cannot contain crit or damage");
        }

        BattlePresentationCommand animation = command(
                sequence, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, parts[2],
                data(
                        "source", parts[1],
                        "targetId", required(parts[3], "targetId"),
                        "moveId", required(parts[4], "moveId"),
                        "hit", Boolean.toString(hit),
                        "crit", Boolean.toString(crit)
                )
        );
        BattlePresentationCommand hp = command(
                sequence, 1, BattlePresentationCommand.Kind.HP_PROJECTION, parts[3],
                data("damage", Integer.toString(damage), "targetHp", Integer.toString(targetHp))
        );
        return List.of(animation, hp);
    }

    private static BattlePresentationCommand projectShift(long sequence, String[] parts) {
        requireParts(parts, 4, "shift_resolved");
        requiredGrid(parts[2], "origin");
        requiredGrid(parts[3], "destination");
        return command(
                sequence, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, parts[1],
                data("origin", parts[2], "destination", parts[3])
        );
    }

    private static BattlePresentationCommand projectStatusSkip(long sequence, String[] parts) {
        requireParts(parts, 5, "status_skip");
        return command(
                sequence, 0, BattlePresentationCommand.Kind.STATUS_SKIP_CUE, parts[1],
                data("status", parts[2], "phase", required(parts[3], "phase"), "reason", parts[4])
        );
    }

    private static BattlePresentationCommand projectTrainerFeature(long sequence, String[] parts) {
        requireParts(parts, 7, "trainer_feature");
        int targetHp = parseNonNegativeInt(parts[6], "targetHp");
        return command(
                sequence, 0, BattlePresentationCommand.Kind.TRAINER_FEATURE_CUE, parts[1],
                data(
                        "feature", required(parts[2], "feature"),
                        "effect", required(parts[3], "effect"),
                        "move", parts[4],
                        "status", parts[5],
                        "targetHp", Integer.toString(targetHp)
                )
        );
    }

    private static BattlePresentationCommand projectRuleEffect(long sequence, String[] parts) {
        requireParts(parts, 9, "rule_effect");
        double amount = parseFiniteDouble(parts[7], "amount");
        int actorHp = parseNonNegativeInt(parts[8], "actorHp");
        return command(
                sequence, 0, BattlePresentationCommand.Kind.RULE_EFFECT_CUE, parts[3],
                data(
                        "sourceKind", required(parts[1], "sourceKind"),
                        "sourceName", required(parts[2], "sourceName"),
                        "targetId", parts[4],
                        "moveId", parts[5],
                        "effect", required(parts[6], "effect"),
                        "amount", Double.toString(amount),
                        "actorHp", Integer.toString(actorHp)
                )
        );
    }

    private static BattlePresentationCommand projectFieldEffect(long sequence, String[] parts) {
        requireParts(parts, 5, "field_effect");
        String fieldKind = required(parts[1], "fieldKind");
        if (!fieldKind.equals("terrain") && !fieldKind.equals("zone") && !fieldKind.equals("room")) {
            throw new IllegalArgumentException("fieldKind must be terrain, zone, or room");
        }
        String effectName = required(parts[2], "effectName");
        String effect = required(parts[3], "effect");
        if (!effect.equals(fieldKind + "_ends")) {
            throw new IllegalArgumentException("field effect stable key must describe authoritative expiry");
        }
        int round = parseNonNegativeInt(parts[4], "round");
        return command(
                sequence, 0, BattlePresentationCommand.Kind.FIELD_EFFECT_CUE, fieldKind,
                data(
                        "fieldKind", fieldKind,
                        "effectName", effectName,
                        "effect", effect,
                        "round", Integer.toString(round)
                )
        );
    }

    private static BattlePresentationCommand projectLifecycleCue(
            long sequence,
            String[] parts,
            String kind,
            BattlePresentationCommand.Kind commandKind
    ) {
        requireParts(parts, 4, kind);
        int round = parseNonNegativeInt(parts[1], "round");
        String actorId = required(parts[2], "actorId");
        String phase = required(parts[3], "phase");
        return command(
                sequence,
                0,
                commandKind,
                actorId,
                data("round", Integer.toString(round), "phase", phase)
        );
    }

    private static BattlePresentationCommand projectTurnStart(long sequence, String[] parts) {
        requireParts(parts, 5, "turn_start");
        int round = parseNonNegativeInt(parts[1], "round");
        String actorId = required(parts[2], "actorId");
        String phase = required(parts[3], "phase");
        int initiativeIndex = parseNonNegativeInt(parts[4], "initiativeIndex");
        return command(
                sequence,
                0,
                BattlePresentationCommand.Kind.TURN_START_CUE,
                actorId,
                data(
                        "round", Integer.toString(round),
                        "phase", phase,
                        "initiativeIndex", Integer.toString(initiativeIndex)
                )
        );
    }

    private static BattlePresentationCommand command(
            long sequence,
            int ordinal,
            BattlePresentationCommand.Kind kind,
            String subjectId,
            Map<String, String> data
    ) {
        return new BattlePresentationCommand(sequence, ordinal, kind, required(subjectId, "subjectId"), data);
    }

    private static Map<String, String> data(String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("data requires key/value pairs");
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) data.put(pairs[i], pairs[i + 1]);
        return data;
    }

    private static void requireParts(String[] parts, int expected, String kind) {
        if (parts.length != expected || !kind.equals(parts[0])) {
            throw new IllegalArgumentException("invalid " + kind + " stable key");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static boolean parseBoolean(String value, String field) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(field + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static int parseNonNegativeInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new IllegalArgumentException(field + " cannot be negative");
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " must be an integer", error);
        }
    }

    private static double parseFiniteDouble(String value, String field) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new IllegalArgumentException(field + " must be finite");
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " must be numeric", error);
        }
    }

    private static void requiredGrid(String value, String field) {
        String[] coordinates = required(value, field).split(",", -1);
        if (coordinates.length != 2) throw new IllegalArgumentException(field + " must be x,y");
        try {
            Integer.parseInt(coordinates[0]);
            Integer.parseInt(coordinates[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " must contain integer coordinates", error);
        }
    }
}
