package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.optifine;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.optifine.render.ChunkVisibility")
public class MixinChunkVisibility {
    @Dynamic @Inject(method = "getMaxChunkY", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getMaxChunkYCC(World world, Entity viewEntity, int renderDistanceChunks, CallbackInfoReturnable<Integer> cbi) {
        // if (!((ICubicWorld) world).isCubicWorld()) {
        //     return;
        // }
        cbi.setReturnValue(Integer.MAX_VALUE - 1);
    }
}