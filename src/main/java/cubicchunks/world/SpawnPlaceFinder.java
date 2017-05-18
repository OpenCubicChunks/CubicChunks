package cubicchunks.world;

import cubicchunks.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpawnPlaceFinder {

    private static final int MIN_FREE_SPACE_SPAWN = 32;

    public BlockPos getRandomizedSpawnPoint(ICubicWorld world) {
        //TODO: uses getTopSolidOrLiquidBlock() ... not good
        BlockPos ret = world.getSpawnPoint();

        boolean isAdventure = world.getWorldInfo().getGameType() == GameType.ADVENTURE;
        int spawnFuzz;
        if (world instanceof WorldServer) {
            spawnFuzz = world.getWorldType().getSpawnFuzz((WorldServer) world, world.getMinecraftServer());
        } else {
            spawnFuzz = 1;
        }
        int border = MathHelper.floor(world.getWorldBorder().getClosestDistance(ret.getX(), ret.getZ()));
        if (border < spawnFuzz) {
            spawnFuzz = border;
        }

        if (!world.getProvider().hasNoSky() && !isAdventure && spawnFuzz != 0) {
            if (spawnFuzz < 2) {
                spawnFuzz = 2;
            }
            int spawnFuzzHalf = spawnFuzz / 2;
            ret = getTopBlockBisect(world, ret.add(
                    world.getRand().nextInt(spawnFuzzHalf) - spawnFuzz,
                    0,
                    world.getRand().nextInt(spawnFuzzHalf) - spawnFuzz
            ));
        }

        return ret;
    }

    private BlockPos getTopBlockBisect(ICubicWorld world, BlockPos pos) {
        Chunk chunk = ((World) world).getChunkFromBlockCoords(pos);
        BlockPos minPos, maxPos;
        if (findEmpty(chunk, pos) != null) {
            maxPos = pos;
            minPos = findMinPos(chunk, pos);
        } else {
            minPos = pos;
            maxPos = findMaxPos(chunk, pos);
        }
        return bisect(chunk, minPos.down(MIN_FREE_SPACE_SPAWN), maxPos.up(MIN_FREE_SPACE_SPAWN));
    }

    protected BlockPos bisect(Chunk chunk, BlockPos min, BlockPos max) {
        while (min.getY() < max.getY() - 1) {
            BlockPos middle = middleY(min, max);
            if (!chunk.getBlockState(middle).isSideSolid(chunk.getWorld(), middle, EnumFacing.UP)) {
                // middle is empty, so continue searching between bottom and middle
                max = middle;
            } else {
                // middle is filled, so search between that and max
                min = middle;
            }
        }
        return max; // return the empty one
    }

    private BlockPos middleY(BlockPos min, BlockPos max) {
        return new BlockPos(min.getX(), (min.getY() + max.getY()) >> 1, min.getZ());
    }

    private BlockPos findMinPos(Chunk chunk, BlockPos pos) {
        // go down twice as much each time until we hit filled space
        int dy = 16;
        BlockPos p;
        while ((p = findNonEmpty(chunk, pos.down(dy))) == null) {
            dy *= 2;
            if (dy > Integer.MAX_VALUE >> 2) {
                CubicChunks.LOGGER.error("Error finding spawn point: can't find solid start height");
                return pos;
            }
        }
        return p;
    }

    private BlockPos findMaxPos(Chunk chunk, BlockPos pos) {
        // go up twice as much each time until we hit filled space
        int dy = 16;
        BlockPos p;
        while ((p = findEmpty(chunk, pos.up(dy))) == null) {
            dy *= 2;
            if (dy > Integer.MAX_VALUE >> 2) {
                CubicChunks.LOGGER.error("Error finding spawn point: can't find non-solid end height");
                return pos;
            }
        }
        return p;
    }

    @Nullable
    private BlockPos findNonEmpty(Chunk chunk, BlockPos pos) {
        pos = pos.down(MIN_FREE_SPACE_SPAWN);
        for (int i = 0; i < MIN_FREE_SPACE_SPAWN * 2; i++, pos = pos.up()) {
            if (chunk.getBlockState(pos).isSideSolid(chunk.getWorld(), pos, EnumFacing.UP)) {
                return pos;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findEmpty(Chunk chunk, BlockPos pos) {
        pos = pos.down(MIN_FREE_SPACE_SPAWN);
        for (int i = 0; i < MIN_FREE_SPACE_SPAWN * 2; i++, pos = pos.up()) {
            if (!chunk.getBlockState(pos).isSideSolid(chunk.getWorld(), pos, EnumFacing.UP)) {
                return pos;
            }
        }
        return null;
    }
}
