package cubicchunks.cc.mixin.core.client;

import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderCache.class)
public class MixinChunkRenderCache {
    @Final
    @Shadow
    protected World world;

    /*
     * Original Method
     */
    /*
   @Nullable
   public static ChunkRenderCache generateCache(World worldIn, BlockPos from, BlockPos to, int padding) {
      int i = from.getX() - padding >> 4;
      int j = from.getZ() - padding >> 4;
      int k = to.getX() + padding >> 4;
      int l = to.getZ() + padding >> 4;
      Chunk[][] achunk = new Chunk[k - i + 1][l - j + 1];

      for(int i1 = i; i1 <= k; ++i1) {
         for(int j1 = j; j1 <= l; ++j1) {
            achunk[i1 - i][j1 - j] = worldIn.getChunk(i1, j1);
         }
      }

      boolean flag = true;

      for(int l1 = from.getX() >> 4; l1 <= to.getX() >> 4; ++l1) {
         for(int k1 = from.getZ() >> 4; k1 <= to.getZ() >> 4; ++k1) {
            Chunk chunk = achunk[l1 - i][k1 - j];
            if (!chunk.isEmptyBetween(from.getY(), to.getY())) {
               flag = false;
            }
         }
      }

      if (flag) {
         return null;
      } else {
         int i2 = 1;
         BlockPos blockpos = from.add(-1, -1, -1);
         BlockPos blockpos1 = to.add(1, 1, 1);
         return new ChunkRenderCache(worldIn, i, j, achunk, blockpos, blockpos1);
      }
   }
   */

    //TODO: Mixin generate cache for rendering above 256.

//    @Inject(method = "generateCache", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    private static void generateCubicCache(World worldIn, BlockPos from, BlockPos to, int padding, CallbackInfoReturnable<ChunkRenderCache> cir) {

//    }

    //TODO: Mixin GetBlockState
//    @Inject(method = "getBlockState", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    private static void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
//
//    }
}
