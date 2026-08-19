package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts already-authoritative ENTITY_RELOCATION presentation commands into world coordinates.
 *
 * The projector binds playback to the matching frozen battle reservation and roster. It performs
 * coordinate translation only; movement legality, collision, terrain, forced movement and reactions
 * remain AutoPTU-Java responsibilities.
 */
public final class BattleWorldRelocationProjector {
    public BattleWorldRelocationBatch project(BattleAuthoritySnapshot snapshot, BattlePresentationBatch batch) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (!snapshot.reservationId().equals(batch.reservationId())) {
            throw new IllegalArgumentException("presentation batch must match battle reservation");
        }
        if (snapshot.arena() == null) {
            throw new IllegalArgumentException("battle reservation has no frozen arena");
        }

        Set<String> rosterIds = new HashSet<>();
        snapshot.roster().forEach(pokemon -> rosterIds.add(pokemon.pokemonId()));
        BattleGridTransform transform = BattleGridTransform.from(snapshot.arena());
        ArrayList<BattleWorldRelocation> relocations = new ArrayList<>();

        for (BattlePresentationCommand command : batch.commands()) {
            if (command.kind() != BattlePresentationCommand.Kind.ENTITY_RELOCATION) continue;
            if (!rosterIds.contains(command.subjectId())) {
                throw new IllegalArgumentException("relocation subject is outside the authoritative roster");
            }
            BattleGridCoordinate origin = parseGrid(command.data().get("origin"), "origin");
            BattleGridCoordinate destination = parseGrid(command.data().get("destination"), "destination");
            relocations.add(new BattleWorldRelocation(
                    command.sequence(),
                    command.ordinal(),
                    command.subjectId(),
                    transform.toWorld(origin),
                    transform.toWorld(destination)));
        }

        return new BattleWorldRelocationBatch(snapshot.reservationId(), snapshot.arena(), relocations);
    }

    private static BattleGridCoordinate parseGrid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " grid coordinate is required");
        }
        String[] coordinates = value.strip().split(",", -1);
        if (coordinates.length != 2) {
            throw new IllegalArgumentException(field + " grid coordinate must be x,y");
        }
        try {
            return new BattleGridCoordinate(
                    Integer.parseInt(coordinates[0].strip()),
                    Integer.parseInt(coordinates[1].strip()));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " grid coordinate must contain integers", error);
        }
    }
}
