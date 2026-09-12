package io.autoptu.cobblemon.fabric.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C presentation payload for the AutoPTU battle HUD.
 *
 * Every gameplay field is supplied by the server. A max HP of -1 means the authoritative battle
 * boundary has not supplied max HP; clients must display current HP without inventing a ratio.
 * Test-lab payloads may provide explicit synthetic current/max values because they are QA-only.
 */
public record FabricBattleHudPayload(
        boolean visible,
        Combatant left,
        Combatant right,
        String message,
        boolean testMode
) implements CustomPayload {
    public static final Id<FabricBattleHudPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_hud"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricBattleHudPayload> CODEC =
            CustomPayload.codecOf(FabricBattleHudPayload::write, FabricBattleHudPayload::new);

    public FabricBattleHudPayload {
        if (left == null) left = Combatant.empty();
        if (right == null) right = Combatant.empty();
        message = message == null ? "" : message.strip();
    }

    public FabricBattleHudPayload(PacketByteBuf buf) {
        this(
                buf.readBoolean(),
                Combatant.read(buf),
                Combatant.read(buf),
                buf.readString(),
                buf.readBoolean()
        );
    }

    public static FabricBattleHudPayload hidden() {
        return new FabricBattleHudPayload(false, Combatant.empty(), Combatant.empty(), "", false);
    }

    private void write(PacketByteBuf buf) {
        buf.writeBoolean(visible);
        left.write(buf);
        right.write(buf);
        buf.writeString(message);
        buf.writeBoolean(testMode);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record Combatant(
            String displayName,
            String speciesId,
            int level,
            int currentHp,
            int maxHp,
            String statusId
    ) {
        public Combatant {
            displayName = safe(displayName);
            speciesId = safe(speciesId);
            statusId = safe(statusId);
            if (level < 0) throw new IllegalArgumentException("level cannot be negative");
            if (currentHp < 0) throw new IllegalArgumentException("currentHp cannot be negative");
            if (maxHp < -1) throw new IllegalArgumentException("maxHp must be -1 or non-negative");
            if (maxHp >= 0 && currentHp > maxHp) {
                throw new IllegalArgumentException("currentHp cannot exceed authoritative maxHp");
            }
        }

        public static Combatant empty() {
            return new Combatant("", "", 0, 0, -1, "");
        }

        public boolean hasKnownMaxHp() {
            return maxHp > 0;
        }

        public float hpRatio() {
            if (!hasKnownMaxHp()) return -1.0F;
            return Math.max(0.0F, Math.min(1.0F, (float) currentHp / (float) maxHp));
        }

        private static Combatant read(PacketByteBuf buf) {
            return new Combatant(
                    buf.readString(),
                    buf.readString(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readInt(),
                    buf.readString()
            );
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(displayName);
            buf.writeString(speciesId);
            buf.writeVarInt(level);
            buf.writeVarInt(currentHp);
            buf.writeInt(maxHp);
            buf.writeString(statusId);
        }

        private static String safe(String value) {
            return value == null ? "" : value.strip();
        }
    }
}
