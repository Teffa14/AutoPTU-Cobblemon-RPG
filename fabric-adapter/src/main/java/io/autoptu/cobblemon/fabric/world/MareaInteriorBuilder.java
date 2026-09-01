package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the first fixed-coordinate Marea Interior presentation slice.
 *
 * The builder owns Minecraft blocks only. Canonical location identity is defined by
 * CanonicalWorldMapCatalogue; quest/world truth remains in server-owned RPG state.
 */
public final class MareaInteriorBuilder {
    private MareaInteriorBuilder() {}

    public static BuildResult build(ServerWorld world) {
        List<String> builtSites = new ArrayList<>();

        buildPuertoBruma(world);
        builtSites.add("ouros.marea.puerto_bruma");
        buildSendero(world);
        builtSites.add("ouros.marea.sendero_vidrio");
        buildLomaClara(world);
        builtSites.add("ouros.marea.loma_clara");
        buildMirador(world);
        builtSites.add("ouros.marea.estacion_mirador");

        return new BuildResult(List.copyOf(builtSites), anchor("ouros.marea.puerto_bruma"));
    }

    private static void buildPuertoBruma(ServerWorld world) {
        BlockPos center = anchor("ouros.marea.puerto_bruma");
        platform(world, center.add(-31, 0, -31), center.add(31, 0, 31), Blocks.STONE_BRICKS, Blocks.STONE);

        // Main cross-streets keep every first-slice service visibly connected.
        road(world, new BlockPos(2017, 72, 2048), new BlockPos(2079, 72, 2048), 3, Blocks.SMOOTH_STONE);
        road(world, new BlockPos(2048, 72, 2017), new BlockPos(2048, 72, 2079), 3, Blocks.SMOOTH_STONE);

        hall(world, anchor("ouros.marea.bruma_market_hall"), 13, 9, 5, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS);
        hall(world, anchor("ouros.marea.marea_field_office"), 9, 8, 5, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS);
        hall(world, anchor("ouros.marea.tideglass_archive"), 10, 8, 6, Blocks.DARK_OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES);
        hall(world, anchor("ouros.marea.clinic"), 10, 8, 5, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_STONE, Blocks.WHITE_CONCRETE);

        buildBattleYard(world, anchor("ouros.marea.bruma_battle_yard"));
        buildFerryLanding(world, anchor("ouros.marea.ferry_landing"));
        buildRepairRow(world, new BlockPos(2043, 72, 2029));

        lamp(world, center.add(0, 1, 0));
        lamp(world, center.add(14, 1, 0));
        lamp(world, center.add(-14, 1, 0));
        lamp(world, center.add(0, 1, 14));
        lamp(world, center.add(0, 1, -14));
    }

    private static void buildBattleYard(ServerWorld world, BlockPos center) {
        platform(world, center.add(-11, 0, -9), center.add(11, 0, 9), Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS);
        for (int x = -10; x <= 10; x++) {
            world.setBlockState(center.add(x, 1, -8), Blocks.OAK_FENCE.getDefaultState());
            world.setBlockState(center.add(x, 1, 8), Blocks.OAK_FENCE.getDefaultState());
        }
        for (int z = -8; z <= 8; z++) {
            world.setBlockState(center.add(-10, 1, z), Blocks.OAK_FENCE.getDefaultState());
            world.setBlockState(center.add(10, 1, z), Blocks.OAK_FENCE.getDefaultState());
        }
        // Doorways.
        world.setBlockState(center.add(0, 1, -8), Blocks.AIR.getDefaultState());
        world.setBlockState(center.add(0, 1, 8), Blocks.AIR.getDefaultState());
        // Battle presentation markings only; AutoPTU owns tactical coordinates.
        for (int x = -7; x <= 7; x++) {
            world.setBlockState(center.add(x, 1, 0), Blocks.WHITE_CARPET.getDefaultState());
        }
        world.setBlockState(center.add(-6, 1, -4), Blocks.BLUE_CARPET.getDefaultState());
        world.setBlockState(center.add(6, 1, 4), Blocks.RED_CARPET.getDefaultState());
        lamp(world, center.add(-9, 1, -7));
        lamp(world, center.add(9, 1, 7));
    }

