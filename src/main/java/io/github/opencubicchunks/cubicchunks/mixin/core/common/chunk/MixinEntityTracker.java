package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Set;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkManagerAccess;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.TrackedEntity.class)
public abstract class MixinEntityTracker {

    // Mixin AP doesn't see the field, we need to provide intermediary name explicitly
    @SuppressWarnings("target") @Shadow(aliases = "field_18245", remap = false) ChunkMap this$0;

    @Shadow @Final private Entity entity;

    @Shadow @Final private ServerEntity serverEntity;

    @Shadow @Final private Set<ServerPlayerConnection> seenBy;

    @Shadow protected abstract int getEffectiveRange();

    /**
     * @author NotStirred
     * @reason Entire method needs to be rewritten
     */
    @Overwrite
    public void updatePlayer(ServerPlayer player) {
        if (player != this.entity) {
            Vec3 vec3d = player.position().subtract(this.serverEntity.sentPos());
            //This function is fine
            int i = Math.min(this.getEffectiveRange(), (((ChunkManagerAccess) this$0).getViewDistance() - 1) * 16);
            boolean flag = vec3d.x >= (double) (-i) && vec3d.x <= (double) i &&
                vec3d.y >= (double) (-i) && vec3d.y <= (double) i && //Added y comparisons
                vec3d.z >= (double) (-i) && vec3d.z <= (double) i &&
                this.entity.broadcastToPlayer(player);
            if (flag) {
                boolean spawn = this.entity.forcedLoading;
                if (!spawn) {
                    CubePos cubePos = CubePos.from(this.entity);
                    ChunkHolder chunkholder = ((IChunkManager) this$0).getImmutableCubeHolder(cubePos.asLong());
                    if (chunkholder != null && ((ICubeHolder) chunkholder).getCubeIfComplete() != null) {
                        spawn = IChunkManager.getCubeChebyshevDistance(cubePos, player, false) <= ((ChunkManagerAccess) this$0).getViewDistance();
                    }
                }

                if (spawn && this.seenBy.add(player.connection)) {
                    this.serverEntity.addPairing(player);
                }
            } else if (this.seenBy.remove(player.connection)) {
                this.serverEntity.removePairing(player);
            }

        }
    }
}