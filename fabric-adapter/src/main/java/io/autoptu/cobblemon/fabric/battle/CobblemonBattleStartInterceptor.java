package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Earliest public Cobblemon battle-start handoff used by the AutoPTU integration.
 *
 * The handler receives only an opaque Cobblemon battle identifier. It cannot inspect Pokemon
 * stats, HP, moves, abilities or other data through this boundary. Cobblemon is preempted only
 * after the server-owned handler explicitly claims the encounter for AutoPTU.
 */
public final class CobblemonBattleStartInterceptor {
    private CobblemonBattleStartInterceptor() {}

    public record BattleStartSignal(String cobblemonBattleId) {
        public BattleStartSignal {
            if (cobblemonBattleId == null || cobblemonBattleId.isBlank()) {
                throw new IllegalArgumentException("cobblemonBattleId is required");
            }
            cobblemonBattleId = cobblemonBattleId.strip();
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
                    String battleId = event.getBattle().getBattleId().toString();
                    if (handler.tryClaim(new BattleStartSignal(battleId))) {
                        event.cancel();
                    }
                }
        );
    }

    public static void unsubscribe(ObservableSubscription<BattleStartedEvent.Pre> subscription) {
        Objects.requireNonNull(subscription, "subscription");
        CobblemonEvents.BATTLE_STARTED_PRE.unsubscribe(subscription);
    }
}
