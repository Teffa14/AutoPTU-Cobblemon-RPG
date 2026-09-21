package io.autoptu.cobblemon.fabric.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricCanonicalPokemonSummaryPayloadTest {
    @Test
    void roundTripsExactCanonicalDisplayProjection() {
        UUID id = UUID.randomUUID();
        var projection = new FabricCanonicalPokemonSummaryPayload.Projection(
                id, "pokemon-1", 17, 29, 7, 8, 9, 10, 11, List.of("burned", "slowed"), 2);
        var payload = new FabricCanonicalPokemonSummaryPayload(List.of(projection));

        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        FabricCanonicalPokemonSummaryPayload.CODEC.encode(buffer, payload);
        var decoded = FabricCanonicalPokemonSummaryPayload.CODEC.decode(buffer);

        assertEquals(payload, decoded);
        assertEquals(29, decoded.projections().getFirst().maxHp());
        assertEquals(11, decoded.projections().getFirst().spd());
    }
}
