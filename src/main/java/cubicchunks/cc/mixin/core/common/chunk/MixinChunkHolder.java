package cubicchunks.cc.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.ICubeHolderListener;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements ICubeHolder {

    @Shadow public abstract ChunkPos getPosition();

    @Shadow public static ChunkStatus getChunkStatusFromLevel(int level) {
        throw new Error("Mixin failed to apply correctly!");
    }

    @Shadow private int prevChunkLevel;
    @Shadow private int chunkLevel;

    @Shadow public static ChunkHolder.LocationType getLocationTypeFromLevel(int level) {
        throw new Error("Mixin failed to apply correctly!");
    }

    @Shadow private boolean accessible;
    @Shadow private volatile CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> borderFuture;

    @Shadow protected abstract void chain(CompletableFuture<? extends Either<? extends IChunk, ChunkHolder.IChunkLoadingError>> eitherChunk);

    @Shadow @Final private static CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> UNLOADED_CHUNK_FUTURE;
    @Shadow private volatile CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> tickingFuture;
    @Shadow @Final public static Either<Chunk, ChunkHolder.IChunkLoadingError> UNLOADED_CHUNK;
    @Shadow private volatile CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> entityTickingFuture;
    //@Shadow @Final private ChunkPos pos;

    @Shadow @Final private static List<ChunkStatus> CHUNK_STATUS_LIST;
    @Shadow @Final private ChunkHolder.IListener field_219327_v;

    @Shadow public abstract int func_219281_j();

    @Shadow protected abstract void func_219275_d(int p_219275_1_);

    private SectionPos sectionPos;
    private final AtomicReferenceArray<CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>>> chunkFutureByChunkStatus = new AtomicReferenceArray<>(CHUNK_STATUS_LIST.size());


    @Override
    public void setYPos(int yPos) { //Whenever ChunkHolder is instantiated this should be called to finish the construction of the object
        this.sectionPos = SectionPos.of(getPosition().x, yPos, getPosition().z);
    }

    @Override
    public int getYPos()
    {
        return this.sectionPos.getY();
    }

    @Override
    public SectionPos getSectionPos() {
        return sectionPos;
    }

    @Inject(method = "processUpdates", at = @At("HEAD"), cancellable = true)
    void processUpdates(ChunkManager chunkManagerIn, CallbackInfo ci)
    {
        /**
        If sectionPos == null, this is a ChunkManager
        else, this is a CubeManager.
        This is being implemented as a mixin, instead of having a specific CubeManager class.
        this.sectionPos is essentially being used as a flag for changing behaviour.
         */
        if(this.sectionPos == null) {
            //This is a ChunkHolder.
            return;
        }
        //This is a CubeHolder.
        ci.cancel();

        ChunkStatus chunkstatus = getChunkStatusFromLevel(this.prevChunkLevel);
        ChunkStatus chunkstatus1 = getChunkStatusFromLevel(this.chunkLevel);
        boolean flag = this.prevChunkLevel <= ChunkManager.MAX_LOADED_LEVEL;
        boolean flag1 = this.chunkLevel <= ChunkManager.MAX_LOADED_LEVEL;
        ChunkHolder.LocationType chunkholder$locationtype = getLocationTypeFromLevel(this.prevChunkLevel);
        ChunkHolder.LocationType chunkholder$locationtype1 = getLocationTypeFromLevel(this.chunkLevel);
        if (flag) {
            Either<ChunkSection, ChunkHolder.IChunkLoadingError> either = Either.right(new ChunkHolder.IChunkLoadingError() {
                public String toString() {
                    return "Unloaded ticket level " + sectionPos.toString();
                }
            });

            for(int i = flag1 ? chunkstatus1.ordinal() + 1 : 0; i <= chunkstatus.ordinal(); ++i) {
                CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture = this.chunkFutureByChunkStatus.get(i);
                if (completablefuture != null) {
                    completablefuture.complete(either);
                } else {
                    this.chunkFutureByChunkStatus.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean flag5 = chunkholder$locationtype.isAtLeast(ChunkHolder.LocationType.BORDER);
        boolean flag6 = chunkholder$locationtype1.isAtLeast(ChunkHolder.LocationType.BORDER);
        this.accessible |= flag6;
        if (!flag5 && flag6) {
            this.borderFuture = chunkManagerIn.func_222961_b((ChunkHolder)(Object)this);
            this.chain(this.borderFuture);
        }

        if (flag5 && !flag6) {
            CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> completablefuture1 = this.borderFuture;
            this.borderFuture = UNLOADED_CHUNK_FUTURE;
            this.chain(completablefuture1.thenApply((p_222982_1_) -> {
                return p_222982_1_.ifLeft(chunkManagerIn::func_222973_a);
            }));
        }

        boolean flag7 = chunkholder$locationtype.isAtLeast(ChunkHolder.LocationType.TICKING);
        boolean flag2 = chunkholder$locationtype1.isAtLeast(ChunkHolder.LocationType.TICKING);
        if (!flag7 && flag2) {
            this.tickingFuture = chunkManagerIn.func_219179_a((ChunkHolder)(Object)this);
            this.chain(this.tickingFuture);
        }

        if (flag7 && !flag2) {
            this.tickingFuture.complete(UNLOADED_CHUNK);
            this.tickingFuture = UNLOADED_CHUNK_FUTURE;
        }

        boolean flag3 = chunkholder$locationtype.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        boolean flag4 = chunkholder$locationtype1.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        if (!flag3 && flag4) {
            if (this.entityTickingFuture != UNLOADED_CHUNK_FUTURE) {
                throw (IllegalStateException) Util.pauseDevMode(new IllegalStateException());
            }

            this.entityTickingFuture = chunkManagerIn.func_219188_b(this.sectionPos.asChunkPos());
            this.chain(this.entityTickingFuture);
        }

        if (flag3 && !flag4) {
            this.entityTickingFuture.complete(UNLOADED_CHUNK);
            this.entityTickingFuture = UNLOADED_CHUNK_FUTURE;
        }

        ((ICubeHolderListener)this.field_219327_v).onUpdateSectionLevel(this.sectionPos, this::func_219281_j, this.chunkLevel, this::func_219275_d);
        this.prevChunkLevel = this.chunkLevel;
    }
}
