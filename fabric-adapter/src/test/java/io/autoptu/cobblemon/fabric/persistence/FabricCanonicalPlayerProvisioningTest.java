package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCanonicalPlayerProvisioningTest {
    @TempDir
    Path tempDir;

    @Test
    void createsStableFailClosedCanonicalIdentityFromAuthenticatedUuid() {
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(tempDir);
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        CanonicalPlayerState provisioned = FabricCanonicalPlayerProvisioning.provision(repository, uuid);

        assertEquals("minecraft-player:123e4567-e89b-12d3-a456-426614174000", provisioned.playerId());
        assertEquals(0L, provisioned.revision());
        assertTrue(provisioned.trainerClasses().isEmpty());
        assertTrue(provisioned.skillRanks().isEmpty());
        assertTrue(provisioned.availablePokemonCapabilities().isEmpty());
        assertTrue(provisioned.trainerFeatures().isEmpty());
        assertEquals(0, provisioned.actionPoints());
        assertEquals(0, provisioned.initiativeModifier());
        assertEquals(null, provisioned.explicitInitiativeSpeed());
        assertEquals("", provisioned.teamId());
        assertEquals(provisioned, repository.findPlayer(provisioned.playerId()).orElseThrow());
    }

    @Test
    void repeatedJoinNeverOverwritesExistingCanonicalProgress() {
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(tempDir);
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(uuid);
        CanonicalPlayerState existing = new CanonicalPlayerState(
                playerId,
                Set.of("Ace Trainer"),
                Map.of("command", 4),
                Set.of("ride"),
                Set.of("Strategist"),
                3,
                2,
                11,
                "team-red",
                7L
        );
        assertTrue(repository.createPlayerIfAbsent(existing));

        CanonicalPlayerState provisioned = FabricCanonicalPlayerProvisioning.provision(repository, uuid);

        assertEquals(existing, provisioned);
        assertEquals(existing, repository.findPlayer(playerId).orElseThrow());
    }

    @Test
    void deterministicIdentityDoesNotDependOnPlayerProfileNameOrMutableMinecraftState() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

        String first = FabricCanonicalPlayerProvisioning.canonicalPlayerId(uuid);
        String second = FabricCanonicalPlayerProvisioning.canonicalPlayerId(uuid);

        assertEquals(first, second);
        assertEquals("minecraft-player:" + uuid, first);
    }
}
