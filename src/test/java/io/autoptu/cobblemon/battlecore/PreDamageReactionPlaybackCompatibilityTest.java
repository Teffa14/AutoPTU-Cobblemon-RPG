package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreDamageReactionPlaybackCompatibilityTest {
    private final BattlePresentationProjector presentationProjector = new BattlePresentationProjector();
    private final BattleWorldRelocationProjector relocationProjector = new BattleWorldRelocationProjector();

    @Test
    void projectsAuthoritativeReactionSequenceWithoutReevaluatingTelepathy() {
        BattleAuthoritySnapshot snapshot = snapshot();
        LinkedHashMap<String, String> sourceAttributes = new LinkedHashMap<>();
        sourceAttributes.put("phase", "pre_damage_interrupt");
        sourceAttributes.put("authoritative", "true");

        ArrayList<BattleEventPlaybackEnvelope> sourceEvents = new ArrayList<>();
        sourceEvents.add(new BattleEventPlaybackEnvelope(
                41,
                "rule_effect",
                "rule_effect|ability|Telepathy|pokemon-1|pokemon-ally|move-area|shift|0.0|40",
                sourceAttributes));
        sourceEvents.add(new BattleEventPlaybackEnvelope(
                42,
                "shift_resolved",
                "shift_resolved|pokemon-1|2,3|4,3",
                Map.of("cause", "authoritative_pre_damage_reaction")));

        BattlePlaybackBatch playback = new BattlePlaybackBatch("battle-reaction-1", sourceEvents);

        sourceAttributes.put("authoritative", "false");
        sourceEvents.clear();

        BattlePresentationBatch presentation = presentationProjector.project(playback);
        BattleWorldRelocationBatch world = relocationProjector.project(snapshot, presentation);

        assertEquals(2, presentation.commands().size());
        BattlePresentationCommand cue = presentation.commands().get(0);
        assertEquals(41, cue.sequence());
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, cue.kind());
        assertEquals("pokemon-1", cue.subjectId());
        assertEquals("ability", cue.data().get("sourceKind"));
        assertEquals("Telepathy", cue.data().get("sourceName"));
        assertEquals("shift", cue.data().get("effect"));

        BattlePresentationCommand relocationCommand = presentation.commands().get(1);
        assertEquals(42, relocationCommand.sequence());
        assertEquals(BattlePresentationCommand.Kind.ENTITY_RELOCATION, relocationCommand.kind());
        assertEquals("pokemon-1", relocationCommand.subjectId());

        assertEquals(1, world.relocations().size());
        BattleWorldRelocation relocation = world.relocations().getFirst();
        assertEquals("pokemon-1", relocation.combatantId());
        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 102, 64, 203), relocation.origin());
        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 104, 64, 203), relocation.destination());

        assertEquals("true", playback.events().getFirst().attributes().get("authoritative"));
        assertThrows(UnsupportedOperationException.class, () -> playback.events().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.roster().clear());
        assertEquals(new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1), snapshot.arena());
        assertTrue(PreDamageReactionCompatibility.minecraftMayRenderAuthoritativeReactionEvents());
        assertTrue(PreDamageReactionCompatibility.semanticReactionPlaybackFixtureIsAvailable());
    }

    private static BattleAuthoritySnapshot snapshot() {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot reactingPokemon = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:gardevoir", 40, Set.of("Telepathy"), null, 2);
        return new BattleAuthoritySnapshot(
                "battle-reaction-1",
                "player-1",
                trainer,
                List.of(reactingPokemon),
                List.of(),
                20260823L,
                new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1));
    }
}
