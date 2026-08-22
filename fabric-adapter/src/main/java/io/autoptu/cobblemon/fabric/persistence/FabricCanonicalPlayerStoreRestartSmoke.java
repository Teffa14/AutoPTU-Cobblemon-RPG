package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Two-boot production-runtime proof for world-scoped canonical player persistence.
 *
 * CI runs one dedicated server in seed mode, stops it, then boots the same world in verify mode.
 * The fixtures are canonical server-owned test data; no Minecraft or Cobblemon state is imported.
 */
public final class FabricCanonicalPlayerStoreRestartSmoke {
    public static final String MODE_PROPERTY = "autoptu.liveCanonicalStoreRestartSmoke";
    public static final String SEED_SUCCESS_LOG = "AutoPTU live canonical player store seed smoke passed";
    public static final String RESTART_SUCCESS_LOG = "AutoPTU live canonical player store restart smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String PLAYER_ID = "integration-restart-player";
    private static final CanonicalPlayerState FIXTURE = new CanonicalPlayerState(
            PLAYER_ID,
            Set.of("Ace Trainer", "Commander"),
            Map.of("athletics", 6, "command", 5),
            Set.of("ride", "swim"),
            Set.of("Orders", "Focused Training"),
            4,
            2,
            37,
            "team-restart-smoke",
            7
    );
    private static final CanonicalPlayerEncounterProfile ENCOUNTER_FIXTURE =
            new CanonicalPlayerEncounterProfile(
                    PLAYER_ID,
                    List.of("restart-pokemon-1", "restart-pokemon-2"),
                    Map.of("restart-item-1", 2),
                    new BattleArenaSnapshot("minecraft:overworld", 12, 64, -8, 1, 0, 0, 1),
                    3
            );

    private FabricCanonicalPlayerStoreRestartSmoke() {}

    public static void registerIfEnabled() {
        String mode = System.getProperty(MODE_PROPERTY, "").strip().toLowerCase(Locale.ROOT);
        if (mode.isEmpty()) return;
        if (!mode.equals("seed") && !mode.equals("verify")) {
            throw new IllegalArgumentException(MODE_PROPERTY + " must be seed or verify");
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> run(server, mode));
    }

    private static void run(MinecraftServer server, String mode) {
        var repository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
        var encounterProfiles = FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server);
        if (mode.equals("seed")) {
            if (!repository.createPlayerIfAbsent(FIXTURE)) {
                throw new IllegalStateException("canonical restart smoke fixture already exists before seed boot");
            }
            if (!encounterProfiles.createProfileIfAbsent(ENCOUNTER_FIXTURE)) {
                throw new IllegalStateException("canonical encounter profile already exists before seed boot");
            }
            CanonicalPlayerState persisted = repository.findPlayer(PLAYER_ID).orElseThrow(
                    () -> new IllegalStateException("canonical restart smoke fixture was not persisted"));
            CanonicalPlayerEncounterProfile persistedEncounter = encounterProfiles.findProfile(PLAYER_ID).orElseThrow(
                    () -> new IllegalStateException("canonical encounter profile was not persisted"));
            if (!FIXTURE.equals(persisted)) {
                throw new IllegalStateException("canonical restart smoke seed did not round-trip exact state");
            }
            if (!ENCOUNTER_FIXTURE.equals(persistedEncounter)) {
                throw new IllegalStateException("canonical encounter profile did not round-trip exact state");
            }
            LOGGER.info(SEED_SUCCESS_LOG);
            return;
        }

        CanonicalPlayerState persisted = repository.findPlayer(PLAYER_ID).orElseThrow(
                () -> new IllegalStateException("canonical restart smoke fixture missing after server restart"));
        CanonicalPlayerEncounterProfile persistedEncounter = encounterProfiles.findProfile(PLAYER_ID).orElseThrow(
                () -> new IllegalStateException("canonical encounter profile missing after server restart"));
        if (!FIXTURE.equals(persisted)) {
            throw new IllegalStateException("canonical restart smoke state changed across server restart");
        }
        if (!ENCOUNTER_FIXTURE.equals(persistedEncounter)) {
            throw new IllegalStateException("canonical encounter profile changed across server restart");
        }
        LOGGER.info(RESTART_SUCCESS_LOG);
    }
}
