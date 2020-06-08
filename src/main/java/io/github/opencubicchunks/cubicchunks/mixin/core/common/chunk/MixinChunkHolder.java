package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolderListener;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.chunk.util.Utils;
import io.github.opencubicchunks.cubicchunks.network.PacketCubeBlockChanges;
import io.github.opencubicchunks.cubicchunks.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.utils.AddressTools;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements ICubeHolder {

    @Shadow private int prevChunkLevel;
    @Shadow private int chunkLevel;
    @Shadow private ChunkPos pos;

    @Shadow public static ChunkHolder.LocationType getLocationTypeFromLevel(int level) {
        throw new Error("Mixin failed to apply correctly!");
    }

    @Shadow private boolean accessible;

    //This is either a ChunkTaskPriorityQueueSorter or a CubeTaskPriorityQueueSorter depending on if this is a chunkholder or sectionholder.
    @Shadow(aliases = "field_219327_v") @Final private ChunkHolder.IListener chunkHolderListener;

    @Shadow(aliases = "func_219281_j") public abstract int getCompletedLevel();
    @Shadow(aliases = "func_219275_d") protected abstract void setCompletedLevel(int p_219275_1_);

    // these are using java type erasure as a feature - because the generic type information
    // doesn't exist at runtime, we can shadow those fields with different generic types
    // and as long as we are consistent, we can use them with different types than the declaration in the original class
    @Shadow(aliases = "field_219312_g") @Final private AtomicReferenceArray<CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>>> futureByStatus;
    @Shadow private volatile CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> borderFuture;
    @Shadow private volatile CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> tickingFuture;
    @Shadow private volatile CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> entityTickingFuture;
    @Shadow(aliases = "field_219315_j") private CompletableFuture<ICube> chunkFuture;


    @Shadow private int skyLightChangeMask;
    @Shadow private int blockLightChangeMask;
    @Shadow private int boundaryMask;

    @Shadow protected abstract void sendTileEntity(World worldIn, BlockPos posIn);

    @Shadow @Final private ChunkHolder.IPlayerProvider playerProvider;
    @Shadow private int field_219318_m;

    @SuppressWarnings("unused")
    private CubePos cubePos; // set from ASM

    private final ShortArraySet changedLocalBlocks = new ShortArraySet();

    //BEGIN INJECTS:

    @Dynamic
    @Inject(method = "<init>(Lio/github/opencubicchunks/cubicchunks/chunk/util/CubePos;ILnet/minecraft/world/lighting/WorldLightManager;"
            + "Lnet/minecraft/world/server/ChunkHolder$IListener;Lnet/minecraft/world/server/ChunkHolder$IPlayerProvider;)V",
            at = @At("RETURN")
    )
    public void onConstructCubeHolder(CubePos cubePosIn, int levelIn, WorldLightManager lightManagerIn, ChunkHolder.IListener p_i50716_4_,
            ChunkHolder.IPlayerProvider playerProviderIn, CallbackInfo ci) {
        this.borderFuture = UNLOADED_CUBE_FUTURE;
        this.tickingFuture = UNLOADED_CUBE_FUTURE;
        this.entityTickingFuture = UNLOADED_CUBE_FUTURE;
        this.chunkFuture = CompletableFuture.completedFuture(null);
        this.prevChunkLevel = IChunkManager.MAX_CUBE_LOADED_LEVEL + 1;
        this.field_219318_m = this.prevChunkLevel;
        this.pos = cubePosIn.asChunkPos();
    }

    // used from ASM
    @SuppressWarnings("unused") private static ChunkStatus getCubeStatusFromLevel(int cubeLevel) {
        return ICubeHolder.getCubeStatusFromLevel(cubeLevel);
    }

    @Inject(method = "processUpdates", at = @At("HEAD"), cancellable = true)
    void processUpdates(ChunkManager chunkManagerIn, CallbackInfo ci) {
        /*
         If sectionPos == null, this is a ChunkManager
         else, this is a CubeManager.
         This is being implemented as a mixin, instead of having a specific CubeManager class.
         this.sectionPos is essentially being used as a flag for changing behaviour.
         */

        if(this.cubePos == null)
        {
            return;
        }
        ci.cancel();

        ChunkStatus chunkstatus = ICubeHolder.getCubeStatusFromLevel(this.prevChunkLevel);
        ChunkStatus chunkstatus1 = ICubeHolder.getCubeStatusFromLevel(this.chunkLevel);
        boolean wasFullyLoaded = this.prevChunkLevel <= IChunkManager.MAX_CUBE_LOADED_LEVEL;
        boolean isFullyLoaded = this.chunkLevel <= IChunkManager.MAX_CUBE_LOADED_LEVEL;
        ChunkHolder.LocationType previousLocationType = getLocationTypeFromLevel(this.prevChunkLevel);
        ChunkHolder.LocationType currentLocationType = getLocationTypeFromLevel(this.chunkLevel);
        if (wasFullyLoaded) {
            @SuppressWarnings("MixinInnerClass")
            Either<ICube, ChunkHolder.IChunkLoadingError> either = Either.right(new ChunkHolder.IChunkLoadingError() {
                public String toString() {
                    return "Unloaded ticket level " + cubePos.toString();
                }
            });
            for(int i = isFullyLoaded ? chunkstatus1.ordinal() + 1 : 0; i <= chunkstatus.ordinal(); ++i) {
                CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture = this.futureByStatus.get(i);
                if (completablefuture != null) {
                    completablefuture.complete(either);
                } else {
                    this.futureByStatus.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean wasBorder = previousLocationType.isAtLeast(ChunkHolder.LocationType.BORDER);
        boolean isBorder = currentLocationType.isAtLeast(ChunkHolder.LocationType.BORDER);
        this.accessible |= isBorder;
        if (!wasBorder && isBorder) {
            this.borderFuture = ((IChunkManager)chunkManagerIn).createCubeBorderFuture((ChunkHolder)(Object)this);
            this.chainCube(this.borderFuture);
        }

        if (wasBorder && !isBorder) {
            CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> completablefuture1 = this.borderFuture;
            this.borderFuture = UNLOADED_CUBE_FUTURE;
            this.chainCube(Utils.unsafeCast(completablefuture1.thenApply((chunkLoadingErrorEither) -> {
                return chunkLoadingErrorEither.ifLeft(((IChunkManager)chunkManagerIn)::saveCubeScheduleTicks);
            })));
        }

        boolean wasTicking = previousLocationType.isAtLeast(ChunkHolder.LocationType.TICKING);
        boolean isTicking = currentLocationType.isAtLeast(ChunkHolder.LocationType.TICKING);
        if (!wasTicking && isTicking) {
            this.tickingFuture = ((IChunkManager)chunkManagerIn).createCubeTickingFuture((ChunkHolder)(Object)this);
            this.chainCube(Utils.unsafeCast(this.tickingFuture));
        }

        if (wasTicking && !isTicking) {
            this.tickingFuture.complete(UNLOADED_CUBE);
            this.tickingFuture = UNLOADED_CUBE_FUTURE;
        }

        boolean wasEntityTicking = previousLocationType.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        boolean isEntityTicking = currentLocationType.isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING);
        if (!wasEntityTicking && isEntityTicking) {
            if (this.entityTickingFuture != UNLOADED_CUBE_FUTURE) {
                throw (IllegalStateException) Util.pauseDevMode(new IllegalStateException());
            }

            this.entityTickingFuture = ((IChunkManager)chunkManagerIn).createCubeEntityTickingFuture(this.cubePos);
            this.chainCube(Utils.unsafeCast(this.entityTickingFuture));
        }

        if (wasEntityTicking && !isEntityTicking) {
            this.entityTickingFuture.complete(UNLOADED_CUBE);
            this.entityTickingFuture = UNLOADED_CUBE_FUTURE;
        }

        ((ICubeHolderListener)this.chunkHolderListener).onUpdateCubeLevel(this.cubePos, () -> getCompletedLevel(), this.chunkLevel,
                p_219275_1_ -> setCompletedLevel(p_219275_1_));
        this.prevChunkLevel = this.chunkLevel;
    }

    @Override
    public CubePos getCubePos() {
        return cubePos;
    }

    // getChunkIfComplete
    @Nullable
    @Override
    public Cube getCubeIfComplete() {
        CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> completablefuture = this.tickingFuture;
        Either<Cube, ChunkHolder.IChunkLoadingError> either = completablefuture.getNow(null);
        return either == null ? null : either.left().orElse(null);
    }

    @Override
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> getCubeEntityTickingFuture() {
        return this.entityTickingFuture;
    }

    // chain
    @Override
    public void chainCube(CompletableFuture<? extends Either<? extends ICube, ChunkHolder.IChunkLoadingError>> eitherChunk) {
        this.chunkFuture = this.chunkFuture.thenCombine(eitherChunk, (cube, cubeOrError) -> {
            return cubeOrError.map((existingCube) -> {
                return existingCube;
            }, (error) -> {
                return cube;
            });
        });
    }

    // func_219301_a
    @Override
    public CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getCubeFuture(ChunkStatus chunkStatus) {
        CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture =
                this.futureByStatus.get(chunkStatus.ordinal());
        return completablefuture == null ? MISSING_CUBE_FUTURE : completablefuture;
    }

    // func_219302_f
    @Override
    public CompletableFuture<ICube> getCurrentCubeFuture() {
        return chunkFuture;
    }

    // func_219301_a
    public CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getFutureByCubeStatus(ChunkStatus chunkStatus) {
        CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture =
                this.futureByStatus.get(chunkStatus.ordinal());
        return completablefuture == null ? MISSING_CUBE_FUTURE : completablefuture;
    }
    // func_225410_b
    @Override public CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getFutureHigherThanCubeStatus(ChunkStatus chunkStatus) {
        return ICubeHolder.getCubeStatusFromLevel(this.chunkLevel).isAtLeast(chunkStatus) ? this.getFutureByCubeStatus(chunkStatus) : MISSING_CUBE_FUTURE;
    }

    // func_219276_a
    @Override
    public CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkStatus status, ChunkManager chunkManager) {
        int statusOrdinal = status.ordinal();
        CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture = this.futureByStatus.get(statusOrdinal);
        if (completablefuture != null) {
            Either<ICube, ChunkHolder.IChunkLoadingError> either = completablefuture.getNow(null);
            if (either == null || either.left().isPresent()) {
                return completablefuture;
            }
        }

        if (ICubeHolder.getCubeStatusFromLevel(this.chunkLevel).isAtLeast(status)) {
            CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture1 =
                    ((IChunkManager)chunkManager).createCubeFuture((ChunkHolder)(Object)this, status);
            this.chainCube(completablefuture1);
            this.futureByStatus.set(statusOrdinal, completablefuture1);
            return completablefuture1;
        } else {
            return completablefuture == null ? MISSING_CUBE_FUTURE : completablefuture;
        }
    }

    //TODO: check if this todo is still valid:
    // TODO: this needs to be completely replaced for proper section handling
    /**
     * @author Barteks2x**
     * @reason height limits
     */
    @Overwrite
    public void markBlockChanged(int x, int y, int z) {
        if (cubePos == null) {
            throw new IllegalStateException("Why is this getting called?");
        }
        Cube cube = getCubeIfComplete();
        if (cube == null) {
            return;
        }
        changedLocalBlocks.add((short) AddressTools.getLocalAddress(x, y, z));
    }

    /**
     * @author Barteks2x
     * @reason replace packet classes with CC packets
     */
    @Overwrite
    public void sendChanges(Chunk chunkIn) {
        if (cubePos != null) {
            throw new IllegalStateException("Why is this getting called?");
        }
        // noop
    }

    @Override
    public void sendChanges(Cube cube) {
        if (cubePos == null) {
            throw new IllegalStateException("sendChanges(Cube) called on column holder!");
        }
        if (this.changedLocalBlocks.isEmpty() && this.skyLightChangeMask == 0 && this.blockLightChangeMask == 0) {
            return;
        }
        World world = cube.getWorld();
        // if (this.skyLightChangeMask != 0 || this.blockLightChangeMask != 0) {
        //     this.sendToTracking(new SUpdateLightPacket(section.getPos(), this.lightManager, this.skyLightChangeMask & ~this.boundaryMask,
        //             this.blockLightChangeMask & ~this.boundaryMask), true);
        //     int i = this.skyLightChangeMask & this.boundaryMask;
        //     int j = this.blockLightChangeMask & this.boundaryMask;
        //     if (i != 0 || j != 0) {
        //         this.sendToTracking(new SUpdateLightPacket(section.getPos(), this.lightManager, i, j), false);
        //     }
        //     this.skyLightChangeMask = 0;
        //     this.blockLightChangeMask = 0;
        //     this.boundaryMask &= ~(this.skyLightChangeMask & this.blockLightChangeMask);
        // }

        ShortArraySet changed = changedLocalBlocks;
        int changedBlocks = changed.size();
        if (changed.size() >= net.minecraftforge.common.ForgeConfig.SERVER.clumpingThreshold.get()) {
            this.boundaryMask = -1;
        }

        if (changedBlocks >= net.minecraftforge.common.ForgeConfig.SERVER.clumpingThreshold.get()) {
            this.sendToTracking(new PacketCubes(Collections.singletonList(cube)), false);
        } else if (changedBlocks != 0) {
            this.sendToTracking(new PacketCubeBlockChanges(cube, new ShortArrayList(changed)), false);
            for (short pos : changed) {
                BlockPos blockpos1 = new BlockPos(
                        this.cubePos.blockX(AddressTools.getLocalX(pos)),
                        this.cubePos.blockY(AddressTools.getLocalY(pos)),
                        this.cubePos.blockZ(AddressTools.getLocalZ(pos)));
                if (world.getBlockState(blockpos1).hasTileEntity()) {
                    this.sendTileEntity(world, blockpos1);
                }
            }
        }
        changedLocalBlocks.clear();
    }

    private void sendToTracking(Object packetIn, boolean boundaryOnly) {
        // TODO: fix block update tracking
        this.playerProvider.getTrackingPlayers(this.cubePos.asChunkPos(), boundaryOnly)
                .forEach(player -> PacketDispatcher.sendTo(packetIn, player));
    }

    // func_219294_a
    @Override
    public void onCubeWrapperCreated(CubePrimerWrapper primer) {
        for(int i = 0; i < this.futureByStatus.length(); ++i) {
            CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> future = this.futureByStatus.get(i);
            if (future != null) {
                Optional<ICube> optional = future.getNow(MISSING_CUBE).left();
                if (optional.isPresent() && optional.get() instanceof CubePrimer) {
                    this.futureByStatus.set(i, CompletableFuture.completedFuture(Either.left(primer)));
                }
            }
        }

        this.chainCube(CompletableFuture.completedFuture(Either.left((ICube) primer.getCube())));
    }
}
