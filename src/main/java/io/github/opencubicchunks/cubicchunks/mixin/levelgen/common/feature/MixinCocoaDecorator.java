package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;


import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CocoaDecorator.class)
public class MixinCocoaDecorator {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void exitIfListIsEmpty(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions,
                                   CallbackInfo ci) {
        if (leavesPositions.isEmpty() || logPositions.isEmpty()) {
            ci.cancel();
        }
    }
}
