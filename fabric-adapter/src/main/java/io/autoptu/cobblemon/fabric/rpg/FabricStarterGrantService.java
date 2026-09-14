package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalStarterSelectionDecision;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;

/**
 * Reusable server-authoritative starter provisioning boundary for operator tools and future UI.
 * The caller supplies only a live server-owned player identity and a starter catalogue selector;
 * canonical Trainer, Pokemon and party state are resolved and mutated by server repositories.
 */
public final class FabricStarterGrantService {
    public enum Outcome {
        GRANTED,
        ALREADY_CHOSEN,
        INVALID_STARTER,
        MISSING_TRAINER,
        REJECTED
    }

    public record Result(
            Outcome outcome,
            String playerId,
            String speciesId,
            String pokemonId,
            String detail
    ) {
        public Result {
            Objects.requireNonNull(outcome, "outcome");
            playerId = playerId == null ? "" : playerId;
            speciesId = speciesId == null ? "" : speciesId;
            pokemonId = pokemonId == null ? "" : pokemonId;
            detail = detail == null ? "" : detail;
        }
    }

    public Result grant(ServerPlayerEntity target, String requestedStarter) {
        Objects.requireNonNull(target, "target");
        MinecraftServer server = Objects.requireNonNull(target.getServer(), "target server");
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());

        if (FabricCanonicalPlayerStoreRuntime.requireRepository(server).findPlayer(playerId).isEmpty()) {
            return new Result(
                    Outcome.MISSING_TRAINER,
                    playerId,
                    "",
                    "",
                    "canonical Trainer state does not exist"
            );
        }

        CanonicalStarterSelectionDecision decision =
                FabricStarterSelectionRuntime.chooseForPlayer(target, requestedStarter == null ? "" : requestedStarter.trim());

        return switch (decision.outcome()) {
            case CHOSEN -> new Result(
                    Outcome.GRANTED,
                    playerId,
                    decision.speciesId(),
                    decision.pokemonId(),
                    decision.detail()
            );
            case ALREADY_CHOSEN -> new Result(
                    Outcome.ALREADY_CHOSEN,
                    playerId,
                    decision.speciesId(),
                    decision.pokemonId(),
                    decision.detail()
            );
            case INVALID_STARTER -> new Result(
                    Outcome.INVALID_STARTER,
                    playerId,
                    decision.speciesId(),
                    decision.pokemonId(),
                    decision.detail()
            );
            case INVALID_REQUEST, CONFLICT -> new Result(
                    Outcome.REJECTED,
                    playerId,
                    decision.speciesId(),
                    decision.pokemonId(),
                    decision.detail()
            );
        };
    }
}
