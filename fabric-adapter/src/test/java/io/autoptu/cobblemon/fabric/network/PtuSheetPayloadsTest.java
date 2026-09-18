package io.autoptu.cobblemon.fabric.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PtuSheetPayloadsTest {
    @Test void starterRequestRoundTripsConfiguredSelection() {
        var value = new PtuSheetPayloads.StarterRequest("Región inicial", 2, 19);
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            PtuSheetPayloads.StarterRequest.CODEC.encode(buffer, value);
            assertEquals(value, PtuSheetPayloads.StarterRequest.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void starterResponseRoundTripsWithoutCreatingPokemon() {
        var value = new PtuSheetPayloads.StarterResponse("Kanto", 0, 20, "ready", "{\"name\":\"Bulbasaur\"}");
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            PtuSheetPayloads.StarterResponse.CODEC.encode(buffer, value);
            assertEquals(value, PtuSheetPayloads.StarterResponse.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void starterRequestRejectsOversizedCategory() {
        var value = new PtuSheetPayloads.StarterRequest("x".repeat(129), 0, 21);
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try { assertThrows(RuntimeException.class, () -> PtuSheetPayloads.StarterRequest.CODEC.encode(buffer, value)); }
        finally { buffer.release(); }
    }
    @Test void requestCarriesIdentityAndCorrelationOnly() {
        var value = new PtuSheetPayloads.Request(UUID.randomUUID(), 17);
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            PtuSheetPayloads.Request.CODEC.encode(buffer, value);
            assertEquals(value, PtuSheetPayloads.Request.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void responseRoundTripsUnicodeAndStatus() {
        var value = new PtuSheetPayloads.Response(UUID.randomUUID(), 18, "ready", "starter", "{\"nombre\":\"Pokémon ♀\"}");
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            PtuSheetPayloads.Response.CODEC.encode(buffer, value);
            assertEquals(value, PtuSheetPayloads.Response.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void rejectsOversizedServerSheets() {
        var value = new PtuSheetPayloads.Response(UUID.randomUUID(), 18, "ready", "starter", "x".repeat(PtuSheetPayloads.MAX_JSON + 1));
        var buffer = new PacketByteBuf(Unpooled.buffer());
        try { assertThrows(RuntimeException.class, () -> PtuSheetPayloads.Response.CODEC.encode(buffer, value)); }
        finally { buffer.release(); }
    }
}
