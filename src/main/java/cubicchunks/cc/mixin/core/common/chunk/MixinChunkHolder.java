package cubicchunks.cc.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.ISectionHolder;
import cubicchunks.cc.chunk.ISectionHolderListener;
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
import net.minecraft.world.chunk.IChunk;
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

    private static final Either<ChunkSection, ChunkHolder.IChunkLoadingError> MISSING_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    private static final CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> MISSING_CHUNK_FUTURE =
            CompletableFuture.completedFuture(MISSING_SECTION);
    private static final Either<ChunkSection, ChunkHolder.IChunkLoadingError> UNLOADED_SECTION =
            Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    private static final CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> UNLOADED_SECTION_FUTURE =
            CompletableFuture.completedFuture(UNLOADED_SECTION);

    private volatile CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> tickingSectionFuture;

    private final AtomicReferenceArray<CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>>> sectionFutureBySectionStatus =
            new AtomicReferenceArray<>(CHUNK_STATUS_LIST.size());

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

    //BEGIN INJECTS:

    @Inject(method = "processUpdates", at = @At("HEAD"), cancellable = true)
    void processUpdates(ChunkManager chunkManagerIn, CallbackInfo ci) {
        /**
         If sectionPos == null, this is a ChunkManager
         else, this is a CubeManager.
         This is being implemented as a mixin, instead of having a specific CubeManager class.
         this.sectionPos is essentially being used as a flag for changing behaviour.
         */
        if(this.sectionPos == null) {
            //This is a ChunkHolder, we dont ci.cancel() and let the vanilla code run.
            return;
        }
        //This is a CubeHolder, we ci.cancel() preventing vanilla code, and running ours (below)
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
                CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture = this.sectionFutureBySectionStatus.get(i);
                if (completablefuture != null) {
                    completablefuture.complete(either);
                } else {
                    this.sectionFutureBySectionStatus.set(i, CompletableFuture.completedFuture(either));
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

        ((ISectionHolderListener) this.field_219327_v)
                .onUpdateSectionLevel(this.sectionPos, this::func_219281_j, this.chunkLevel, this::func_219275_d);
        this.prevChunkLevel = this.chunkLevel;
    }

    //BEGIN OVERRIDES:

    @Override
    public void setYPos(int yPos) { //Whenever ChunkHolder is instantiated this should be called to finish the construction of the object
        this.sectionPos = SectionPos.of(getPosition().x, yPos, getPosition().z);
    }
    // TODO: this needs to be completely replaced for proper section handling

    @Override
    public int getYPos()
    {
        return this.sectionPos.getY();
    }

    @Override
    public SectionPos getSectionPos() {
        return sectionPos;
    }

    @Nullable
    @Override
    public ChunkSection getSectionIfComplete() {
        CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture = this.tickingSectionFuture;
        Either<ChunkSection, ChunkHolder.IChunkLoadingError> either =
                completablefuture.getNow((Either<ChunkSection, ChunkHolder.IChunkLoadingError>) null);
        return either == null ? null : either.left().orElse((ChunkSection) null);
    }

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
