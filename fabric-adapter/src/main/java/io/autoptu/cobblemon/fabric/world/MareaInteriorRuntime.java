package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalNpcCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.rpg.FabricNpcDialogueRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Physical Marea Interior bootstrap for the first canon-backed Ouros district.
 *
 * Blocks, villager bodies and names are presentation. Canonical NPC identity, quests and location
 * state remain server-owned. Minecraft entity loss or AI movement cannot rewrite NPC history.
 */
public final class MareaInteriorRuntime {
    private static final String NPC_TAG_PREFIX = "autoptu:npc:";

    private static final Map<String, BlockPos> NPC_WORK_POSITIONS = positions();

    private MareaInteriorRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("ouros")
                        .then(CommandManager.literal("world")
                                .then(CommandManager.literal("marea_interior")
                                        .then(CommandManager.literal("build")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                .executes(context -> build(context.getSource())))
                                        .then(CommandManager.literal("visit")
                                                .executes(context -> visit(context.getSource())))
                                        .then(CommandManager.literal("residents")
                                                .executes(context -> residents(context.getSource())))))));
    }

    private static int build(ServerCommandSource source) {
        ServerWorld world = source.getServer().getOverworld();
        MareaInteriorBuilder.BuildResult result = MareaInteriorBuilder.build(world);
        int spawned = spawnResidents(world);
        source.sendFeedback(() -> Text.literal("Marea Interior built at fixed Ouros coordinates. Sites: "
                + result.builtSiteIds().size() + "; canonical resident actors created: " + spawned + "."), false);
        source.sendFeedback(() -> Text.literal("Use /ouros world marea_interior visit to enter Puerto Bruma."), false);
        return 1;
    }

    private static int visit(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Marea Interior visit must be requested by a player."));
            return 0;
        }
        ServerWorld world = source.getServer().getOverworld();
        BlockPos anchor = site("ouros.marea.puerto_bruma");
        player.teleport(
                world,
                anchor.getX() + 0.5D,
                anchor.getY() + 2.0D,
                anchor.getZ() + 0.5D,
                Set.of(),
                0.0F,
                0.0F
        );
        player.sendMessage(Text.literal("Puerto Bruma — Marea Interior"), false);
        player.sendMessage(Text.literal("Market Hall, Field Office, Tideglass Archive, Battle Yard, clinic and ferry landing are all physical sites around you."), false);
        return 1;
    }

    private static int residents(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Canonical Marea residents: " + CanonicalNpcCatalogue.DEFAULT.npcs().size()), false);
        for (var npc : CanonicalNpcCatalogue.DEFAULT.npcs()) {
            source.sendFeedback(() -> Text.literal(npc.displayName() + " — " + npc.role()
                    + " — " + String.join(" / ", npc.classConcepts())
                    + " — partner: " + npc.companionName() + " the " + npc.companionSpeciesId()), false);
        }
        return 1;
    }

    private static int spawnResidents(ServerWorld world) {
        int spawned = 0;
        for (var npc : CanonicalNpcCatalogue.DEFAULT.npcs()) {
            BlockPos position = NPC_WORK_POSITIONS.get(npc.npcId());
            if (position == null) continue;
            if (findExisting(world, position, npc.npcId()) != null) continue;

            VillagerEntity villager = new VillagerEntity(EntityType.VILLAGER, world);
            villager.refreshPositionAndAngles(
                    position.getX() + 0.5D,
                    position.getY() + 1.0D,
                    position.getZ() + 0.5D,
                    180.0F,
                    0.0F
            );
            villager.setPersistent();
            if (!world.spawnEntity(villager)) continue;
            FabricNpcDialogueRuntime.bind(villager, npc.npcId());
            villager.addCommandTag("ouros:marea-resident");
            spawned++;
        }
        return spawned;
    }

    private static VillagerEntity findExisting(ServerWorld world, BlockPos position, String npcId) {
        String tag = NPC_TAG_PREFIX + npcId;
        Box area = new Box(position).expand(12.0D, 8.0D, 12.0D);
        return world.getEntitiesByType(
                        EntityType.VILLAGER,
                        area,
                        villager -> villager.getCommandTags().contains(tag)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static Map<String, BlockPos> positions() {
        LinkedHashMap<String, BlockPos> values = new LinkedHashMap<>();
        values.put("ouros.npc.mara_veyra", site("ouros.marea.marea_field_office").add(0, 0, 1));
        values.put("ouros.npc.ivo_serrat", site("ouros.marea.bruma_market_hall").add(0, 0, 1));
        values.put("ouros.npc.nerea_sol", site("ouros.marea.estacion_mirador").add(1, 0, 1));
        values.put("ouros.npc.taro_min", site("ouros.marea.tideglass_archive").add(0, 0, 1));
        values.put("ouros.npc.sela_orrin", site("ouros.marea.bruma_battle_yard").add(-7, 0, -5));
        values.put("ouros.npc.lia_morn", site("ouros.marea.ferry_landing").add(-4, 0, 2));
        values.put("ouros.npc.mina_cors", site("ouros.marea.ferry_landing").add(4, 0, -3));
        values.put("ouros.npc.oren_vale", site("ouros.marea.clinic").add(0, 0, 1));
        values.put("ouros.npc.teo_lark", new BlockPos(2043, 72, 2029));
        values.put("ouros.npc.alba_rios", site("ouros.marea.loma_clara").add(11, 0, 10));
        values.put("ouros.npc.brin_havel", site("ouros.marea.loma_storehouse").add(0, 0, 1));
        values.put("ouros.npc.jo_venn", site("ouros.marea.loma_field_school").add(0, 0, 1));
        values.put("ouros.npc.ema_rey", site("ouros.marea.estacion_mirador").add(5, 0, 10));
        values.put("ouros.npc.pia_min", site("ouros.marea.tideglass_archive").add(2, 0, 1));
        values.put("ouros.npc.jace_orrin", site("ouros.marea.bruma_battle_yard").add(7, 0, 5));
        return Map.copyOf(values);
    }

    private static BlockPos site(String siteId) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(siteId)
                .orElseThrow(() -> new IllegalStateException("missing canonical site: " + siteId));
        return new BlockPos(site.x(), site.y(), site.z());
    }
}