    private static void buildFerryLanding(ServerWorld world, BlockPos center) {
        // Fixed quay/platform. It does not author ferry service state.
        platform(world, center.add(-8, 0, -6), center.add(8, 0, 6), Blocks.STONE_BRICKS, Blocks.COBBLESTONE);
        for (int z = -14; z <= -6; z++) {
            for (int x = -2; x <= 2; x++) {
                world.setBlockState(center.add(x, 0, z), Blocks.SPRUCE_PLANKS.getDefaultState());
                world.setBlockState(center.add(x, -1, z), Blocks.SPRUCE_LOG.getDefaultState());
            }
        }
        for (int z = -13; z <= -7; z += 3) {
            world.setBlockState(center.add(-3, 0, z), Blocks.SPRUCE_FENCE.getDefaultState());
            world.setBlockState(center.add(3, 0, z), Blocks.SPRUCE_FENCE.getDefaultState());
        }
    }

    private static void buildRepairRow(ServerWorld world, BlockPos center) {
        for (int i = 0; i < 3; i++) {
            BlockPos shop = center.add((i - 1) * 8, 0, 0);
            hall(world, shop, 7, 6, 4, Blocks.BRICKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS);
        }
    }

    private static void buildSendero(ServerWorld world) {
        BlockPos start = new BlockPos(2048, 73, 2080);
        BlockPos lower = anchor("ouros.marea.sendero_vidrio");
        BlockPos crossing = anchor("ouros.marea.sendero_crossing");
        BlockPos junction = new BlockPos(2054, 84, 2188);
        BlockPos loma = anchor("ouros.marea.loma_clara").add(0, 0, -25);
        BlockPos mirador = anchor("ouros.marea.estacion_mirador");

        road(world, start, lower, 2, Blocks.DIRT_PATH);
        road(world, lower, crossing, 2, Blocks.DIRT_PATH);
        road(world, crossing, junction, 2, Blocks.DIRT_PATH);
        road(world, junction, loma, 2, Blocks.DIRT_PATH);
        road(world, junction, mirador.add(-11, -1, 8), 2, Blocks.COARSE_DIRT);

        // Seasonal crossing is a visible ford. Water is presentation terrain only.
        for (int x = -7; x <= 7; x++) {
            for (int z = -3; z <= 3; z++) {
                world.setBlockState(crossing.add(x, -1, z), Blocks.STONE.getDefaultState());
                world.setBlockState(crossing.add(x, 0, z), Blocks.WATER.getDefaultState());
            }
        }
        for (int z = -2; z <= 2; z++) {
            world.setBlockState(crossing.add(0, 0, z), Blocks.COBBLESTONE.getDefaultState());
            world.setBlockState(crossing.add(1, 0, z), Blocks.COBBLESTONE.getDefaultState());
        }
        lamp(world, lower.add(2, 1, 0));
        lamp(world, junction.add(2, 1, 0));
    }

