package io.autoptu.cobblemon.battlecore;

import java.util.List;

public record BattlePresentationBatch(String reservationId, List<BattlePresentationCommand> commands) {
    public BattlePresentationBatch {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        commands = commands == null ? List.of() : List.copyOf(commands);

        long priorSequence = -1;
        int priorOrdinal = -1;
        for (BattlePresentationCommand command : commands) {
            if (command == null) throw new IllegalArgumentException("commands cannot contain null");
            if (command.sequence() < priorSequence) {
                throw new IllegalArgumentException("command sequence must not move backward");
            }
            if (command.sequence() == priorSequence && command.ordinal() <= priorOrdinal) {
                throw new IllegalArgumentException("command ordinal must increase within an event");
            }
            if (command.sequence() != priorSequence) priorOrdinal = -1;
            priorSequence = command.sequence();
            priorOrdinal = command.ordinal();
        }
    }
}
