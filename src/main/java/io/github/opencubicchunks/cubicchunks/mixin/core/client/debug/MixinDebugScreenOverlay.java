package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class MixinDebugScreenOverlay {
	@Shadow abstract LevelChunk getServerChunk();

	@Inject(method = "getGameInformation",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I", ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void onGetGameInformation(CallbackInfoReturnable<List<String>> cir, String string2, BlockPos blockPos, Entity entity, Direction direction, String string7, Level level, LongSet longSet, List list, LevelChunk levelChunk, int i) {
		LevelChunk worldChunk2 = this.getServerChunk();
		if (worldChunk2 != null) {
			LevelLightEngine lightingProvider = level.getChunkSource().getLightEngine();
			list.add("Server Light: (" + lightingProvider.getLayerListener(LightLayer.SKY).getLightValue(blockPos) + " sky, "
					+ lightingProvider.getLayerListener(LightLayer.BLOCK).getLightValue(blockPos) + " block)");
		} else {
			list.add("Server Light: (?? sky, ?? block)");
		}
	}
}
