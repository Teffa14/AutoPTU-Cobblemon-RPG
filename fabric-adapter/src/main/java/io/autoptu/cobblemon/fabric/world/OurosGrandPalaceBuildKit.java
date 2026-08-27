package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Shared authored primitives for the Ouros Grand Palace. */
final class OurosGrandPalaceBuildKit {
    private OurosGrandPalaceBuildKit() {}

    static void fill(ServerWorld world, BlockPos o,
                     int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.setBlockState(o.add(x, y, z), state);
                }
            }
        }
    }

    static void clear(ServerWorld world, BlockPos o,
                      int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(world, o, x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
    }

    static BlockState stair(BlockState state, Direction facing) {
        return state.with(Properties.HORIZONTAL_FACING, facing);
    }

    static BlockState log(BlockState state, Direction.Axis axis) {
        return state.with(Properties.AXIS, axis);
    }

    static BlockState litLantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(Properties.HANGING, hanging);
    }

    static BlockState soulLantern(boolean hanging) {
        return Blocks.SOUL_LANTERN.getDefaultState().with(Properties.HANGING, hanging);
    }

    static BlockState litCandle(BlockState candle) {
        return candle.with(Properties.LIT, true);
    }

    static void roomShell(ServerWorld world, BlockPos o, Room r,
                          BlockState wall, BlockState pilaster, BlockState floorA,
                          BlockState floorB, BlockState ceiling, BlockState cornice) {
        patternedFloor(world, o, r, floorA, floorB);
        for (int y = r.floorY() + 1; y <= r.ceilingY(); y++) {
            for (int x = r.minX(); x <= r.maxX(); x++) {
                world.setBlockState(o.add(x, y, r.minZ()), wall);
                world.setBlockState(o.add(x, y, r.maxZ()), wall);
            }
            for (int z = r.minZ(); z <= r.maxZ(); z++) {
                world.setBlockState(o.add(r.minX(), y, z), wall);
                world.setBlockState(o.add(r.maxX(), y, z), wall);
            }
        }

        for (int x : new int[]{r.minX() + 2, r.maxX() - 2}) {
            for (int z : new int[]{r.minZ() + 2, r.maxZ() - 2}) {
                fill(world, o, x, r.floorY() + 1, z, x, r.ceilingY() - 1, z, pilaster);
            }
        }

        fill(world, o, r.minX() + 1, r.ceilingY(), r.minZ() + 1,
                r.maxX() - 1, r.ceilingY(), r.maxZ() - 1, ceiling);
        corniceRing(world, o, r, cornice, r.floorY() + 2);
        corniceRing(world, o, r, cornice, r.ceilingY() - 1);
    }

    static void patternedFloor(ServerWorld world, BlockPos o, Room r, BlockState a, BlockState b) {
        for (int x = r.minX(); x <= r.maxX(); x++) {
            for (int z = r.minZ(); z <= r.maxZ(); z++) {
                int tile = Math.floorMod((x - r.minX()) / 2 + (z - r.minZ()) / 2, 2);
                world.setBlockState(o.add(x, r.floorY(), z), tile == 0 ? a : b);
            }
        }
    }

    static void insetCeiling(ServerWorld world, BlockPos o, Room r,
                             BlockState border, BlockState field, BlockState accent) {
        int y = r.ceilingY();
        fill(world, o, r.minX() + 1, y, r.minZ() + 1, r.maxX() - 1, y, r.maxZ() - 1, field);
        for (int layer = 0; layer < 3; layer++) {
            int x1 = r.minX() + 1 + layer;
            int x2 = r.maxX() - 1 - layer;
            int z1 = r.minZ() + 1 + layer;
            int z2 = r.maxZ() - 1 - layer;
            BlockState state = layer == 1 ? accent : border;
            for (int x = x1; x <= x2; x++) {
                world.setBlockState(o.add(x, y, z1), state);
                world.setBlockState(o.add(x, y, z2), state);
            }
            for (int z = z1; z <= z2; z++) {
                world.setBlockState(o.add(x1, y, z), state);
                world.setBlockState(o.add(x2, y, z), state);
            }
        }
    }

    static void glazedCeiling(ServerWorld world, BlockPos o, Room r,
                              BlockState frame, BlockState glassA, BlockState glassB) {
        int y = r.ceilingY();
        for (int x = r.minX() + 1; x <= r.maxX() - 1; x++) {
            for (int z = r.minZ() + 1; z <= r.maxZ() - 1; z++) {
                boolean beam = Math.floorMod(x - r.minX(), 5) == 0 || Math.floorMod(z - r.minZ(), 5) == 0;
                if (beam) {
                    world.setBlockState(o.add(x, y, z), frame);
                } else {
                    world.setBlockState(o.add(x, y, z), Math.floorMod(x * 7 + z * 11, 7) == 0 ? glassB : glassA);
                }
            }
        }
    }

    static void corniceRing(ServerWorld world, BlockPos o, Room r, BlockState state, int y) {
        for (int x = r.minX() + 1; x <= r.maxX() - 1; x++) {
            world.setBlockState(o.add(x, y, r.minZ() + 1), state);
            world.setBlockState(o.add(x, y, r.maxZ() - 1), state);
        }
        for (int z = r.minZ() + 1; z <= r.maxZ() - 1; z++) {
            world.setBlockState(o.add(r.minX() + 1, y, z), state);
            world.setBlockState(o.add(r.maxX() - 1, y, z), state);
        }
    }

    static void doorNorth(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = (r.minX() + r.maxX()) / 2;
        clear(world, o, c - width / 2, r.floorY() + 1, r.minZ(), c + width / 2, r.floorY() + height, r.minZ());
    }

    static void doorSouth(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = (r.minX() + r.maxX()) / 2;
        clear(world, o, c - width / 2, r.floorY() + 1, r.maxZ(), c + width / 2, r.floorY() + height, r.maxZ());
    }

    static void doorWest(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = (r.minZ() + r.maxZ()) / 2;
        clear(world, o, r.minX(), r.floorY() + 1, c - width / 2, r.minX(), r.floorY() + height, c + width / 2);
    }

    static void doorEast(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = (r.minZ() + r.maxZ()) / 2;
        clear(world, o, r.maxX(), r.floorY() + 1, c - width / 2, r.maxX(), r.floorY() + height, c + width / 2);
    }

    static void grandDoorFrameNorth(ServerWorld world, BlockPos o, Room r, BlockState frame) {
        int c = (r.minX() + r.maxX()) / 2;
        int y = r.floorY();
        fill(world, o, c - 4, y + 1, r.minZ() + 1, c - 3, y + 8, r.minZ() + 1, frame);
        fill(world, o, c + 3, y + 1, r.minZ() + 1, c + 4, y + 8, r.minZ() + 1, frame);
        fill(world, o, c - 4, y + 8, r.minZ() + 1, c + 4, y + 9, r.minZ() + 1, frame);
    }

    static void tallWindowX(ServerWorld world, BlockPos o, int x, int centerZ, int floorY,
                            int height, BlockState frame, BlockState glass) {
        int base = floorY + 2;
        fill(world, o, x, base, centerZ - 3, x, base + height, centerZ + 3, frame);
        fill(world, o, x, base + 1, centerZ - 2, x, base + height - 2, centerZ + 2, glass);
        for (int z : new int[]{centerZ - 2, centerZ, centerZ + 2}) {
            fill(world, o, x, base + 1, z, x, base + height - 2, z, frame);
        }
        fill(world, o, x, base + height - 1, centerZ - 1, x, base + height, centerZ + 1, glass);
    }

    static void tallWindowZ(ServerWorld world, BlockPos o, int centerX, int z, int floorY,
                            int height, BlockState frame, BlockState glass) {
        int base = floorY + 2;
        fill(world, o, centerX - 3, base, z, centerX + 3, base + height, z, frame);
        fill(world, o, centerX - 2, base + 1, z, centerX + 2, base + height - 2, z, glass);
        for (int x : new int[]{centerX - 2, centerX, centerX + 2}) {
            fill(world, o, x, base + 1, z, x, base + height - 2, z, frame);
        }
        fill(world, o, centerX - 1, base + height - 1, z, centerX + 1, base + height, z, glass);
    }

    static void wallPanelZ(ServerWorld world, BlockPos o, int centerX, int y, int z,
                           int width, int height, BlockState frame, BlockState field, BlockState ornament) {
        int half = width / 2;
        fill(world, o, centerX - half, y, z, centerX + half, y + height, z, frame);
        fill(world, o, centerX - half + 1, y + 1, z, centerX + half - 1, y + height - 1, z, field);
        for (int dx = -half + 2; dx <= half - 2; dx += 3) {
            world.setBlockState(o.add(centerX + dx, y + height - 2, z), ornament);
        }
    }

    static void wallPanelX(ServerWorld world, BlockPos o, int x, int y, int centerZ,
                           int width, int height, BlockState frame, BlockState field, BlockState ornament) {
        int half = width / 2;
        fill(world, o, x, y, centerZ - half, x, y + height, centerZ + half, frame);
        fill(world, o, x, y + 1, centerZ - half + 1, x, y + height - 1, centerZ + half - 1, field);
        for (int dz = -half + 2; dz <= half - 2; dz += 3) {
            world.setBlockState(o.add(x, y + height - 2, centerZ + dz), ornament);
        }
    }

    static void chandelier(ServerWorld world, BlockPos o, int x, int ceilingY, int z,
                           int drop, BlockState metal, BlockState glass) {
        int hubY = ceilingY - drop;
        for (int y = hubY + 1; y < ceilingY; y++) {
            world.setBlockState(o.add(x, y, z), Blocks.CHAIN.getDefaultState());
        }
        world.setBlockState(o.add(x, hubY, z), glass);
        for (Direction d : Direction.Type.HORIZONTAL) {
            int dx = d.getOffsetX();
            int dz = d.getOffsetZ();
            world.setBlockState(o.add(x + dx, hubY, z + dz), metal);
            world.setBlockState(o.add(x + dx * 2, hubY, z + dz * 2), metal);
            world.setBlockState(o.add(x + dx * 2, hubY + 1, z + dz * 2), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(x + dx * 2, hubY + 2, z + dz * 2), litCandle(Blocks.CANDLE.getDefaultState()));
        }
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                world.setBlockState(o.add(x + dx, hubY - 1, z + dz), glass);
            }
        }
    }

    static void wallCandelabra(ServerWorld world, BlockPos o, int x, int y, int z, Direction outward,
                               BlockState bracket) {
        int dx = outward.getOffsetX();
        int dz = outward.getOffsetZ();
        world.setBlockState(o.add(x, y, z), bracket);
        world.setBlockState(o.add(x + dx, y, z + dz), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 1, z + dz), litCandle(Blocks.CANDLE.getDefaultState()));
    }

    static void table(ServerWorld world, BlockPos o, int x1, int z1, int x2, int z2, int y,
                      BlockState top, BlockState leg) {
        fill(world, o, x1, y, z1, x2, y, z2, top);
        for (int x : new int[]{x1, x2}) {
            for (int z : new int[]{z1, z2}) {
                world.setBlockState(o.add(x, y - 1, z), leg);
            }
        }
    }

    static void chair(ServerWorld world, BlockPos o, int x, int y, int z, Direction facing,
                      BlockState seatStairs, BlockState back) {
        world.setBlockState(o.add(x, y, z), stair(seatStairs, facing));
        int bx = x - facing.getOffsetX();
        int bz = z - facing.getOffsetZ();
        world.setBlockState(o.add(bx, y + 1, bz), back);
    }

    static void bench(ServerWorld world, BlockPos o, int x1, int z1, int x2, int z2, int y,
                      BlockState seat, BlockState support) {
        fill(world, o, x1, y, z1, x2, y, z2, seat);
        world.setBlockState(o.add(x1, y - 1, z1), support);
        world.setBlockState(o.add(x2, y - 1, z2), support);
    }

    static void cabinetZ(ServerWorld world, BlockPos o, int x1, int x2, int y, int z,
                         int height, BlockState wood, BlockState shelf, boolean books) {
        fill(world, o, x1, y, z, x2, y + height, z, wood);
        if (x2 - x1 >= 2 && height >= 3) {
            fill(world, o, x1 + 1, y + 1, z, x2 - 1, y + height - 1, z,
                    books ? Blocks.BOOKSHELF.getDefaultState() : Blocks.BARREL.getDefaultState());
            for (int yy = y + 2; yy < y + height; yy += 2) {
                fill(world, o, x1 + 1, yy, z - 1, x2 - 1, yy, z - 1, shelf);
            }
        }
    }

    static void cabinetX(ServerWorld world, BlockPos o, int x, int y, int z1, int z2,
                         int height, BlockState wood, BlockState shelf, boolean books) {
        fill(world, o, x, y, z1, x, y + height, z2, wood);
        if (z2 - z1 >= 2 && height >= 3) {
            fill(world, o, x, y + 1, z1 + 1, x, y + height - 1, z2 - 1,
                    books ? Blocks.BOOKSHELF.getDefaultState() : Blocks.BARREL.getDefaultState());
            for (int yy = y + 2; yy < y + height; yy += 2) {
                fill(world, o, x - 1, yy, z1 + 1, x - 1, yy, z2 - 1, shelf);
            }
        }
    }

    static void pottedPlant(ServerWorld world, BlockPos o, int x, int y, int z, BlockState pot) {
        world.setBlockState(o.add(x, y, z), Blocks.POLISHED_GRANITE.getDefaultState());
        world.setBlockState(o.add(x, y + 1, z), pot);
    }

    static void globe(ServerWorld world, BlockPos o, int x, int y, int z) {
        world.setBlockState(o.add(x, y, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        world.setBlockState(o.add(x, y + 1, z), Blocks.CUT_COPPER_SLAB.getDefaultState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double d = dx * dx / 4.0 + dy * dy / 4.0 + dz * dz / 4.0;
                    if (d > 1.35) continue;
                    BlockState state = Math.floorMod(dx * 13 + dy * 17 + dz * 19, 5) == 0
                            ? Blocks.GREEN_TERRACOTTA.getDefaultState()
                            : Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState();
                    world.setBlockState(o.add(x + dx, y + 3 + dy, z + dz), state);
                }
            }
        }
        world.setBlockState(o.add(x - 2, y + 3, z), Blocks.CUT_COPPER_SLAB.getDefaultState());
        world.setBlockState(o.add(x + 2, y + 3, z), Blocks.CUT_COPPER_SLAB.getDefaultState());
    }

    static void coatOfArms(ServerWorld world, BlockPos o, int centerX, int y, int z) {
        BlockState pale = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState gold = Blocks.WAXED_CUT_COPPER.getDefaultState();
        BlockState red = Blocks.RED_GLAZED_TERRACOTTA.getDefaultState();
        fill(world, o, centerX - 5, y, z, centerX + 5, y + 7, z, Blocks.POLISHED_DIORITE.getDefaultState());
        clear(world, o, centerX - 4, y + 1, z - 1, centerX + 4, y + 6, z - 1);
        for (int dx : new int[]{-4, 4}) {
            fill(world, o, centerX + dx, y + 1, z - 1, centerX + dx, y + 6, z - 1, pale);
            world.setBlockState(o.add(centerX + dx / 2, y + 4, z - 1), gold);
        }
        fill(world, o, centerX - 2, y + 2, z - 1, centerX + 2, y + 5, z - 1, red);
        world.setBlockState(o.add(centerX, y + 6, z - 1), gold);
        world.setBlockState(o.add(centerX - 1, y + 1, z - 1), gold);
        world.setBlockState(o.add(centerX + 1, y + 1, z - 1), gold);
        world.setBlockState(o.add(centerX, y + 3, z - 2), Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
    }

    static void artPanelZ(ServerWorld world, BlockPos o, int centerX, int y, int z, int variant) {
        BlockState frame = Blocks.DARK_OAK_PLANKS.getDefaultState();
        fill(world, o, centerX - 3, y, z, centerX + 3, y + 5, z, frame);
        BlockState[] colors = {
                Blocks.BLUE_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(),
                Blocks.RED_TERRACOTTA.getDefaultState(),
                Blocks.CYAN_TERRACOTTA.getDefaultState(),
                Blocks.PURPLE_TERRACOTTA.getDefaultState(),
                Blocks.ORANGE_TERRACOTTA.getDefaultState()
        };
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int yy = y + 1; yy <= y + 4; yy++) {
                int idx = Math.floorMod((x - centerX) * 3 + yy + variant * 5, colors.length);
                world.setBlockState(o.add(x, yy, z - 1), colors[idx]);
            }
        }
    }

    static void harpsichord(ServerWorld world, BlockPos o, int x, int y, int z, Direction facing) {
        BlockState wood = Blocks.DARK_OAK_PLANKS.getDefaultState();
        fill(world, o, x - 3, y, z - 1, x + 3, y + 2, z + 1, wood);
        fill(world, o, x - 2, y + 3, z, x + 2, y + 4, z + 1, Blocks.DARK_OAK_TRAPDOOR.getDefaultState());
        for (int dx = -2; dx <= 2; dx++) {
            world.setBlockState(o.add(x + dx, y + 2, z - 2), Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            if (dx % 2 == 0) {
                world.setBlockState(o.add(x + dx, y + 3, z - 2), Blocks.POLISHED_BLACKSTONE_BUTTON.getDefaultState());
            }
        }
        for (int dx : new int[]{-2, 2}) {
            world.setBlockState(o.add(x + dx, y - 1, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        chair(world, o, x, y, z - 4, facing, Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
    }

    static void steppedArchZ(ServerWorld world, BlockPos o, int centerX, int y, int z,
                             int halfWidth, int height, BlockState state) {
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            int rise = (int) Math.round((1.0 - Math.abs(dx) / (double) (halfWidth + 1)) * height);
            fill(world, o, centerX + dx, y, z, centerX + dx, y + Math.max(1, rise), z, state);
        }
    }

    record Room(String name, int minX, int minZ, int maxX, int maxZ, int floorY, int ceilingY) {
        int centerX() { return (minX + maxX) / 2; }
        int centerZ() { return (minZ + maxZ) / 2; }
    }
}