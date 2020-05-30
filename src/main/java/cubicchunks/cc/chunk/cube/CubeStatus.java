package cubicchunks.cc.chunk.cube;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.Util;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.List;

public class CubeStatus {
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
}
