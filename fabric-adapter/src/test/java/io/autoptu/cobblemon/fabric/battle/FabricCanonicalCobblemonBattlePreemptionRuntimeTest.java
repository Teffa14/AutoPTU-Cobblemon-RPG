package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCanonicalCobblemonBattlePreemptionRuntimeTest {
    @Test
    void matchesOnlyOpposingWildPresentationEntityForPlayer() {
        var player = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1, CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "player-1", List.of("party-mon"));
        var wild = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                2, CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "cobblemon-pokemon-uuid", List.of("cobblemon-pokemon-uuid"), "entity-uuid");
        var signal = new CobblemonBattleStartInterceptor.BattleStartSignal("battle", List.of(player, wild));

        assertTrue(FabricCanonicalCobblemonBattlePreemptionRuntime.matchesBoundWild(
                signal, "player-1", "entity-uuid"));
        assertFalse(FabricCanonicalCobblemonBattlePreemptionRuntime.matchesBoundWild(
                signal, "player-1", "different-entity"));
    }

    @Test
    void doesNotTreatSameSideWildAsOpponent() {
        var player = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1, CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "player-1", List.of("party-mon"));
        var wild = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1, CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "wild", List.of("wild"), "entity-uuid");
        var signal = new CobblemonBattleStartInterceptor.BattleStartSignal("battle", List.of(player, wild));

        assertFalse(FabricCanonicalCobblemonBattlePreemptionRuntime.matchesBoundWild(
                signal, "player-1", "entity-uuid"));
    }
}
