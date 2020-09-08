package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkManagerAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.sectionToCube;

@Mixin(ChunkManager.EntityTracker.class)
public abstract class MixinEntityTracker {

    @SuppressWarnings("ShadowTarget") @Shadow ChunkManager this$0;

    @Shadow @Final private Entity entity;

    @Shadow @Final private TrackedEntity serverEntity;

    @Shadow @Final private Set<ServerPlayerEntity> seenBy;

    @Shadow protected abstract int getEffectiveRange();

    /**
     * @author NotStirred
     * @reason Entire method needs to be rewritten
     */
    @Overwrite
    public void updatePlayer(ServerPlayerEntity player) {
        if (player != this.entity) {
            Vector3d vec3d = player.position().subtract(this.serverEntity.sentPos());
                            //This function is fine
            int i = Math.min(this.getEffectiveRange(), (((ChunkManagerAccess)this$0).getViewDistance() - 1) * 16);
            boolean flag = vec3d.x >= (double)(-i) && vec3d.x <= (double)i &&
                    vec3d.y >= (double)(-i) && vec3d.y <= (double)i && //Added y comparisons
                    vec3d.z >= (double)(-i) && vec3d.z <= (double)i &&
                    this.entity.broadcastToPlayer(player);
            if (flag) {
                boolean spawn = this.entity.forcedLoading;
                if (!spawn) {
                    CubePos cubePos = CubePos.of(sectionToCube(this.entity.xChunk), sectionToCube(this.entity.yChunk),
                            sectionToCube(this.entity.zChunk));
                    ChunkHolder chunkholder = ((IChunkManager)this$0).getImmutableCubeHolder(cubePos.asLong());
                    if (chunkholder != null && ((ICubeHolder) chunkholder).getCubeIfComplete() != null) {
                        spawn = IChunkManager.getCubeChebyshevDistance(cubePos, player, false) <= ((ChunkManagerAccess)this$0).getViewDistance();
                    }
                }

                if (spawn && this.seenBy.add(player)) {
                    this.serverEntity.addPairing(player);
                }
            } else if (this.seenBy.remove(player)) {
                this.serverEntity.removePairing(player);
            }

        }
    }
}