package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Binds authoritative HP presentation commands to the frozen battle reservation and roster.
 * This class never calculates damage or mutates canonical HP.
 */
public final class BattleHealthProjectionProjector {
    public BattleHealthProjectionBatch project(BattleAuthoritySnapshot snapshot, BattlePresentationBatch batch) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (!snapshot.reservationId().equals(batch.reservationId())) {
            throw new IllegalArgumentException("presentation batch must match frozen battle reservation");
        }

        Set<String> rosterIds = new HashSet<>();
        snapshot.roster().forEach(pokemon -> rosterIds.add(pokemon.pokemonId()));

        ArrayList<BattleHealthProjection> healthUpdates = new ArrayList<>();
        for (BattlePresentationCommand command : batch.commands()) {
            if (command.kind() != BattlePresentationCommand.Kind.HP_PROJECTION) continue;
            if (!rosterIds.contains(command.subjectId())) {
                throw new IllegalArgumentException("HP projection references combatant outside frozen roster: " + command.subjectId());
            }
            int damage = parseNonNegative(command.data().get("damage"), "damage");
            int targetHp = parseNonNegative(command.data().get("targetHp"), "targetHp");
            healthUpdates.add(new BattleHealthProjection(
                    command.sequence(), command.ordinal(), command.subjectId(), damage, targetHp));
        }
        return new BattleHealthProjectionBatch(snapshot.reservationId(), healthUpdates);
    }

    private static int parseNonNegative(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new IllegalArgumentException(field + " cannot be negative");
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " must be an integer", error);
        }
    }
}
