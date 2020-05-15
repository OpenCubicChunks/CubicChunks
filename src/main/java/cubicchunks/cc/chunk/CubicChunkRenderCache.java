package cubicchunks.cc.chunk;

import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;


public class CubicChunkRenderCache extends ChunkRenderCache {
    /*
    TODO: Create a 1.15.2 Cubic Render Cache.
     */

    public CubicChunkRenderCache(World worldIn, int chunkStartXIn, int chunkStartYin, int chunkStartZIn, Chunk[][] chunksIn, BlockPos startPos, BlockPos endPos) {
        super(worldIn, chunkStartXIn, chunkStartZIn, chunksIn, startPos, endPos);
    }

   /* 1.15 Method:
    public ChunkRenderCache(World worldIn, int chunkStartXIn, int chunkStartZIn, Chunk[][] chunksIn, BlockPos startPos, BlockPos endPos) {
      this.world = worldIn;
      this.chunkStartX = chunkStartXIn;
      this.chunkStartZ = chunkStartZIn;
      this.chunks = chunksIn;
      this.cacheStartPos = startPos;
      this.cacheSizeX = endPos.getX() - startPos.getX() + 1;
      this.cacheSizeY = endPos.getY() - startPos.getY() + 1;
      this.cacheSizeZ = endPos.getZ() - startPos.getZ() + 1;
      this.blockStates = new BlockState[this.cacheSizeX * this.cacheSizeY * this.cacheSizeZ];
      this.fluidStates = new IFluidState[this.cacheSizeX * this.cacheSizeY * this.cacheSizeZ];

      for(BlockPos blockpos : BlockPos.getAllInBoxMutable(startPos, endPos)) {
         int i = (blockpos.getX() >> 4) - chunkStartXIn;
         int j = (blockpos.getZ() >> 4) - chunkStartZIn;
         Chunk chunk = chunksIn[i][j];
         int k = this.getIndex(blockpos);
         this.blockStates[k] = chunk.getBlockState(blockpos);
         this.fluidStates[k] = chunk.getFluidState(blockpos);
      }

   }
     */

    /* Our 1.12 Method:
        public RenderCubeCache(World world, BlockPos from, BlockPos to, int subtract) {
        super(world, from, to, subtract);
        this.world = world;
        this.cubeY = Coords.blockToCube(from.getY() - subtract);
        int cubeXEnd = Coords.blockToCube(to.getX() + subtract);
        int cubeYEnd = Coords.blockToCube(to.getY() + subtract);
        int cubeZEnd = Coords.blockToCube(to.getZ() + subtract);

        cubeArrays = new ExtendedBlockStorage[cubeXEnd - this.chunkX + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.chunkZ + 1];
        // because java is stupid and won't allow generic array creation, and temporary local variable because it won't allow annotation on assignment
        @SuppressWarnings("unchecked")
        Map<BlockPos, TileEntity>[][][] tileEntities = new Map[cubeXEnd - this.chunkX + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.chunkZ + 1];
        this.tileEntities = tileEntities;

        ExtendedBlockStorage nullStorage = new ExtendedBlockStorage(0, true);

        for (int currentCubeX = chunkX; currentCubeX <= cubeXEnd; currentCubeX++) {
            for (int currentCubeY = cubeY; currentCubeY <= cubeYEnd; currentCubeY++) {
                for (int currentCubeZ = chunkZ; currentCubeZ <= cubeZEnd; currentCubeZ++) {
                    ExtendedBlockStorage ebs;
                    Map<BlockPos, TileEntity> teMap;

                    Cube cube = ((ICubicWorldInternal) world).getCubeFromCubeCoords(currentCubeX, currentCubeY, currentCubeZ);
                    ebs = cube.getStorage();

                    teMap = cube.getTileEntityMap();

                    if (ebs == null) {
                        ebs = nullStorage;
                    }
                    cubeArrays[currentCubeX - chunkX][currentCubeY - cubeY][currentCubeZ - chunkZ] = ebs;
                    tileEntities[currentCubeX - chunkX][currentCubeY - cubeY][currentCubeZ - chunkZ] = teMap;
                }
            }
        }
    }
     */
}
