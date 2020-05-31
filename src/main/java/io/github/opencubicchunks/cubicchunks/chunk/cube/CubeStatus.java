package io.github.opencubicchunks.cubicchunks.chunk.cube;

import com.google.common.collect.ImmutableList;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.List;

public class CubeStatus {
    private static final Object2IntOpenHashMap<ChunkStatus> CUBE_TASK_RANGE_XZ = new Object2IntOpenHashMap<>();

    static {
        for (ChunkStatus chunkStatus : ChunkStatus.getAll()) {
            int r = MathUtil.ceilDiv(chunkStatus.getTaskRange(), 2);
            CUBE_TASK_RANGE_XZ.put(chunkStatus, r);
        }
    }

    private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(
            ChunkStatus.FULL,
            ChunkStatus.FEATURES,
            ChunkStatus.LIQUID_CARVERS,
            ChunkStatus.STRUCTURE_STARTS,
            ChunkStatus.STRUCTURE_STARTS,
            ChunkStatus.STRUCTURE_STARTS,
            ChunkStatus.STRUCTURE_STARTS
    );

    private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(ChunkStatus.getAll().size()), (rangeByStatus) -> {
        int range = 0;

        for(int status = ChunkStatus.getAll().size() - 1; status >= 0; --status) {
            while(range + 1 < STATUS_BY_RANGE.size() && status <= STATUS_BY_RANGE.get(range + 1).ordinal()) {
                ++range;
            }
            rangeByStatus.add(0, range);
        }
    });

    public static ChunkStatus getStatus(int distance) {
        if (distance >= STATUS_BY_RANGE.size()) {
            return ChunkStatus.EMPTY;
        } else {
            return distance < 0 ? ChunkStatus.FULL : STATUS_BY_RANGE.get(distance);
        }
    }

    public static int maxDistance() {
        return STATUS_BY_RANGE.size();
    }

    public static int getDistance(ChunkStatus status) {
        return RANGE_BY_STATUS.getInt(status.ordinal());
    }

    public static int getCubeTaskRange(ChunkStatus chunkStatusIn) {
        return CUBE_TASK_RANGE_XZ.getInt(chunkStatusIn);
    }
}
