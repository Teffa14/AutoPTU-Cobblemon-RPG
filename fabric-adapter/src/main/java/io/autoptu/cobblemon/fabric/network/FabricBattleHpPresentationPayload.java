package io.autoptu.cobblemon.fabric.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** S2C presentation-only payload for authoritative PTU current HP on a bound Pokemon actor. */
public record FabricBattleHpPresentationPayload(UUID entityUuid, int currentHp) implements CustomPayload {
    public static final Id<FabricBattleHpPresentationPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_hp_presentation"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricBattleHpPresentationPayload> CODEC =
            CustomPayload.codecOf(FabricBattleHpPresentationPayload::write, FabricBattleHpPresentationPayload::new);

    public FabricBattleHpPresentationPayload(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readVarInt());
    }

    public FabricBattleHpPresentationPayload {
        if (entityUuid == null) throw new IllegalArgumentException("entityUuid is required");
        if (currentHp < 0) throw new IllegalArgumentException("currentHp cannot be negative");
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(entityUuid);
        buf.writeVarInt(currentHp);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
