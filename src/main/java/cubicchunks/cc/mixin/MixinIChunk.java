package cubicchunks.cc.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Region;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Region.class)
public class MixinIChunk {
    @Inject(at = @At("HEAD"), method = "getBlockState", cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (World.isOutsideBuildHeight(pos)) {
            cir.setReturnValue(Blocks.GLASS.getDefaultState());
        }
    }
}
