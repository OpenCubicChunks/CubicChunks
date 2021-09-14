package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightHeightmapGetter;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.LightSurfaceTrackerWrapper;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DebugScreenOverlay.class)
public abstract class MixinDebugScreenOverlay {

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract LevelChunk getServerChunk();

    @SuppressWarnings("rawtypes")
    @Inject(method = "getGameInformation",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 6),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onAddChunkInfo(CallbackInfoReturnable<List> cir, /*IntegratedServer integratedserver, Connection networkmanager, float f, float f1,*/
                                String s, BlockPos blockpos, Entity entity, Direction direction, String s1, /*ChunkPos chunkpos,*/ Level world, LongSet longset,
                                List debugScreenList/*, String s2*/) {
        //noinspection unchecked
        debugScreenList.add(String.format("Cube:  %d %d %d in %d %d %d",
            blockpos.getX() & (CubeAccess.DIAMETER_IN_BLOCKS - 1), blockpos.getY() & (CubeAccess.DIAMETER_IN_BLOCKS - 1), blockpos.getZ() & (CubeAccess.DIAMETER_IN_BLOCKS - 1),
            Coords.blockToCube(blockpos.getX()), Coords.blockToCube(blockpos.getY()), Coords.blockToCube(blockpos.getZ()))
        );
    }

    @Inject(method = "getGameInformation",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I",
            ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void onGetGameInformation(CallbackInfoReturnable<List<String>> cir, String string2, BlockPos pos, Entity entity, Direction direction,
                                      String string7, Level level, LongSet longSet, List<String> list, LevelChunk clientChunk, int i) {
        LevelChunk serverChunk = this.getServerChunk();
        if (((CubicLevelHeightAccessor) level).isCubic()) {
            if (this.minecraft.getSingleplayerServer() != null) {
                String serverHeight = "???";
                if (serverChunk != null) {
                    LightSurfaceTrackerWrapper heightmap = ((LightHeightmapGetter) serverChunk).getServerLightHeightmap();
                    int height = heightmap.getFirstAvailable(pos.getX() & 0xF, pos.getZ() & 0xF);
                    serverHeight = "" + height;
                }
                list.add("Server light heightmap height: " + serverHeight);
            }
            int clientHeight = ((LightHeightmapGetter) clientChunk).getClientLightHeightmap().getFirstAvailable(pos.getX() & 0xF, pos.getZ() & 0xF);
            list.add("Client light heightmap height: " + clientHeight);
        }

        // No cubic check here because it's a vanilla feature that was removed anyway
        if (this.minecraft.getSingleplayerServer() != null) {
            if (serverChunk != null) {
                LevelLightEngine lightingProvider = level.getChunkSource().getLightEngine();
                list.add("Server Light: (" + lightingProvider.getLayerListener(LightLayer.SKY).getLightValue(pos) + " sky, "
                    + lightingProvider.getLayerListener(LightLayer.BLOCK).getLightValue(pos) + " block)");
            } else {
                list.add("Server Light: (?? sky, ?? block)");
            }
        }
    }
}
