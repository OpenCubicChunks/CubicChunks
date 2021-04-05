package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.world.entity.IsCubicContextPersistentEntitySectionManager;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager<T extends EntityAccess> implements IsCubicContextPersistentEntitySectionManager {

    @Shadow public abstract void updateChunkStatus(ChunkPos chunkPos, Visibility visibility);

    private boolean isCubic;

    @Override public boolean isCubic() {
        return this.isCubic;
    }

    @Override public void setIsCubic(boolean isCubic) {
        this.isCubic = isCubic;
    }


    @Inject(method = "updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/server/level/ChunkHolder$FullChunkStatus;)V", at = @At("HEAD"), cancellable = true)
    private void updateCubeStatus(ChunkPos pos, ChunkHolder.FullChunkStatus fullChunkStatus, CallbackInfo ci) {
        if (isCubic) {
            ci.cancel();
            if (pos instanceof ImposterChunkPos) {
                Visibility visibility = Visibility.fromFullChunkStatus(fullChunkStatus);
                this.updateChunkStatus(pos, visibility);
            }
        }
    }

    @Redirect(method = "requestChunkLoad", at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private ChunkPos createImposterIfCubic(long pos) {
        if (isCubic) {
            return new ImposterChunkPos(CubePos.from(pos));
        } else {
            return new ChunkPos(pos);
        }
    }

    @Redirect(method = "storeChunkSections", at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private ChunkPos useImposterChunkPos(long pos) {
        if (isCubic) {
            return new ImposterChunkPos(CubePos.from(pos));
        } else {
            return new ChunkPos(pos);
        }
    }
}