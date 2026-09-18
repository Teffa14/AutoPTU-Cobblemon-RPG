package io.autoptu.cobblemon.fabric.ptu;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PtuLifecycleRecordTest {
    @Test void starterAcquisitionDoesNotReplaceIdentityOrCreationHistory() {
        UUID pokemon = UUID.randomUUID(), trainer = UUID.randomUUID();
        var starter = PtuLifecycleRecord.observe(null, pokemon, PtuLifecycleRecord.Origin.STARTER, trainer);
        assertSame(starter, PtuLifecycleRecord.observe(starter, pokemon, PtuLifecycleRecord.Origin.ACQUIRED, trainer));
        assertSame(starter, PtuLifecycleRecord.observe(starter, pokemon, PtuLifecycleRecord.Origin.EXISTING, trainer));
        assertEquals(pokemon, starter.pokemonId());
    }
    @Test void capturePromotesProvisionalWorldRecordWithoutClaimingPtuResolution() {
        UUID pokemon = UUID.randomUUID(), trainer = UUID.randomUUID();
        var initial = PtuLifecycleRecord.observe(null, pokemon, PtuLifecycleRecord.Origin.EXISTING, null);
        var captured = PtuLifecycleRecord.observe(initial, pokemon, PtuLifecycleRecord.Origin.NATIVE_CAPTURE, trainer);
        assertEquals(PtuLifecycleRecord.Origin.NATIVE_CAPTURE, captured.origin());
        assertEquals(trainer, captured.firstTrainer());
        assertSame(captured, PtuLifecycleRecord.observe(captured, pokemon, PtuLifecycleRecord.Origin.NATIVE_CAPTURE, trainer));
    }
    @Test void tradeAndPcMovesPreserveOriginalTrainer() {
        UUID pokemon = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        var original = PtuLifecycleRecord.observe(null, pokemon, PtuLifecycleRecord.Origin.STARTER, first);
        var afterTrade = PtuLifecycleRecord.observe(original, pokemon, PtuLifecycleRecord.Origin.ACQUIRED, second);
        assertEquals(first, afterTrade.firstTrainer());
        assertEquals(original, afterTrade);
    }
    @Test void foreignOrUnknownSchemaRecordsCannotBeReused() {
        var original = PtuLifecycleRecord.observe(null, UUID.randomUUID(), PtuLifecycleRecord.Origin.STARTER, UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> PtuLifecycleRecord.observe(original, UUID.randomUUID(), PtuLifecycleRecord.Origin.ACQUIRED, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> new PtuLifecycleRecord(2, UUID.randomUUID(), PtuLifecycleRecord.Origin.EXISTING, null));
    }
    @Test void storageJsonRoundTripPreservesUuidOriginAndTrainer() {
        var value = PtuLifecycleRecord.observe(null, UUID.randomUUID(), PtuLifecycleRecord.Origin.STARTER, UUID.randomUUID());
        var json = new com.google.gson.Gson();
        assertEquals(value, json.fromJson(json.toJson(value), PtuLifecycleRecord.class));
    }
}
