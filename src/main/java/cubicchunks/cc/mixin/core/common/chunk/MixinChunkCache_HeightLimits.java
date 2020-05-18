package cubicchunks.cc.mixin.core.common.chunk;

import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkRenderCache.class)
public class MixinChunkCache_HeightLimits {
    //TODO: Mixin generate cache for rendering above 256. This should let us get rendered blocks above 256.

//    @Inject(method = "generateCache", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    private static void generateCubicCache(World worldIn, BlockPos from, BlockPos to, int padding, CallbackInfoReturnable<ChunkRenderCache> cir) {

//    }

    //TODO: Mixin GetBlockState for 256 height. Method Changed between 1.12 and 1.15.
//    @Inject(method = "getBlockState", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    private static void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
//
//    }
}