    private static void buildLomaClara(ServerWorld world) {
        BlockPos center = anchor("ouros.marea.loma_clara");
        platform(world, center.add(-28, 0, -22), center.add(28, 0, 22), Blocks.GRASS_BLOCK, Blocks.DIRT);
        road(world, center.add(-27, 0, 0), center.add(27, 0, 0), 2, Blocks.DIRT_PATH);
        road(world, center.add(0, 0, -21), center.add(0, 0, 21), 2, Blocks.DIRT_PATH);

        hall(world, anchor("ouros.marea.loma_storehouse"), 11, 8, 5, Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS);
        hall(world, anchor("ouros.marea.loma_communal_kitchen"), 9, 7, 5, Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS);
        hall(world, anchor("ouros.marea.loma_field_school"), 9, 8, 5, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.OAK_PLANKS);

        // Small authored field strips provide context without encoding crop yields or mechanics.
        for (int x = 2052; x <= 2071; x++) {
            for (int z = 2229; z <= 2241; z++) {
                BlockPos pos = new BlockPos(x, 86, z);
                world.setBlockState(pos, ((x + z) & 1) == 0 ? Blocks.FARMLAND.getDefaultState() : Blocks.DIRT.getDefaultState());
                world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState());
            }
        }
        lamp(world, center.add(0, 1, 0));
    }

    private static void buildMirador(ServerWorld world) {
        BlockPos station = anchor("ouros.marea.estacion_mirador");
        platform(world, station.add(-17, 0, -15), station.add(17, 0, 15), Blocks.STONE, Blocks.COBBLESTONE);
        hall(world, station, 13, 10, 6, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES);
        hall(world, station.add(5, 0, 10), 7, 6, 4, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS);

        BlockPos mast = anchor("ouros.marea.mirador_weather_mast");
        for (int y = 0; y <= 11; y++) {
            world.setBlockState(mast.up(y), Blocks.IRON_BARS.getDefaultState());
        }
        world.setBlockState(mast.up(12), Blocks.LIGHTNING_ROD.getDefaultState());
        for (int x = -2; x <= 2; x++) {
            world.setBlockState(mast.add(x, 6, 0), Blocks.IRON_BARS.getDefaultState());
        }

        BlockPos transect = anchor("ouros.marea.mirador_transect");
        road(world, station.add(-7, 0, 7), transect, 1, Blocks.COARSE_DIRT);
        world.setBlockState(transect, Blocks.MOSSY_COBBLESTONE.getDefaultState());
        lamp(world, transect.up());
    }

    private static void platform(ServerWorld world, BlockPos min, BlockPos max, Block surface, Block foundation) {
        int minX = Math.min(min.getX(), max.getX());
        int maxX = Math.max(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxZ = Math.max(min.getZ(), max.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos ground = new BlockPos(x, minY, z);
                world.setBlockState(ground, surface.getDefaultState());
                world.setBlockState(ground.down(), foundation.getDefaultState());
                for (int y = 1; y <= 7; y++) {
                    world.setBlockState(ground.up(y), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void hall(ServerWorld world, BlockPos center, int width, int depth, int height, Block wall, Block floor, Block roof) {
        int halfW = width / 2;
        int halfD = depth / 2;
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfD; z <= halfD; z++) {
                world.setBlockState(center.add(x, 0, z), floor.getDefaultState());
                for (int y = 1; y <= height; y++) {
                    boolean boundary = x == -halfW || x == halfW || z == -halfD || z == halfD;
                    world.setBlockState(center.add(x, y, z), boundary ? wall.getDefaultState() : Blocks.AIR.getDefaultState());
                }
                world.setBlockState(center.add(x, height + 1, z), roof.getDefaultState());
            }
        }
        // South entrance and a small window rhythm.
        world.setBlockState(center.add(0, 1, -halfD), Blocks.AIR.getDefaultState());
        world.setBlockState(center.add(0, 2, -halfD), Blocks.AIR.getDefaultState());
        if (halfW >= 3) {
            world.setBlockState(center.add(-2, 2, -halfD), Blocks.GLASS_PANE.getDefaultState());
            world.setBlockState(center.add(2, 2, -halfD), Blocks.GLASS_PANE.getDefaultState());
        }
    }

    private static void road(ServerWorld world, BlockPos start, BlockPos end, int halfWidth, Block surface) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) steps = 1;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.round(start.getX() + dx * t);
            int y = (int) Math.round(start.getY() + dy * t);
            int z = (int) Math.round(start.getZ() + dz * t);
            for (int ox = -halfWidth; ox <= halfWidth; ox++) {
                for (int oz = -halfWidth; oz <= halfWidth; oz++) {
                    if (Math.abs(ox) + Math.abs(oz) > halfWidth + 1) continue;
                    BlockPos ground = new BlockPos(x + ox, y, z + oz);
                    world.setBlockState(ground, surface.getDefaultState());
                    world.setBlockState(ground.down(), Blocks.STONE.getDefaultState());
                    world.setBlockState(ground.up(), Blocks.AIR.getDefaultState());
                    world.setBlockState(ground.up(2), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void lamp(ServerWorld world, BlockPos base) {
        world.setBlockState(base, Blocks.COBBLESTONE_WALL.getDefaultState());
        world.setBlockState(base.up(), Blocks.OAK_FENCE.getDefaultState());
        world.setBlockState(base.up(2), Blocks.LANTERN.getDefaultState());
    }

    private static BlockPos anchor(String siteId) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(siteId)
                .orElseThrow(() -> new IllegalStateException("missing canonical site: " + siteId));
        return new BlockPos(site.x(), site.y(), site.z());
    }

    public record BuildResult(List<String> builtSiteIds, BlockPos puertoBrumaAnchor) {
        public BuildResult {
            builtSiteIds = List.copyOf(builtSiteIds);
        }
    }
}
