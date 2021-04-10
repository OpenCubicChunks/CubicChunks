package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerTickList.class)
public abstract class MixinServerTickList<T> {


    @Shadow @Final private ServerLevel level;

    @Shadow public abstract List<TickNextTickData<T>> fetchTicksInArea(BoundingBox bounds, boolean updateState, boolean getStaleTicks);

    @Inject(method = "fetchTicksInChunk", at = @At("HEAD"), cancellable = true)
    private void fetchTicksInCube(ChunkPos pos, boolean updateState, boolean getStaleTicks, CallbackInfoReturnable<List<TickNextTickData<T>>> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        if (pos instanceof ImposterChunkPos) {
            CubePos cubePos = ((ImposterChunkPos) pos).toCubePos();
            // TODO: Follow up on this in 1.17 release, behavior is different from vanilla. Vanilla does NOT offset bb max by 2.
            List<TickNextTickData<T>> fetchedTicks = this.fetchTicksInArea(new BoundingBox(cubePos.minCubeX() - 2, cubePos.minCubeY() - 2, cubePos.minCubeZ() - 2, cubePos.maxCubeX() + 2,
                cubePos.maxCubeY() + 2, cubePos.maxCubeZ() + 2), updateState, getStaleTicks);

            if (!fetchedTicks.isEmpty()) {
                String s = "";
            }
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
}
