package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.battlecore.BattleEntityBoundPresentationDispatcher;
import io.autoptu.cobblemon.battlecore.BattleHealthProjectionBatch;
import io.autoptu.cobblemon.battlecore.BattleHealthProjectionProjector;
import io.autoptu.cobblemon.battlecore.BattlePlaybackBatch;
import io.autoptu.cobblemon.battlecore.BattlePresentationBatch;
import io.autoptu.cobblemon.battlecore.BattlePresentationEntityBindings;
import io.autoptu.cobblemon.battlecore.BattlePresentationEntityProjector;
import io.autoptu.cobblemon.battlecore.BattlePresentationProjector;
import io.autoptu.cobblemon.battlecore.BattleWorldRelocationBatch;
import io.autoptu.cobblemon.battlecore.BattleWorldRelocationProjector;
import io.autoptu.cobblemon.battlecore.GatewayBackedBattleEntityBoundPresentationConsumer;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;

import java.util.Map;
import java.util.Objects;

/**
 * Production Fabric/Cobblemon projection boundary for semantic AutoPTU battle events.
 *
 * Callers provide the frozen reservation snapshot, exact presentation identities and the ordered
 * authoritative playback batch. Minecraft only renders those results. It never calculates damage,
 * movement legality, status behavior, targets, RNG, fainting or battle outcomes here.
 */
public final class FabricSemanticBattlePlayback {
    private final PresentationEntityHandleRegistry<PokemonEntity> handles = new PresentationEntityHandleRegistry<>();
    private final BattlePresentationProjector presentationProjector = new BattlePresentationProjector();
    private final BattleHealthProjectionProjector healthProjector = new BattleHealthProjectionProjector();
    private final BattleWorldRelocationProjector relocationProjector = new BattleWorldRelocationProjector();
    private final BattlePresentationEntityProjector entityProjector = new BattlePresentationEntityProjector();
    private final GatewayBackedBattleEntityBoundPresentationConsumer consumer =
            new GatewayBackedBattleEntityBoundPresentationConsumer(
                    new RegistryBackedPresentationEntityGateway<>(handles, new CobblemonPresentationEntityBackend()));

    public void registerEntity(String reservationId, String presentationEntityId, PokemonEntity entity) {
        handles.register(reservationId, presentationEntityId, Objects.requireNonNull(entity, "entity"));
    }

    public int registeredEntityCount(String reservationId) {
        return handles.registeredCount(reservationId);
    }

    public void project(
            BattleAuthoritySnapshot snapshot,
            Map<String, String> presentationEntityIdsByCombatant,
            BattlePlaybackBatch playback
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(playback, "playback");
        if (!snapshot.reservationId().equals(playback.reservationId())) {
            throw new IllegalArgumentException("playback reservation must match authoritative snapshot");
        }

        // Preserve the exact semantic envelopes that crossed the authoritative boundary before any
        // Minecraft-specific projection. The trace is evidence only and never becomes battle truth.
        FabricSemanticBattleTrace.record(playback);

        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Objects.requireNonNull(presentationEntityIdsByCombatant, "presentationEntityIdsByCombatant"));
        BattlePresentationBatch presentation = presentationProjector.project(playback);
        BattleHealthProjectionBatch health = healthProjector.project(snapshot, presentation);
        BattleWorldRelocationBatch relocations = relocationProjector.project(snapshot, presentation);

        BattleEntityBoundPresentationDispatcher.dispatch(
                entityProjector.bindCombatantStream(presentation, health, relocations, bindings),
                consumer
        );
    }

    public void releaseReservation(String reservationId) {
        handles.releaseReservation(reservationId);
        FabricSemanticBattleTrace.release(reservationId);
    }
}
