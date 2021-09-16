package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkBorderRenderer.class)
public class MixinChunkBorderRenderer {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMinBuildHeight()I"))
    public int minHeight(ClientLevel clientLevel) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null) {
            return Coords.sectionToMinBlock(Coords.blockToCube(Coords.getCubeYForEntity(cameraEntity))) - 256;
        } else {
            return 0;
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMaxBuildHeight()I"))
    public int maxHeight(ClientLevel clientLevel) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null) {
            return Coords.sectionToMinBlock(Coords.blockToCube(Coords.getCubeYForEntity(cameraEntity))) + 256;
        } else {
            return 256;
        }
    }
}
