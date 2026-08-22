package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Earliest public Cobblemon battle-start handoff used by the AutoPTU integration.
 *
 * The handler receives opaque participant identities only. It cannot inspect Pokemon stats, HP,
 * moves, abilities or other battle data through this boundary. The UUIDs are lookup keys for
 * server-owned canonical state; they are not themselves PTU authority. Cobblemon is preempted
 * only after the server-owned handler explicitly claims the encounter for AutoPTU.
 */
public final class CobblemonBattleStartInterceptor {
    private CobblemonBattleStartInterceptor() {}

    public enum ParticipantKind {
        PLAYER,
        NPC,
        WILD
    }

    public record ParticipantIdentity(
            int side,
            ParticipantKind kind,
            String actorId,
            List<String> pokemonIds
    ) {
        public ParticipantIdentity {
            if (side != 1 && side != 2) {
                throw new IllegalArgumentException("side must be 1 or 2");
            }
            Objects.requireNonNull(kind, "kind");
            actorId = requireIdentifier(actorId, "actorId");
            if (pokemonIds == null || pokemonIds.isEmpty()) {
                throw new IllegalArgumentException("pokemonIds must not be empty");
            }
            ArrayList<String> copy = new ArrayList<>();
            for (String pokemonId : pokemonIds) {
                String normalized = requireIdentifier(pokemonId, "pokemonId");
                if (copy.contains(normalized)) {
                    throw new IllegalArgumentException("duplicate pokemonId in participant");
                }
                copy.add(normalized);
            }
            pokemonIds = List.copyOf(copy);
        }
    }

    public record BattleStartSignal(
            String cobblemonBattleId,
            List<ParticipantIdentity> participants
    ) {
        public BattleStartSignal {
            cobblemonBattleId = requireIdentifier(cobblemonBattleId, "cobblemonBattleId");
            if (participants == null || participants.isEmpty()) {
                throw new IllegalArgumentException("participants must not be empty");
            }
            participants = List.copyOf(participants);
        }
    }

    @FunctionalInterface
    public interface ClaimHandler {
        boolean tryClaim(BattleStartSignal signal);
    }

    public static ObservableSubscription<BattleStartedEvent.Pre> subscribe(ClaimHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return CobblemonEvents.BATTLE_STARTED_PRE.subscribe(
                Priority.HIGHEST,
                (Consumer<BattleStartedEvent.Pre>) event -> {
                    BattleStartSignal signal = new BattleStartSignal(
                            event.getBattle().getBattleId().toString(),
                            List.of(
                                    extractSide(1, event.getBattle().getSide1()),
                                    extractSide(2, event.getBattle().getSide2())
                            ).stream().flatMap(List::stream).toList()
                    );
                    if (handler.tryClaim(signal)) {
                        event.cancel();
                    }
                }
        );
    }

    public static void unsubscribe(ObservableSubscription<BattleStartedEvent.Pre> subscription) {
        Objects.requireNonNull(subscription, "subscription");
        CobblemonEvents.BATTLE_STARTED_PRE.unsubscribe(subscription);
    }

    private static List<ParticipantIdentity> extractSide(int sideNumber, BattleSide side) {
        ArrayList<ParticipantIdentity> participants = new ArrayList<>();
        for (BattleActor actor : side.getActors()) {
            ArrayList<String> pokemonIds = new ArrayList<>();
            for (BattlePokemon pokemon : actor.getPokemonList()) {
                pokemonIds.add(pokemon.getUuid().toString());
            }
            participants.add(new ParticipantIdentity(
                    sideNumber,
                    participantKind(actor.getType()),
                    actor.getUuid().toString(),
                    pokemonIds
            ));
        }
        return List.copyOf(participants);
    }

    private static ParticipantKind participantKind(ActorType type) {
        return switch (type) {
            case PLAYER -> ParticipantKind.PLAYER;
            case NPC -> ParticipantKind.NPC;
            case WILD -> ParticipantKind.WILD;
        };
    }

    private static String requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
