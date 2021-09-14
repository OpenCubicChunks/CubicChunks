package io.github.opencubicchunks.cubicchunks.mixin.core.common.level;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.world.CubicServerTickList;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerTickList.class)
public abstract class MixinServerTickList<T> implements TickList<T>, CubicServerTickList<T> {

    @Mutable @Shadow @Final private Set<TickNextTickData<T>> tickNextTickList;

    @Shadow @Final private ServerLevel level;

    @Shadow @Final private Set<TickNextTickData<T>> tickNextTickSet;

    @Shadow @Final private Queue<TickNextTickData<T>> currentlyTicking;

    @Shadow @Final private List<TickNextTickData<T>> alreadyTicked;

    @Shadow @Final private Consumer<TickNextTickData<T>> ticker;

    @Shadow public abstract List<TickNextTickData<T>> fetchTicksInArea(BoundingBox bounds, boolean updateState, boolean getStaleTicks);

    @Shadow public abstract void scheduleTick(BlockPos pos, T object, int delay, TickPriority priority);

    @Shadow public abstract ListTag save(ChunkPos chunkPos);

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void useFasterSetImpl(ServerLevel serverLevel, Predicate<T> predicate, Function<T, ResourceLocation> function, Consumer<TickNextTickData<T>> consumer, CallbackInfo ci) {
        tickNextTickList = new ObjectAVLTreeSet<>(TickNextTickData.createTimeComparator());
    }

    //INTENTIONALLY CANCELLING ON VANILLA WORLDS, AS THEY GET OUR OPTIMISATION TOO - NotStirred
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void serverTickListOptimisation(CallbackInfo ci) {
        ci.cancel();
        int i = this.tickNextTickList.size();
        if (i != this.tickNextTickSet.size()) {
            throw new IllegalStateException("TickNextTick list out of synch");
        } else {
            if (i > 65536) {
                i = 65536;
            }
            Iterator<TickNextTickData<T>> iterator = this.tickNextTickList.iterator();
            this.level.getProfiler().push("cleaning");

            long gameTime = this.level.getGameTime();
            TickNextTickData<T> tickNextTickData2;
            while (i > 0 && iterator.hasNext()) {
                tickNextTickData2 = iterator.next();
                if (tickNextTickData2.triggerTick > gameTime) {
                    break;
                }

                if (level.isPositionTickingWithEntitiesLoaded(tickNextTickData2.pos)) {
                    iterator.remove();
                    this.tickNextTickSet.remove(tickNextTickData2);
                    this.currentlyTicking.add(tickNextTickData2);
                    --i;
                }
            }

            this.level.getProfiler().popPush("ticking");

            while ((tickNextTickData2 = this.currentlyTicking.poll()) != null) {
                try {
                    this.alreadyTicked.add(tickNextTickData2);
                    this.ticker.accept(tickNextTickData2);
                } catch (Throwable var8) {
                    CrashReport crashReport = CrashReport.forThrowable(var8, "Exception while ticking");
                    CrashReportCategory crashReportCategory = crashReport.addCategory("Block being ticked");
                    CrashReportCategory.populateBlockDetails(crashReportCategory, this.level, tickNextTickData2.pos, null);
                    throw new ReportedException(crashReport);
                }
            }

            this.level.getProfiler().pop();
            this.alreadyTicked.clear();
            this.currentlyTicking.clear();
        }
    }

    @Inject(method = "fetchTicksInChunk", at = @At("HEAD"), cancellable = true)
    private void fetchTicksInCube(ChunkPos pos, boolean updateState, boolean getStaleTicks, CallbackInfoReturnable<List<TickNextTickData<T>>> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        if (pos instanceof ImposterChunkPos) {
            CubePos cubePos = ((ImposterChunkPos) pos).toCubePos();
            // TODO: Follow up on this in 1.17 release, behavior is different from vanilla. Vanilla does NOT offset bb max by 2.
            List<TickNextTickData<T>> fetchedTicks = this.fetchTicksInArea(new BoundingBox(cubePos.minCubeX() - 2, cubePos.minCubeY() - 2, cubePos.minCubeZ() - 2,
                cubePos.maxCubeX() + 1, cubePos.maxCubeY() + 1, cubePos.maxCubeZ() + 1), updateState, getStaleTicks);
            cir.setReturnValue(fetchedTicks);
        }
    }


    @Inject(method = "fetchTicksInArea(Ljava/util/List;Ljava/util/Collection;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Z)Ljava/util/List;", at = @At("HEAD"),
        cancellable = true)
    private void fetchTicks3D(@Nullable List<TickNextTickData<T>> dst, Collection<TickNextTickData<T>> src, BoundingBox bounds, boolean move,
                              CallbackInfoReturnable<List<TickNextTickData<T>>> cir) {
        Iterator<TickNextTickData<T>> iterator = src.iterator();

        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return;
        }

        while (iterator.hasNext()) {
            TickNextTickData<T> tickNextTickData = iterator.next();
            BlockPos blockPos = tickNextTickData.pos;
            if (blockPos.getX() >= bounds.minX() && blockPos.getX() < bounds.maxX() && blockPos.getY() >= bounds.minY() && blockPos.getY() < bounds.maxY() && blockPos.getZ() >= bounds.minZ()
                && blockPos.getZ() < bounds.maxZ()) {
                if (move) {
                    iterator.remove();
                }

                if (dst == null) {
                    dst = Lists.newArrayList();
                }

                (dst).add(tickNextTickData);
            }
        }

        cir.setReturnValue(dst);
    }

    @Override public List<TickNextTickData<T>> fetchTicksInCube(CubePos cubePos, boolean updateState, boolean getStaleTicks) {
        return this.fetchTicksInArea(new BoundingBox(cubePos.minCubeX() - 2, cubePos.minCubeY() - 2, cubePos.minCubeZ() - 2,
            cubePos.maxCubeX() + 1, cubePos.maxCubeY() + 1, cubePos.maxCubeZ() + 1), updateState, getStaleTicks);
    }

    @Override public ListTag save(CubePos cubePos) {
        return this.save(new ImposterChunkPos(cubePos));
    }
}
