package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;

/** Client asks for an ID only. Data, ownership and current state are resolved by the server. */
public final class PtuSheetPayloads {
    public static final int MAX_JSON = 48_000;
    private static boolean registered;
    private PtuSheetPayloads() {}
    public record Request(UUID pokemonId, int sequence) implements CustomPayload {
        public static final Id<Request> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "ptu_sheet_request"));
        public static final PacketCodec<PacketByteBuf, Request> CODEC = CustomPayload.codecOf(Request::write, Request::read);
        private static Request read(PacketByteBuf buf) { return new Request(buf.readUuid(), buf.readVarInt()); }
        private void write(PacketByteBuf buf) { buf.writeUuid(pokemonId); buf.writeVarInt(sequence); }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record Response(UUID pokemonId, int sequence, String status, String origin, String json) implements CustomPayload {
        public static final Id<Response> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "ptu_sheet_response"));
        public static final PacketCodec<PacketByteBuf, Response> CODEC = CustomPayload.codecOf(Response::write, Response::read);
        private static Response read(PacketByteBuf buf) {
            return new Response(buf.readUuid(), buf.readVarInt(), buf.readString(32), buf.readString(32), buf.readString(MAX_JSON));
        }
        private void write(PacketByteBuf buf) {
            buf.writeUuid(pokemonId); buf.writeVarInt(sequence); buf.writeString(status, 32); buf.writeString(origin, 32); buf.writeString(json, MAX_JSON);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playC2S().register(Request.ID, Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Response.ID, Response.CODEC);
        PayloadTypeRegistry.playC2S().register(StarterRequest.ID, StarterRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(StarterResponse.ID, StarterResponse.CODEC);
        registered = true;
    }
    public record StarterRequest(String category, int option, int sequence) implements CustomPayload {
        public static final Id<StarterRequest> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "ptu_starter_request"));
        public static final PacketCodec<PacketByteBuf, StarterRequest> CODEC = CustomPayload.codecOf(StarterRequest::write, StarterRequest::read);
        private static StarterRequest read(PacketByteBuf buf) { return new StarterRequest(buf.readString(128), buf.readVarInt(), buf.readVarInt()); }
        private void write(PacketByteBuf buf) { buf.writeString(category, 128); buf.writeVarInt(option); buf.writeVarInt(sequence); }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record StarterResponse(String category, int option, int sequence, String status, String json) implements CustomPayload {
        public static final Id<StarterResponse> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "ptu_starter_response"));
        public static final PacketCodec<PacketByteBuf, StarterResponse> CODEC = CustomPayload.codecOf(StarterResponse::write, StarterResponse::read);
        private static StarterResponse read(PacketByteBuf buf) {
            return new StarterResponse(buf.readString(128), buf.readVarInt(), buf.readVarInt(), buf.readString(32), buf.readString(MAX_JSON));
        }
        private void write(PacketByteBuf buf) {
            buf.writeString(category, 128); buf.writeVarInt(option); buf.writeVarInt(sequence); buf.writeString(status, 32); buf.writeString(json, MAX_JSON);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
