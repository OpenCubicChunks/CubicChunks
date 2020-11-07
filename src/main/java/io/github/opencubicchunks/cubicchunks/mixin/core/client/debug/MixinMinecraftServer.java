package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import io.github.opencubicchunks.cubicchunks.debug.DebugVisualization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Map;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Shadow @Final protected WorldData worldData;

    @Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;

    @Inject(method = "createLevels", at = @At("RETURN"))
    private void onLoadWorlds(ChunkProgressListener chunkProgressListener, CallbackInfo ci) {
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> lvl : this.worldData.worldGenSettings().dimensions().entrySet()) {
            DebugVisualization.onWorldLoad(levels.get(lvl.getKey()));
        }
    }

    @Redirect(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;close()V"))
    private void onUnloadWorld(ServerLevel serverLevel) throws IOException {
        DebugVisualization.onWorldUnload(serverLevel);
        serverLevel.close();
    }
}
