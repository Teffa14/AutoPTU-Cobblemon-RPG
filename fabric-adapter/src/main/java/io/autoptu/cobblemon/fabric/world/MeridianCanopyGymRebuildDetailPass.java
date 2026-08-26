package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Human-scale architectural detail for the zero-base Meridian rebuild.
 *
 * Every element belongs to an existing room, roof, facade, service or landscape system. The goal is
 * density with structural logic: joinery, drainage, shutters, railings, nursery equipment, arena
 * furniture and lighting. This pass never invents PTU effects.
 */
public final class MeridianCanopyGymRebuildDetailPass {
    private static final BlockState FRAME_X = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState FRAME_Z = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);
    private static final BlockState HANGING_LANTERN = Blocks.LANTERN.getDefaultState()
            .with(Properties.HANGING, true);
    private static final BlockState SHUTTER_SOUTH = Blocks.DARK_OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
            .with(Properties.OPEN, true);
    private static final BlockState SHUTTER_NORTH = Blocks.DARK_OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.OPEN, true);
    private static final BlockState SHUTTER_EAST = Blocks.DARK_OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.EAST)
            .with(Properties.OPEN, true);
    private static final BlockState SHUTTER_WEST = Blocks.DARK_OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.WEST)
            .with(Properties.OPEN, true);

    private MeridianCanopyGymRebuildDetailPass() {}

    public static void apply(ServerWorld world, BlockPos o) {
        detailGatehouse(world, o);
        detailConservatory(world, o);
        detailBotanicalWing(world, o);
        detailHydroWing(world, o);
        detailArena(world, o);
        detailBackstage(world, o);
        detailLandscape(world, o);
    }

    private static void detailGatehouse(ServerWorld world, BlockPos o) {
        // Copper gutters follow both roof eaves and discharge into visible downpipes at the towers.
        for (int z = -26; z <= -16; z++) {
            world.setBlockState(o.add(-16, 9, z), Blocks.OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
            world.setBlockState(o.add(16, 9, z), Blocks.OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
        }
        for (int z : new int[]{-24, -18}) {
            for (int y = 3; y <= 8; y++) {
                world.setBlockState(o.add(-16, y, z), Blocks.OXIDIZED_COPPER_GRATE.getDefaultState());
                world.setBlockState(o.add(16, y, z), Blocks.OXIDIZED_COPPER_GRATE.getDefaultState());
            }
            world.setBlockState(o.add(-16, 2, z), Blocks.CAULDRON.getDefaultState());
            world.setBlockState(o.add(16, 2, z), Blocks.CAULDRON.getDefaultState());
        }

        // Window joinery. Open shutters sit on masonry, not in midair.
        for (int x : new int[]{-11, -8, 8, 11}) {
            world.setBlockState(o.add(x - 1, 3, -25), SHUTTER_SOUTH);
            world.setBlockState(o.add(x + 1, 3, -25), SHUTTER_SOUTH);
            world.setBlockState(o.add(x, 2, -25), Blocks.POLISHED_TUFF_SLAB.getDefaultState());
            world.setBlockState(o.add(x, 5, -25), Blocks.DARK_OAK_SLAB.getDefaultState());
        }

        // Ridge caps and finials are anchored to the actual ridge beam.
        for (int z = -25; z <= -17; z += 2) {
            world.setBlockState(o.add(0, 18, z), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
        }
        for (int z : new int[]{-25, -17}) {
            world.setBlockState(o.add(0, 19, z), Blocks.LIGHTNING_ROD.getDefaultState());
        }

        // Entry desk details and visitor storage.
        world.setBlockState(o.add(-4, 2, -18), Blocks.BARREL.getDefaultState());
        world.setBlockState(o.add(-3, 2, -18), Blocks.CHISELED_BOOKSHELF.getDefaultState());
        world.setBlockState(o.add(3, 2, -18), Blocks.ENDER_CHEST.getDefaultState());
        world.setBlockState(o.add(4, 2, -18), Blocks.BARREL.getDefaultState());
    }

    private static void detailConservatory(ServerWorld world, BlockPos o) {
        // Wall plates, gutters and roof-edge vents make the barrel vault feel constructed.
        for (int z = -15; z <= 8; z++) {
            world.setBlockState(o.add(-15, 13, z), FRAME_Z);
            world.setBlockState(o.add(15, 13, z), FRAME_Z);
            if (Math.floorMod(z, 3) == 0) {
                world.setBlockState(o.add(-14, 13, z), Blocks.OXIDIZED_COPPER_GRATE.getDefaultState());
                world.setBlockState(o.add(14, 13, z), Blocks.OXIDIZED_COPPER_GRATE.getDefaultState());
            }
        }

        // Supported hanging botany from the cross beams installed by structural QA.
        for (int[] p : new int[][]{{-4, -8}, {4, -8}, {-4, 4}, {4, 4}}) {
            world.setBlockState(o.add(p[0], 12, p[1]), Blocks.SPORE_BLOSSOM.getDefaultState());
        }

        // Balcony rail interruptions create observation bays with benches and plant records.
        for (int[] p : new int[][]{{-12, -2}, {12, -2}, {-9, 6}, {9, 6}}) {
            world.setBlockState(o.add(p[0], 9, p[1]), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(p[0], 10, p[1]), Blocks.LECTERN.getDefaultState());
            world.setBlockState(o.add(p[0] + (p[0] < 0 ? 1 : -1), 10, p[1]), Blocks.POTTED_FERN.getDefaultState());
        }

        // Water ring details remain outside circulation bridges.
        for (int[] p : new int[][]{{-5, -4}, {5, -4}, {-5, 1}, {5, 1}}) {
            world.setBlockState(o.add(p[0], 2, p[1]), Blocks.LILY_PAD.getDefaultState());
        }

        // Floor mosaic at the four cardinal bridge landings.
        for (int[] p : new int[][]{{-8, -2}, {8, -2}, {0, -8}, {0, 5}}) {
            world.setBlockState(o.add(p[0], 2, p[1]), Blocks.GREEN_GLAZED_TERRACOTTA.getDefaultState());
            world.setBlockState(o.add(p[0] + 1, 2, p[1]), Blocks.WHITE_GLAZED_TERRACOTTA.getDefaultState());
        }
    }

    private static void detailBotanicalWing(ServerWorld world, BlockPos o) {
        // Gallery rails and small sheltered work alcoves make the challenge wing inhabitable.
        for (int z : new int[]{-11, -9, -2, 0, 7, 9}) {
            for (int x = -28; x <= -18; x++) {
                if (world.getBlockState(o.add(x, 8, z)).isOf(Blocks.DARK_OAK_SLAB)) {
                    world.setBlockState(o.add(x, 9, z), Blocks.DARK_OAK_FENCE.getDefaultState());
                }
            }
        }

        // Shuttered nursery windows on each staggered terrace.
        int[][] windows = {
                {-30, -8, Direction.WEST.ordinal()}, {-30, -2, Direction.WEST.ordinal()},
                {-28, 3, Direction.WEST.ordinal()}, {-26, 9, Direction.WEST.ordinal()},
                {-18, -7, Direction.EAST.ordinal()}, {-18, 2, Direction.EAST.ordinal()}
        };
        for (int[] w : windows) {
            int x = w[0];
            int z = w[1];
            boolean westFace = w[2] == Direction.WEST.ordinal();
            BlockPos wall = o.add(x, 4, z);
            world.setBlockState(wall, Blocks.GLASS_PANE.getDefaultState());
            world.setBlockState(o.add(x, 4, z - 1), westFace ? SHUTTER_WEST : SHUTTER_EAST);
            world.setBlockState(o.add(x, 4, z + 1), westFace ? SHUTTER_WEST : SHUTTER_EAST);
        }

        // Nursery work surfaces and supplies.
        for (int z : new int[]{-10, -5, 1, 7}) {
            world.setBlockState(o.add(-22, 2, z), Blocks.COMPOSTER.getDefaultState());
            world.setBlockState(o.add(-21, 2, z), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(-20, 2, z), Blocks.FLOWER_POT.getDefaultState());
            world.setBlockState(o.add(-19, 2, z), Blocks.DARK_OAK_SLAB.getDefaultState());
        }

        // Roof ridges receive consistent caps and supported lanterns at gallery nodes.
        for (int[] p : new int[][]{{-26, -9, 11}, {-23, 1, 13}, {-22, 9, 15}}) {
            world.setBlockState(o.add(p[0], p[2], p[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
        }
        for (int z : new int[]{-7, 0, 7}) {
            world.setBlockState(o.add(-19, 9, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-19, 8, z), HANGING_LANTERN);
        }
    }

    private static void detailHydroWing(ServerWorld world, BlockPos o) {
        // Pipe-like copper service runs are supported against the masonry side wall.
        for (int z = -10; z <= 10; z += 4) {
            world.setBlockState(o.add(31, 5, z), Blocks.LIGHTNING_ROD.getDefaultState());
            world.setBlockState(o.add(31, 6, z), Blocks.COPPER_BULB.getDefaultState());
            world.setBlockState(o.add(30, 5, z), Blocks.OXIDIZED_COPPER_GRATE.getDefaultState());
        }

        // Bridge rails and inspection stations.
        for (int z : new int[]{-8, 0, 8}) {
            for (int x = 19; x <= 29; x++) {
                if (x == 23 || x == 25) {
                    continue;
                }
                world.setBlockState(o.add(x, 4, z - 1), Blocks.DARK_OAK_FENCE.getDefaultState());
                world.setBlockState(o.add(x, 4, z + 1), Blocks.DARK_OAK_FENCE.getDefaultState());
            }
            world.setBlockState(o.add(23, 4, z), Blocks.LECTERN.getDefaultState());
            world.setBlockState(o.add(26, 4, z), Blocks.BARREL.getDefaultState());
        }

        // Water ecology and maintenance clutter kept inside the channels/edges.
        for (int z : new int[]{-10, -5, 5, 10}) {
            world.setBlockState(o.add(20, 3, z), Blocks.LILY_PAD.getDefaultState());
            world.setBlockState(o.add(28, 3, z), Blocks.LILY_PAD.getDefaultState());
            world.setBlockState(o.add(22, 3, z), Blocks.CAULDRON.getDefaultState());
            world.setBlockState(o.add(27, 3, z), Blocks.COMPOSTER.getDefaultState());
        }

        // Copper gutters follow each sawtooth valley.
        for (int z : new int[]{-8, -2, 4, 10}) {
            for (int x = 18; x <= 30; x++) {
                world.setBlockState(o.add(x, 11, z), Blocks.OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
            }
        }
    }

    private static void detailArena(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);

        // Spectator rail follows supported bowl edges. Breaks are left at the entry and leader axes.
        int[][] railNodes = {
                {-21, -5}, {-19, -7}, {-14, -9}, {-6, -10}, {6, -10}, {14, -9}, {19, -7}, {21, -5},
                {-21, 5}, {-19, 7}, {-14, 9}, {-6, 10}, {6, 10}, {14, 9}, {19, 7}, {21, 5}
        };
        for (int[] p : railNodes) {
            world.setBlockState(c.add(p[0], 7, p[1]), Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(c.add(p[0], 8, p[1]), Blocks.COPPER_GRATE.getDefaultState());
        }

        // Officials' desks, trainer benches and equipment cabinets.
        for (int x : new int[]{-15, 15}) {
            world.setBlockState(c.add(x, 3, -7), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(c.add(x + (x < 0 ? 1 : -1), 3, -7), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(c.add(x, 4, -7), Blocks.LECTERN.getDefaultState());
            world.setBlockState(c.add(x, 3, 7), Blocks.BARREL.getDefaultState());
            world.setBlockState(c.add(x + (x < 0 ? 1 : -1), 3, 7), Blocks.CHEST.getDefaultState());
        }

        // Four ceremonial pylons frame the battle bowl. Banners stand on supported masonry caps.
        for (int[] p : new int[][]{{-17, -5}, {17, -5}, {-17, 5}, {17, 5}}) {
            world.setBlockState(c.add(p[0], 7, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
            world.setBlockState(c.add(p[0], 8, p[1]), Blocks.GREEN_BANNER.getDefaultState());
        }

        // Oculus edge lighting hangs from the already-supported roof ties.
        for (int[] p : new int[][]{{-10, 0}, {10, 0}, {0, -8}, {0, 8}}) {
            BlockPos support = c.add(p[0], 12, p[1]);
            if (!world.getBlockState(support).isAir()) {
                world.setBlockState(c.add(p[0], 11, p[1]), Blocks.CHAIN.getDefaultState());
                world.setBlockState(c.add(p[0], 10, p[1]), HANGING_LANTERN);
            }
        }

        // Leader apse gets a planted backdrop without intruding on the battle floor.
        for (int x = -5; x <= 5; x++) {
            world.setBlockState(c.add(x, 5, 10), Blocks.MOSS_BLOCK.getDefaultState());
            if (Math.floorMod(x, 2) == 0) {
                world.setBlockState(c.add(x, 6, 10), Blocks.FLOWERING_AZALEA.getDefaultState());
            }
        }
    }

    private static void detailBackstage(ServerWorld world, BlockPos o) {
        // Shelving rhythm and maintenance lighting along the service bar.
        for (int z = 16; z <= 28; z += 3) {
            world.setBlockState(o.add(-30, 4, z), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(-29, 4, z), Blocks.CHISELED_BOOKSHELF.getDefaultState());
            world.setBlockState(o.add(-27, 4, z), Blocks.SCAFFOLDING.getDefaultState());
            world.setBlockState(o.add(-26, 4, z), Blocks.COMPOSTER.getDefaultState());
        }
        for (int z : new int[]{17, 23, 29}) {
            world.setBlockState(o.add(-28, 7, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-28, 6, z), HANGING_LANTERN);
        }

        // Loading edge: carts are abstracted with slabs/trapdoors, but each sits on the gravel yard.
        for (int z : new int[]{19, 25}) {
            world.setBlockState(o.add(-33, 1, z), Blocks.SPRUCE_SLAB.getDefaultState());
            world.setBlockState(o.add(-32, 1, z), Blocks.SPRUCE_SLAB.getDefaultState());
            world.setBlockState(o.add(-33, 2, z), Blocks.SPRUCE_TRAPDOOR.getDefaultState());
        }
    }

    private static void detailLandscape(ServerWorld world, BlockPos o) {
        // Small rock/root compositions break the flat edge of the site and visually anchor trees.
        int[][] rocks = {
                {-25, -26}, {25, -26}, {-30, -5}, {30, -5}, {-23, 13}, {23, 13}
        };
        for (int[] p : rocks) {
            world.setBlockState(o.add(p[0], 1, p[1]), Blocks.MOSSY_COBBLESTONE.getDefaultState());
            world.setBlockState(o.add(p[0] + 1, 1, p[1]), Blocks.MOSSY_COBBLESTONE_SLAB.getDefaultState());
            world.setBlockState(o.add(p[0], 2, p[1] + 1), Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(o.add(p[0] - 1, 1, p[1]), Blocks.MANGROVE_ROOTS.getDefaultState());
            world.setBlockState(o.add(p[0] + 1, 1, p[1] + 1), Blocks.MOSS_CARPET.getDefaultState());
        }

        // Low path lanterns are mounted on walls, not floating beside the road.
        for (int[] p : new int[][]{{-12, -31}, {12, -31}, {-17, -25}, {17, -25}}) {
            world.setBlockState(o.add(p[0], 1, p[1]), Blocks.STONE_BRICK_WALL.getDefaultState());
            world.setBlockState(o.add(p[0], 2, p[1]), Blocks.LANTERN.getDefaultState());
        }
    }
}
