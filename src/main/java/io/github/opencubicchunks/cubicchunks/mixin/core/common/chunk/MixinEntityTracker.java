package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.sectionToCube;

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

@Mixin(ChunkManager.EntityTracker.class)
public abstract class MixinEntityTracker {

    @Shadow ChunkManager this$0;

    @Shadow @Final private Entity entity;

    @Shadow @Final private TrackedEntity entry;

    @Shadow @Final private Set<ServerPlayerEntity> trackingPlayers;

    @Shadow protected abstract int func_229843_b_();

    /**
     * @author NotStirred
     * @reason Entire method needs to be rewritten
     */
    @Overwrite
    public void updateTrackingState(ServerPlayerEntity player) {
        if (player != this.entity) {
            Vector3d vec3d = player.getPositionVec().subtract(this.entry.func_219456_b());
                            //This function is fine
            int i = Math.min(this.func_229843_b_(), (((ChunkManagerAccess)this$0).getViewDistance() - 1) * 16);
            boolean flag = vec3d.x >= (double)(-i) && vec3d.x <= (double)i &&
                    vec3d.y >= (double)(-i) && vec3d.y <= (double)i && //Added y comparisons
                    vec3d.z >= (double)(-i) && vec3d.z <= (double)i &&
                    this.entity.isSpectatedByPlayer(player);
            if (flag) {
                boolean spawn = this.entity.forceSpawn;
                if (!spawn) {
                    CubePos cubePos = CubePos.of(sectionToCube(this.entity.chunkCoordX), sectionToCube(this.entity.chunkCoordY),
                            sectionToCube(this.entity.chunkCoordZ));
                    ChunkHolder chunkholder = ((IChunkManager)this$0).getImmutableCubeHolder(cubePos.asLong());
                    if (chunkholder != null && ((ICubeHolder) chunkholder).getCubeIfComplete() != null) {
                        spawn = IChunkManager.getCubeChebyshevDistance(cubePos, player, false) <= ((ChunkManagerAccess)this$0).getViewDistance();
                    }
                }

                if (spawn && this.trackingPlayers.add(player)) {
                    this.entry.track(player);
                }
            } else if (this.trackingPlayers.remove(player)) {
                this.entry.untrack(player);
            }

        }
    }
}
