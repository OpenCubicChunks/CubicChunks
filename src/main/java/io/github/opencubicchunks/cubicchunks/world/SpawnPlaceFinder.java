package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SpawnPlaceFinder {

    private SpawnPlaceFinder() {
        throw new Error();
    }

    private static final int MIN_FREE_SPACE_SPAWN = 32;
/*
    public static BlockPos getRandomizedSpawnPoint(World world) {
        //TODO: uses getTopSolidOrLiquidBlock() ... not good
        BlockPos ret = world.getSpawnPoint();

        CubicChunks.LOGGER.trace("Finding spawnpoint starting from {}", ret);

        boolean isAdventure = world.getWorldInfo().getGameType() == GameType.ADVENTURE;
        int spawnFuzz;
        if (world instanceof ServerWorld) {
            spawnFuzz = world.getWorldType().getSpawnFuzz((ServerWorld) world,
                    Objects.requireNonNull(world.getServer()));
        } else {
            spawnFuzz = 1;
        }
        int border = MathHelper.floor(world.getWorldBorder().getClosestDistance(ret.getX(), ret.getZ()));
        if (border < spawnFuzz) {
            spawnFuzz = border;
        }

        if (!world.provider.isNether() && !isAdventure && spawnFuzz != 0) {
            if (spawnFuzz < 2) {
                spawnFuzz = 2;
            }
            int spawnFuzzHalf = spawnFuzz / 2;
            CubicChunks.LOGGER.trace("Running bisect with spawn fizz {}", spawnFuzz);
            ret = getTopBlockBisect(world, ret.add(
                    world.rand.nextInt(spawnFuzzHalf) - spawnFuzz,
                    0,
                    world.rand.nextInt(spawnFuzzHalf) - spawnFuzz
            ));
            if (ret == null) {
                ret = world.getSpawnPoint();
                CubicChunks.LOGGER.trace("No spawnpoint place found starting at {}, spawning at {}", ret, ret);
            } else {
                ret = ret.up();
            }
        }

        return ret;
    }
*/
    @Nullable
    public static BlockPos getTopBlockBisect(World world, BlockPos pos) {
        BlockPos minPos, maxPos;
        if (findNonEmpty(world, pos) == null) {
            CubicChunks.LOGGER.trace("Starting bisect with empty space at init {}", pos);
            maxPos = pos;
            minPos = findMinPos(world, pos);
            CubicChunks.LOGGER.trace("Found minPos {} and maxPos {}", minPos, maxPos);
        } else {
            CubicChunks.LOGGER.trace("Starting bisect without empty space at init {}", pos);
            minPos = pos;
            maxPos = findMaxPos(world, pos);
            CubicChunks.LOGGER.trace("Found minPos {} and maxPos {}", minPos, maxPos);
        }
        if (minPos == null || maxPos == null) {
            CubicChunks.LOGGER.error("No suitable spawn found, using original input {} (min={}, max={})", pos, minPos, maxPos);
            return null;
        }
        assert findNonEmpty(world, maxPos) == null && findNonEmpty(world, minPos) != null;
        return bisect(world, minPos.down(MIN_FREE_SPACE_SPAWN), maxPos.up(MIN_FREE_SPACE_SPAWN));
    }

    @Nullable
    private static BlockPos bisect(World world, BlockPos min, BlockPos max) {
        while (min.getY() < max.getY() - 1) {
            CubicChunks.LOGGER.trace("Bisect step with min={}, max={}", min, max);
            BlockPos middle = middleY(min, max);
            if (findNonEmpty(world, middle) != null) {
                // middle has solid space, so it can be used as new minimum
                min = middle;
            } else {
                // middle is empty, so can be used as new maximum
                max = middle;
            }
        }
        // now max should contain the all-empty part, but min should still have filled part.
        // take the block above the non-empty part of min
        return findNonEmpty(world, min);
    }

    private static BlockPos middleY(BlockPos min, BlockPos max) {
        return new BlockPos(min.getX(), (int) ((min.getY() + (long) max.getY()) >> 1), min.getZ());
    }

    @Nullable
    private static BlockPos findMinPos(World world, BlockPos pos) {
        // go down twice as much each time until we hit filled space
        double dy = 16;
        while (findNonEmpty(world, inWorldUp(world, pos, -dy)) == null) {
            if (dy > Integer.MAX_VALUE) {
                CubicChunks.LOGGER.trace("Error finding spawn point: can't find solid start height at {}", pos);
                return null;
            }
            dy *= 2;
        }
        return inWorldUp(world, pos, -dy);
    }

    @Nullable
    private static BlockPos findMaxPos(World world, BlockPos pos) {
        // go up twice as much each time until we hit empty space
        double dy = 16;
        while (findNonEmpty(world, inWorldUp(world, pos, dy)) != null) {
            if (dy > Integer.MAX_VALUE) {
                CubicChunks.LOGGER.trace("Error finding spawn point: can't find non-solid end height at {}", pos);
                return null;
            }
            dy *= 2;
        }
        return inWorldUp(world, pos, dy);
    }

    @Nullable
    private static BlockPos findNonEmpty(World world, BlockPos pos) {
        pos = pos.down(MIN_FREE_SPACE_SPAWN);
        for (int i = 0; i < MIN_FREE_SPACE_SPAWN * 2; i++, pos = pos.up()) {
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos inWorldUp(World world, BlockPos original, double up) {
        int y = (int) (original.getY() + up);
        //y = MathHelper.clamp(y, ((ICubicWorld) world).getMinHeight(), ((ICubicWorld) world).getMaxHeight());
        return new BlockPos(original.getX(), y, original.getZ());
    }
}