package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkManagerAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.sectionToCube;

@Mixin(ChunkMap.TrackedEntity.class)
public abstract class MixinEntityTracker {

    @SuppressWarnings({"target"}) @Shadow(aliases = "this$0", remap = false) ChunkMap syntheticThis;

    @Shadow @Final private Entity entity;

    @Shadow @Final private ServerEntity serverEntity;

    @Shadow @Final private Set<ServerPlayer> seenBy;

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
            int i = Math.min(this.getEffectiveRange(), (((ChunkManagerAccess) syntheticThis).getViewDistance() - 1) * 16);
            boolean flag = vec3d.x >= (double)(-i) && vec3d.x <= (double)i &&
                    vec3d.y >= (double)(-i) && vec3d.y <= (double)i && //Added y comparisons
                    vec3d.z >= (double)(-i) && vec3d.z <= (double)i &&
                    this.entity.broadcastToPlayer(player);
            if (flag) {
                boolean spawn = this.entity.forcedLoading;
                if (!spawn) {
                    CubePos cubePos = CubePos.of(sectionToCube(this.entity.xChunk), sectionToCube(this.entity.yChunk),
                            sectionToCube(this.entity.zChunk));
                    ChunkHolder chunkholder = ((IChunkManager) syntheticThis).getImmutableCubeHolder(cubePos.asLong());
                    if (chunkholder != null && ((ICubeHolder) chunkholder).getCubeIfComplete() != null) {
                        spawn = IChunkManager.getCubeChebyshevDistance(cubePos, player, false) <= ((ChunkManagerAccess) syntheticThis).getViewDistance();
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