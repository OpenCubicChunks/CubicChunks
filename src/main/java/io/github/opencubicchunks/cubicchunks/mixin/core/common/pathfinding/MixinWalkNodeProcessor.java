package io.github.opencubicchunks.cubicchunks.mixin.core.common.pathfinding;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Region;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor extends NodeProcessor {

    @ModifyConstant(method = "getStart", constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO))
    private int getStartWorldHeight(int _0) { return -CubicChunks.worldMAXHeight; }

    // Stolen from Barteks2x but fixed for 1.15
    @Redirect(method = "getStart", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/Region;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState getLoadedBlockState_getStart(Region region, BlockPos pos) {
        if (!entity.world.isBlockLoaded(pos)) {
            return Blocks.BEDROCK.getDefaultState();
        }
        return region.getBlockState(pos);
    }

    @ModifyConstant(method = "getSafePoint", constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO, ordinal = 1))
    private int getSafeWorldHeight(int _0) { return -CubicChunks.worldMAXHeight; }

    @ModifyConstant(method = "getSafePoint", constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, ordinal = 0))
    private int getSafePos(int _0) { return -CubicChunks.worldMAXHeight; }

    @ModifyConstant(method = "func_227480_b_", constant = @Constant(intValue = 1, ordinal = 0))
    private static int getPathWorldHeight(int _1) { return -CubicChunks.worldMAXHeight; }

}
