package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class CuboidPrimer extends CubePrimer {


    public CuboidPrimer(CubePos cubePos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor) {
        super(cubePos, upgradeData, levelHeightAccessor);
    }

    public CuboidPrimer(CubePos cubePosIn, UpgradeData upgradeData,
                        @Nullable LevelChunkSection[] sectionsIn,
                        ProtoTickList<Block> blockProtoTickList,
                        ProtoTickList<Fluid> fluidProtoTickList,
                        LevelHeightAccessor levelHeightAccessor) {
        super(cubePosIn, upgradeData, sectionsIn, blockProtoTickList, fluidProtoTickList, levelHeightAccessor);
    }
}
