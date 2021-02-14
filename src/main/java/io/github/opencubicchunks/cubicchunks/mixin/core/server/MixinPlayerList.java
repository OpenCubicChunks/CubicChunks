package io.github.opencubicchunks.cubicchunks.mixin.core.server;

import io.github.opencubicchunks.cubicchunks.chunk.IVerticalView;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList implements IVerticalView {

    @Shadow private int viewDistance;

    @Shadow public abstract void broadcastAll(Packet<?> packet);

    @Shadow @Final private MinecraftServer server;
    private int verticalViewDistance = 0;

    //Should never be called by Vanilla
    @Inject(method = "setViewDistance", at = @At("HEAD"))
    private void doVerticalChangesAswell(int viewDistance, CallbackInfo ci) {
        ci.cancel();

        this.viewDistance = viewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance));

        for (ServerLevel serverLevel : this.server.getAllLevels()) {
            if (serverLevel != null) {
                if (((CubicLevelHeightAccessor) serverLevel).isCubic()) {
                    ((IVerticalView) serverLevel.getChunkSource()).setCubeViewDistance(this.viewDistance, this.verticalViewDistance);
                }
                serverLevel.getChunkSource().setViewDistance(viewDistance);
            }
        }

    }

    @Override public void setCubeViewDistance(int horizontalDistance, int verticalDistance) {
        this.viewDistance = horizontalDistance;
        this.verticalViewDistance = verticalDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance)); //TODO: Create a ClientboundSetCubeCacheRadiusPacket

        for (ServerLevel serverLevel : this.server.getAllLevels()) {
            if (serverLevel != null) {
                if (((CubicLevelHeightAccessor) serverLevel).isCubic()) {
                    ((IVerticalView) serverLevel.getChunkSource()).setCubeViewDistance(this.viewDistance, this.verticalViewDistance);
                }
                serverLevel.getChunkSource().setViewDistance(viewDistance);
            }
        }
    }

    @Override public int getVerticalViewDistance() {
        return this.verticalViewDistance;
    }
}
