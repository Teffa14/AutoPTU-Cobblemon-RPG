package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import io.autoptu.cobblemon.fabric.world.VisibleWildPokemonEncounterRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read-only server-authoritative status surface for the normal persistent RPG loop. */
public final class FabricPlayerStatusRuntime {
    private FabricPlayerStatusRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("status")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU status must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        Optional<CanonicalPlayerState> trainer = FabricCanonicalPlayerStoreRuntime
                .requireRepository(player.getServer())
                .findPlayer(playerId);

        int partyCount = 0;
        boolean partyStateReadable = true;
        try {
            CanonicalPartyQueryService partyService = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
            );
            partyCount = partyService.findParty(playerId).map(party -> party.members().size()).orElse(0);
        } catch (IllegalStateException inconsistentParty) {
            partyStateReadable = false;
        }

        Optional<WorldEncounterTriggerRequestService.Request> encounter =
                VisibleWildPokemonEncounterRuntime.requests().pendingForPlayer(playerId);

        Snapshot snapshot = snapshot(trainer, partyCount, partyStateReadable, encounter);
        for (String line : formatLines(snapshot)) {
            player.sendMessage(Text.literal(line), false);
        }
        return snapshot.trainerLoaded() ? 1 : 0;
    }

    static Snapshot snapshot(
            Optional<CanonicalPlayerState> trainer,
            int partyCount,
            boolean partyStateReadable,
            Optional<WorldEncounterTriggerRequestService.Request> encounter
    ) {
        if (partyCount < 0) throw new IllegalArgumentException("partyCount must be >= 0");
        trainer = trainer == null ? Optional.empty() : trainer;
        encounter = encounter == null ? Optional.empty() : encounter;

        ArrayList<String> blockers = new ArrayList<>();
        if (trainer.isEmpty()) blockers.add("Trainer state is not loaded.");
        if (!partyStateReadable) blockers.add("Canonical party state is inconsistent.");
        else if (partyCount == 0) blockers.add("Choose a starter to create the persistent party.");
        if (encounter.isPresent()) {
            blockers.add("Visible wild encounter is pending canonical handoff/battle start.");
        }

        return new Snapshot(
                trainer.isPresent(),
                trainer.map(CanonicalPlayerState::revision).orElse(-1L),
                partyCount,
                partyStateReadable,
                encounter.map(WorldEncounterTriggerRequestService.Request::canonicalEncounterId).orElse(null),
                encounter.isPresent() ? "pending world encounter" : "none",
                List.copyOf(blockers)
        );
    }

    static List<String> formatLines(Snapshot snapshot) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU status");
        lines.add("Trainer: " + (snapshot.trainerLoaded() ? "loaded" : "missing"));
        lines.add("Save revision: " + (snapshot.revision() >= 0 ? snapshot.revision() : "unavailable"));
        lines.add("Party: " + (snapshot.partyStateReadable() ? snapshot.partyCount() + " member(s)" : "unreadable"));
        lines.add("Encounter: " + (snapshot.canonicalEncounterId() == null ? "none" : snapshot.canonicalEncounterId()));
        lines.add("Battle: " + snapshot.battleState());
        if (snapshot.blockers().isEmpty()) lines.add("Blockers: none");
        else for (String blocker : snapshot.blockers()) lines.add("Blocker: " + blocker);
        return List.copyOf(lines);
    }

    record Snapshot(
            boolean trainerLoaded,
            long revision,
            int partyCount,
            boolean partyStateReadable,
            String canonicalEncounterId,
            String battleState,
            List<String> blockers
    ) {}
}
