package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S2C read-only projection for Cobblemon's native Summary widgets.
 *
 * The presentation Pokemon UUID is generated for one summary view and is not a canonical Pokemon
 * identity. Clients receive display values only; no client response can mutate canonical state.
 */
public record FabricCanonicalPokemonSummaryPayload(List<Projection> projections) implements CustomPayload {
    public static final Id<FabricCanonicalPokemonSummaryPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "canonical_pokemon_summary"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricCanonicalPokemonSummaryPayload> CODEC =
            CustomPayload.codecOf(FabricCanonicalPokemonSummaryPayload::write, FabricCanonicalPokemonSummaryPayload::new);

    public record Projection(
            UUID presentationPokemonId,
            String canonicalPokemonId,
            int level,
            int currentHp,
            int maxHp,
            int atk,
            int def,
            int spatk,
            int spdef,
            int spd,
            List<String> statuses,
            int injuries,
            List<String> moveIds
    ) {
        public Projection {
            if (presentationPokemonId == null) throw new IllegalArgumentException("presentationPokemonId is required");
            if (canonicalPokemonId == null || canonicalPokemonId.isBlank()) {
                throw new IllegalArgumentException("canonicalPokemonId is required");
            }
            canonicalPokemonId = canonicalPokemonId.strip();
            if (level < 1) throw new IllegalArgumentException("canonical level must be positive");
            if (maxHp <= 0 || currentHp < 0 || currentHp > maxHp) {
                throw new IllegalArgumentException("invalid canonical HP projection");
            }
            if (atk < 1 || def < 1 || spatk < 1 || spdef < 1 || spd < 1) {
                throw new IllegalArgumentException("canonical combat stats must be positive");
            }
            statuses = statuses == null ? List.of() : List.copyOf(statuses);
            if (injuries < 0) throw new IllegalArgumentException("injuries must not be negative");
            moveIds = moveIds == null ? List.of() : List.copyOf(moveIds);
        }

        public static Projection from(UUID presentationPokemonId, CanonicalPokemonDetail detail) {
            if (detail == null || detail.health() == null || detail.combatStats() == null) {
                throw new IllegalStateException("native Cobblemon summary requires canonical HP and combat stats");
            }
            CanonicalCombatStats stats = detail.combatStats();
            List<String> moveIds = detail.moveLoadout() == null ? List.of() : detail.moveLoadout().moveIds();
            return new Projection(
                    presentationPokemonId,
                    detail.pokemonId(),
                    detail.level(),
                    detail.health().currentHp(),
                    detail.health().maxHp(),
                    stats.atk(),
                    stats.def(),
                    stats.spatk(),
                    stats.spdef(),
                    stats.spd(),
                    detail.statuses(),
                    detail.injuryState() == null ? 0 : detail.injuryState().injuries(),
                    moveIds
            );
        }
    }

    public FabricCanonicalPokemonSummaryPayload {
        projections = projections == null ? List.of() : List.copyOf(projections);
    }

    public FabricCanonicalPokemonSummaryPayload(PacketByteBuf buf) {
        this(read(buf));
    }

    private static List<Projection> read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        ArrayList<Projection> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID presentationId = buf.readUuid();
            String canonicalId = buf.readString();
            int level = buf.readVarInt();
            int currentHp = buf.readVarInt();
            int maxHp = buf.readVarInt();
            int atk = buf.readVarInt();
            int def = buf.readVarInt();
            int spatk = buf.readVarInt();
            int spdef = buf.readVarInt();
            int spd = buf.readVarInt();
            int statusCount = buf.readVarInt();
            ArrayList<String> statuses = new ArrayList<>(statusCount);
            for (int statusIndex = 0; statusIndex < statusCount; statusIndex++) {
                statuses.add(buf.readString());
            }
            int injuries = buf.readVarInt();
            int moveCount = buf.readVarInt();
            ArrayList<String> moveIds = new ArrayList<>(moveCount);
            for (int moveIndex = 0; moveIndex < moveCount; moveIndex++) {
                moveIds.add(buf.readString());
            }
            result.add(new Projection(
                    presentationId, canonicalId, level, currentHp, maxHp, atk, def, spatk, spdef, spd, statuses, injuries, moveIds));
        }
        return List.copyOf(result);
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(projections.size());
        for (Projection projection : projections) {
            buf.writeUuid(projection.presentationPokemonId());
            buf.writeString(projection.canonicalPokemonId());
            buf.writeVarInt(projection.level());
            buf.writeVarInt(projection.currentHp());
            buf.writeVarInt(projection.maxHp());
            buf.writeVarInt(projection.atk());
            buf.writeVarInt(projection.def());
            buf.writeVarInt(projection.spatk());
            buf.writeVarInt(projection.spdef());
            buf.writeVarInt(projection.spd());
            buf.writeVarInt(projection.statuses().size());
            for (String status : projection.statuses()) buf.writeString(status);
            buf.writeVarInt(projection.injuries());
            buf.writeVarInt(projection.moveIds().size());
            for (String moveId : projection.moveIds()) buf.writeString(moveId);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
