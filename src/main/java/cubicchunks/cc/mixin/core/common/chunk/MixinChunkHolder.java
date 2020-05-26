package cubicchunks.cc.mixin.core.common.chunk;

import static cubicchunks.cc.chunk.util.Utils.unsafeCast;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ISection;
import cubicchunks.cc.chunk.ISectionHolderListener;
import cubicchunks.cc.chunk.ISectionHolder;
import cubicchunks.cc.network.PacketCubes;
import cubicchunks.cc.network.PacketDispatcher;
import cubicchunks.cc.network.PacketSectionBlockChanges;
import cubicchunks.cc.utils.AddressTools;
import cubicchunks.cc.utils.Coords;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements ISectionHolder {

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

    //@Shadow protected abstract void chain(CompletableFuture<? extends Either<? extends IChunk, ChunkHolder.IChunkLoadingError>> eitherChunk);

    //@Shadow @Final private static CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> UNLOADED_CHUNK_FUTURE;
    //@Shadow @Final public static Either<Chunk, ChunkHolder.IChunkLoadingError> UNLOADED_CHUNK;
    //@Shadow private volatile CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> entityTickingFuture;
    //@Shadow @Final private ChunkPos pos;

    @Shadow @Final private static List<ChunkStatus> CHUNK_STATUS_LIST;

    //This is either a ChunkTaskPriorityQueueSorter or a SectionTaskPriorityQueueSorter depending on if this is a chunkholder or sectionholder.
    @Shadow(aliases = "field_219327_v") @Final private ChunkHolder.IListener chunkHolderListener;

    @Shadow(aliases = "func_219281_j") public abstract int getCompletedLevel();
    @Shadow(aliases = "func_219275_d") protected abstract void setCompletedLevel(int p_219275_1_);

    private CompletableFuture<ISection> sectionFuture = CompletableFuture.completedFuture((ISection) null);

    private static final Either<ISection, ChunkHolder.IChunkLoadingError> MISSING_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    private static final CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> MISSING_CHUNK_FUTURE = CompletableFuture.completedFuture(MISSING_SECTION);
    private static final Either<ChunkSection, ChunkHolder.IChunkLoadingError> UNLOADED_SECTION =
            Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    private static final CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> UNLOADED_SECTION_FUTURE =
            CompletableFuture.completedFuture(UNLOADED_SECTION);

    private volatile CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> tickingSectionFuture = UNLOADED_SECTION_FUTURE;
    private volatile CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> entityTickingSectionFuture = UNLOADED_SECTION_FUTURE;

    private final AtomicReferenceArray<CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>>> sectionFutureBySectionStatus = new AtomicReferenceArray<>(CHUNK_STATUS_LIST.size());

    @Shadow private int skyLightChangeMask;
    @Shadow private int blockLightChangeMask;
    @Shadow private int boundaryMask;
    @Shadow private int blockChangeMask;

    @Shadow protected abstract void sendTileEntity(World worldIn, BlockPos posIn);

    @Shadow protected abstract void sendToTracking(IPacket<?> packetIn, boolean boundaryOnly);

    @Shadow @Final private WorldLightManager lightManager;
    @Shadow @Final private ChunkHolder.IPlayerProvider playerProvider;
    @Shadow @Final private ChunkPos pos;
    @Shadow private short[] changedBlockPositions;
    @Shadow private int changedBlocks;
    private SectionPos sectionPos;
    private final AtomicReferenceArray<CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>>> chunkFutureByChunkStatus =
            new AtomicReferenceArray<>(CHUNK_STATUS_LIST.size());

    // TODO: this is going to be one section per ChunkHolder for section holders
    private final Int2ObjectArrayMap<ShortArraySet> changedLocalBlocks = new Int2ObjectArrayMap<>();

    /**
     * A future that returns the ChunkSection if it is a border chunk, {@link
     * net.minecraft.world.server.ChunkHolder.IChunkLoadingError#UNLOADED} otherwise.
     */
    private volatile CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> borderSectionFuture = UNLOADED_SECTION_FUTURE;

    //BEGIN INJECTS:

    @Inject(method = "processUpdates", at = @At("HEAD"), cancellable = true)
    void processUpdates(ChunkManager chunkManagerIn, CallbackInfo ci) {
        /*
         If sectionPos == null, this is a ChunkManager
         else, this is a CubeManager.
         This is being implemented as a mixin, instead of having a specific CubeManager class.
         this.sectionPos is essentially being used as a flag for changing behaviour.
         */

        if(this.sectionPos == null)
        {
            return;
        }
        ci.cancel();

        ChunkStatus chunkstatus = getChunkStatusFromLevel(this.prevChunkLevel);
        ChunkStatus chunkstatus1 = getChunkStatusFromLevel(this.chunkLevel);
        boolean wasFullyLoaded = this.prevChunkLevel <= ChunkManager.MAX_LOADED_LEVEL;
        boolean isFullyLoaded = this.chunkLevel <= ChunkManager.MAX_LOADED_LEVEL;
        ChunkHolder.LocationType previousLocationType = getLocationTypeFromLevel(this.prevChunkLevel);
        ChunkHolder.LocationType currentLocationType = getLocationTypeFromLevel(this.chunkLevel);
        if (wasFullyLoaded) {
            @SuppressWarnings("MixinInnerClass")
            Either<ISection, ChunkHolder.IChunkLoadingError> either = Either.right(new ChunkHolder.IChunkLoadingError() {
                public String toString() {
                    return "Unloaded ticket level " + sectionPos.toString();
                }
            });
            for(int i = isFullyLoaded ? chunkstatus1.ordinal() + 1 : 0; i <= chunkstatus.ordinal(); ++i) {
                CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> completablefuture = this.sectionFutureBySectionStatus.get(i);
                if (completablefuture != null) {
                    completablefuture.complete(either);
                } else {
                    this.sectionFutureBySectionStatus.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean wasBorder = previousLocationType.isAtLeast(ChunkHolder.LocationType.BORDER);
        boolean isBorder = currentLocationType.isAtLeast(ChunkHolder.LocationType.BORDER);
        this.accessible |= isBorder;
        if (!wasBorder && isBorder) {
            this.borderSectionFuture = ((IChunkManager)chunkManagerIn).createSectionBorderFuture((ChunkHolder)(Object)this);
            this.chainSection((CompletableFuture)this.borderSectionFuture);
        }

        if (wasBorder && !isBorder) {
            CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture1 = this.borderSectionFuture;
            this.borderSectionFuture = UNLOADED_SECTION_FUTURE;
            this.chainSection(unsafeCast(completablefuture1.thenApply((chunkLoadingErrorEither) -> {
                return chunkLoadingErrorEither.ifLeft(((IChunkManager)chunkManagerIn)::saveSectionScheduleTicks);
            })));
        }

        boolean wasTicking = previousLocationType.isAtLeast(ChunkHolder.LocationType.TICKING);
        boolean isTicking = currentLocationType.isAtLeast(ChunkHolder.LocationType.TICKING);
        if (!wasTicking && isTicking) {
            this.tickingSectionFuture = ((IChunkManager)chunkManagerIn).createSectionTickingFuture((ChunkHolder)(Object)this);
            this.chainSection(unsafeCast(this.tickingSectionFuture));
        }

        if (wasTicking && !isTicking) {
            this.tickingSectionFuture.complete(UNLOADED_SECTION);
            this.tickingSectionFuture = UNLOADED_SECTION_FUTURE;
        }

        boolean wasEntityTicking = previousLocationType.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        boolean isEntityTicking = currentLocationType.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        if (!wasEntityTicking && isEntityTicking) {
            if (this.entityTickingSectionFuture != UNLOADED_SECTION_FUTURE) {
                throw (IllegalStateException) Util.pauseDevMode(new IllegalStateException());
            }

            this.entityTickingSectionFuture = ((IChunkManager)chunkManagerIn).createSectionEntityTickingFuture(this.sectionPos);
            this.chainSection(unsafeCast(this.entityTickingSectionFuture));
        }

        if (wasEntityTicking && !isEntityTicking) {
            this.entityTickingSectionFuture.complete(UNLOADED_SECTION);
            this.entityTickingSectionFuture = UNLOADED_SECTION_FUTURE;
        }

        ((ISectionHolderListener)this.chunkHolderListener).onUpdateSectionLevel(this.sectionPos, () -> getCompletedLevel(), this.chunkLevel,
                p_219275_1_ -> setCompletedLevel(p_219275_1_));
        this.prevChunkLevel = this.chunkLevel;
    }

    // func_219276_a
    @Override
    public CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> createSectionFuture(ChunkStatus chunkStatus, ChunkManager chunkManager)
    {
        int i = chunkStatus.ordinal();
        CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> completablefuture = this.sectionFutureBySectionStatus.get(i);
        if (completablefuture != null) {
            Either<ISection, ChunkHolder.IChunkLoadingError> either = completablefuture.getNow((Either<ISection, ChunkHolder.IChunkLoadingError>)null);
            if (either == null || either.left().isPresent()) {
                return completablefuture;
            }
        }

        if (getChunkStatusFromLevel(this.chunkLevel).isAtLeast(chunkStatus)) {
            CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> completablefuture1 =
                    ((IChunkManager)chunkManager).createSectionFuture((ChunkHolder)(Object)this, chunkStatus);
            this.chainSection(completablefuture1);
            this.sectionFutureBySectionStatus.set(i, completablefuture1);
            return completablefuture1;
        } else {
            return completablefuture == null ? MISSING_CHUNK_FUTURE : completablefuture;
        }

        //This is a CubeHolder, we set return value, preventing vanilla code, and running ours (below)
    }

    //BEGIN OVERRIDES:

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

    // getChunkIfComplete
    @Nullable
    @Override
    public ChunkSection getSectionIfComplete() {
        CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture = this.tickingSectionFuture;
        Either<ChunkSection, ChunkHolder.IChunkLoadingError> either = completablefuture.getNow((Either<ChunkSection, ChunkHolder.IChunkLoadingError>)null);
        return either == null ? null : either.left().orElse((ChunkSection)null);
    }

    // chain
    @Override
    public void chainSection(CompletableFuture<? extends Either<? extends ISection, ChunkHolder.IChunkLoadingError>> eitherChunk) {
        this.sectionFuture = this.sectionFuture.thenCombine(eitherChunk, (p_219295_0_, p_219295_1_) -> {
            return p_219295_1_.map((p_219283_0_) -> {
                return p_219283_0_;
            }, (p_219288_1_) -> {
                return p_219295_0_;
            });
        });
    }

    // func_219301_a
    @Override
    public CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> getSectionFuture(ChunkStatus chunkStatus) {
        CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> completablefuture =
                this.sectionFutureBySectionStatus.get(chunkStatus.ordinal());
        return completablefuture == null ? MISSING_CHUNK_FUTURE : completablefuture;
    }

    // TODO: this needs to be completely replaced for proper section handling
    /**
     * @author Barteks2x
     * @reason height limits
     */
    @Overwrite
    public void markBlockChanged(int x, int y, int z) {
        Chunk chunk = ((ChunkHolder) (Object) this).getChunkIfComplete();
        if (chunk == null) {
            return;
        }
        this.blockChangeMask |= 1 << (y >> 4);
        short pos = (short) AddressTools.getLocalAddress(x, y & 0xF, z);
        int sectionY = Coords.blockToCube(y);
        ShortArraySet changed = changedLocalBlocks.get(sectionY);
        if (changed == null) {
            changed = new ShortArraySet(32);
            changedLocalBlocks.put(sectionY, changed);
        }
        changed.add(pos);
        changedBlocks++;
    }

    /**
     * @author Barteks2x
     * @reason replace packet classes with CC packets
     */
    @Overwrite
    public void sendChanges(Chunk chunkIn) {
        if (this.changedLocalBlocks.isEmpty() && this.skyLightChangeMask == 0 && this.blockLightChangeMask == 0) {
            return;
        }
        World world = chunkIn.getWorld();
        if (this.skyLightChangeMask != 0 || this.blockLightChangeMask != 0) {
            this.sendToTracking(new SUpdateLightPacket(chunkIn.getPos(), this.lightManager, this.skyLightChangeMask & ~this.boundaryMask,
                    this.blockLightChangeMask & ~this.boundaryMask), true);
            int i = this.skyLightChangeMask & this.boundaryMask;
            int j = this.blockLightChangeMask & this.boundaryMask;
            if (i != 0 || j != 0) {
                this.sendToTracking(new SUpdateLightPacket(chunkIn.getPos(), this.lightManager, i, j), false);
            }

            this.skyLightChangeMask = 0;
            this.blockLightChangeMask = 0;
            this.boundaryMask &= ~(this.skyLightChangeMask & this.blockLightChangeMask);
        }

        for (IntIterator iterator = changedLocalBlocks.keySet().iterator(); iterator.hasNext(); ) {
            int sectionY = iterator.nextInt();
            ShortArraySet changed = changedLocalBlocks.get(sectionY);
            int changedBlocks = changed.size();
            //if (changed.size() >= net.minecraftforge.common.ForgeConfig.SERVER.clumpingThreshold.get()) {
            //    this.boundaryMask = -1;
            //}

            if (changedBlocks >= net.minecraftforge.common.ForgeConfig.SERVER.clumpingThreshold.get()) {
                this.sendToTracking(new PacketCubes(Collections.singletonMap(SectionPos.from(pos, sectionY),
                        chunkIn.getSections()[sectionY])), false);
            } else if (changedBlocks != 0) {
                this.sendToTracking(new PacketSectionBlockChanges(chunkIn.getSections()[sectionY],
                        SectionPos.from(pos, sectionY), new ShortArrayList(changed)), false);
                for (short pos : changed) {
                    BlockPos blockpos1 = new BlockPos(
                            AddressTools.getLocalX(pos) +  this.pos.x * 16,
                            AddressTools.getLocalY(pos) + sectionY*16,
                            AddressTools.getLocalZ(pos) + this.pos.z * 16
                    );
                    if (world.getBlockState(blockpos1).hasTileEntity()) {
                        this.sendTileEntity(world, blockpos1);
                    }
                }
            }
        }
        this.changedBlocks = 0;
        this.blockChangeMask = 0;
        changedLocalBlocks.clear();
    }

    private void sendToTracking(Object packetIn, boolean boundaryOnly) {
        this.playerProvider.getTrackingPlayers(this.pos, boundaryOnly)
                .forEach(player -> PacketDispatcher.sendTo(packetIn, player));
    }
}
