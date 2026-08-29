package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricFirstJoinOnboardingRuntimeTest {
    @TempDir Path temp;

    @Test
    void canonicalTrainerWithoutPartyNeedsOnboardingUntilPersistentPartyExists() {
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(uuid);
        FileVersionedCanonicalStateRepository players = new FileVersionedCanonicalStateRepository(temp);
        FileCanonicalPlayerEncounterProfileRepository profiles = new FileCanonicalPlayerEncounterProfileRepository(temp);

        assertFalse(FabricFirstJoinOnboardingRuntime.needsOnboarding(players, profiles, uuid));

        players.createPlayerIfAbsent(new CanonicalPlayerState(
                playerId, Set.of(), Map.of(), Set.of(), Set.of(), 0, 0, null, "", 0L
        ));
        assertTrue(FabricFirstJoinOnboardingRuntime.needsOnboarding(players, profiles, uuid));

        profiles.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                playerId,
                List.of(playerId + ":starter"),
                Map.of(),
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1),
                0L
        ));
        assertFalse(FabricFirstJoinOnboardingRuntime.needsOnboarding(players, profiles, uuid));

        FileVersionedCanonicalStateRepository reopenedPlayers = new FileVersionedCanonicalStateRepository(temp);
        FileCanonicalPlayerEncounterProfileRepository reopenedProfiles = new FileCanonicalPlayerEncounterProfileRepository(temp);
        assertFalse(FabricFirstJoinOnboardingRuntime.needsOnboarding(reopenedPlayers, reopenedProfiles, uuid));
    }
}
