package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.storage.CubicEntityStorage;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.chunk.entity.ChunkEntityStateEventHandler;
import io.github.opencubicchunks.cubicchunks.chunk.entity.ChunkEntityStateEventSource;
import io.github.opencubicchunks.cubicchunks.chunk.entity.IsCubicEntityContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager<T extends EntityAccess> implements IsCubicEntityContext, ChunkEntityStateEventSource {


    @Shadow @Final private Long2ObjectMap<Object> chunkLoadStatuses;
    @Shadow @Final private EntityPersistentStorage<T> permanentStorage;
    @Shadow @Final private Queue<ChunkEntities<T>> loadingInbox;
    @Shadow @Final private EntitySectionStorage<T> sectionStorage;
    @Shadow @Final private Long2ObjectMap<Visibility> chunkVisibility;

    private boolean isCubic;
    private List<ChunkEntityStateEventHandler> eventHandler = new ArrayList<>();

    @Shadow public abstract void updateChunkStatus(ChunkPos chunkPos, Visibility visibility);

    @Override public boolean isCubic() {
        return this.isCubic;
    }

    @Override public void registerChunkEntityStateEventHandler(ChunkEntityStateEventHandler handler) {
        this.eventHandler.add(handler);
    }

    @Override public void setIsCubic(boolean isCubic) {
        this.isCubic = isCubic;
        ((IsCubicEntityContext) this.sectionStorage).setIsCubic(isCubic);
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

    @Inject(method = "requestChunkLoad", at = @At("HEAD"), cancellable = true)
    private void requestCubeLoad(long pos, CallbackInfo ci) {
        if (!this.isCubic) {
            return;
        }
        ci.cancel();

        //Used to avoid access Widening PersistentEntitySectionManager.ChunkLoadStatus.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Enum<?> pending = Enum.valueOf((Class) chunkLoadStatuses.defaultReturnValue().getClass(), "PENDING");
        this.chunkLoadStatuses.put(pos, pending);
        CubePos cubePos = new CubePos(pos);

        ((CubicEntityStorage) this.permanentStorage).loadCubeEntities(cubePos).thenAccept(((Queue) this.loadingInbox)::add).exceptionally((throwable) -> {
            CubicChunks.LOGGER.error("Failed to read cube {}", cubePos, throwable);
            return null;
        });

    }

    @Redirect(method = "storeChunkSections", at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private ChunkPos useImposterChunkPos(long pos) {
        if (isCubic) {
            return new ImposterChunkPos(CubePos.from(pos));
        } else {
            return new ChunkPos(pos);
        }
    }

    @Inject(method = "isPositionTicking(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
    private void returnFalseIfCubic(ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir) {
        if (chunkPos instanceof ImposterChunkPos) {
            return;
        }

        if (isCubic) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPositionTicking(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void useCubePos(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (!isCubic) {
            return;
        }

        cir.setReturnValue(this.chunkVisibility.get(CubePos.asLong(blockPos)).isTicking());
    }

    // "event handlers" for CubicFastServerTickList

    @Inject(method = "processPendingLoads",
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void onLoad(CallbackInfo ci, ChunkEntities<T> entities) {
        if (!isCubic) {
            return;
        }
        if (!eventHandler.isEmpty()) {
            var pos = ((ImposterChunkPos) entities.getPos()).toCubePos();
            eventHandler.forEach(x -> x.onCubeEntitiesLoad(pos));
        }
    }

    @Inject(method = "processChunkUnload",
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;remove(J)Ljava/lang/Object;"))
    private void onUnload(long posLong, CallbackInfoReturnable<Boolean> cir) {
        if (!isCubic) {
            return;
        }
        // posLong is cubePos if cubic, chunk pos if not cubic
        if (eventHandler != null) {
            var pos = CubePos.from(posLong);
            eventHandler.forEach(x -> x.onCubeEntitiesUnload(pos));
        }
    }
}