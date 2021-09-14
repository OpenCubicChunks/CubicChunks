package io.github.opencubicchunks.cubicchunks.world.storage;

import java.util.function.Function;
import java.util.function.Predicate;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ProtoTickListAccess;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoTickList;

public class CubeProtoTickList<T> extends ProtoTickList<T> {

    public CubeProtoTickList(Predicate<T> predicate, ImposterChunkPos chunkPos, ListTag listTag, CubeProtoTickListHeightAccess levelHeightAccessor) {
        super(predicate, chunkPos, listTag, levelHeightAccessor);
    }

    public CubeProtoTickList(Predicate<T> predicate, ImposterChunkPos chunkPos, CubeProtoTickListHeightAccess levelHeightAccessor) {
        this(predicate, chunkPos, new ListTag(), levelHeightAccessor);
    }

    @Override public void scheduleTick(BlockPos pos, T object, int delay, TickPriority priority) {
        ChunkAccess.getOrCreateOffsetList(((ProtoTickListAccess) this).getToBeTicked(), Coords.blockToIndex(pos.getX(), pos.getY(), pos.getZ()))
            .add(ProtoChunk.packOffsetCoordinates(pos));
    }

    @Override public void copyOut(TickList<T> scheduler, Function<BlockPos, T> dataMapper) {
        for (int tickListIDX = 0; tickListIDX < ((ProtoTickListAccess) this).getToBeTicked().length; ++tickListIDX) {
            if (((ProtoTickListAccess) this).getToBeTicked()[tickListIDX] != null) {

                for (Short sectionRel : ((ProtoTickListAccess) this).getToBeTicked()[tickListIDX]) {
                    BlockPos blockPos = ProtoCube.unpackToWorld(sectionRel, tickListIDX, ((ImposterChunkPos) ((ProtoTickListAccess) this).getChunkPos()).toCubePos());
                    scheduler.scheduleTick(blockPos, dataMapper.apply(blockPos), 0);
                }

                ((ProtoTickListAccess) this).getToBeTicked()[tickListIDX].clear();
            }
        }
    }

    public static class CubeProtoTickListHeightAccess implements LevelHeightAccessor, CubicLevelHeightAccessor {

        private final CubePos pos;
        private final CubicLevelHeightAccessor accessor;

        public CubeProtoTickListHeightAccess(CubePos pos, CubicLevelHeightAccessor accessor) {
            this.pos = pos;
            this.accessor = accessor;
        }

        @Override public int getHeight() {
            return 0;
        }

        @Override public int getMinBuildHeight() {
            return 0;
        }

        @Override public int getSectionsCount() {
            return CubeAccess.SECTION_COUNT;
        }

        @Override public int getSectionYFromSectionIndex(int index) {
            return Coords.cubeToSection(this.pos.getY(), Coords.indexToY(index));
        }

        @Override public WorldStyle worldStyle() {
            return this.accessor.worldStyle();
        }

        @Override public boolean isCubic() {
            return true;
        }

        @Override public boolean generates2DChunks() {
            return this.accessor.generates2DChunks();
        }
    }
}
