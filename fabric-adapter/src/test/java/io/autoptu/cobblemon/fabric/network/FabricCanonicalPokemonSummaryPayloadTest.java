package io.autoptu.cobblemon.fabric.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCanonicalPokemonSummaryPayloadTest {
    @Test
    void roundTripsExactCanonicalDisplayProjection() {
        UUID id = UUID.randomUUID();
        var projection = new FabricCanonicalPokemonSummaryPayload.Projection(
                id, "pokemon-1", 12, 17, 29, 7, 8, 9, 10, 11,
                List.of("burned", "slowed"), 2, true, List.of("ember", "quick-attack"), true,
                true, List.of("overland-6", "jump-2"));
        var payload = new FabricCanonicalPokemonSummaryPayload(List.of(projection));

        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        FabricCanonicalPokemonSummaryPayload.CODEC.encode(buffer, payload);
        var decoded = FabricCanonicalPokemonSummaryPayload.CODEC.decode(buffer);

        assertEquals(payload, decoded);
        assertEquals(12, decoded.projections().getFirst().level());
        assertEquals(29, decoded.projections().getFirst().maxHp());
        assertEquals(11, decoded.projections().getFirst().spd());
        assertTrue(decoded.projections().getFirst().moveLoadoutAvailable());
        assertEquals(List.of("ember", "quick-attack"), decoded.projections().getFirst().moveIds());
        assertTrue(decoded.projections().getFirst().heldItemEquipped());
        assertTrue(decoded.projections().getFirst().movementAvailable());
        assertEquals(List.of("overland-6", "jump-2"), decoded.projections().getFirst().capabilityIds());
    }

    @Test
    void roundTripsUnavailableCanonicalMoveLoadoutSeparatelyFromEmptyLoadout() {
        UUID id = UUID.randomUUID();
        var projection = new FabricCanonicalPokemonSummaryPayload.Projection(
                id, "pokemon-2", 5, 10, 10, 5, 5, 5, 5, 5,
                List.of(), 0, false, List.of(), false, false, List.of());
        var payload = new FabricCanonicalPokemonSummaryPayload(List.of(projection));

        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        FabricCanonicalPokemonSummaryPayload.CODEC.encode(buffer, payload);
        var decoded = FabricCanonicalPokemonSummaryPayload.CODEC.decode(buffer);

        assertEquals(payload, decoded);
        assertTrue(decoded.projections().getFirst().moveIds().isEmpty());
        assertFalse(decoded.projections().getFirst().moveLoadoutAvailable());
        assertFalse(decoded.projections().getFirst().movementAvailable());
        assertTrue(decoded.projections().getFirst().capabilityIds().isEmpty());
    }
}
