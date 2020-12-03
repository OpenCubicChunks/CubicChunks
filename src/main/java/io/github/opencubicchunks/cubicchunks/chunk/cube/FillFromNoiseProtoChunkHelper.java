package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;

public class FillFromNoiseProtoChunkHelper extends ProtoChunk {

    private final int relativeColumnX;
    private final int relativeColumnZ;

    private final CubePrimer cubePrimer;


    public FillFromNoiseProtoChunkHelper(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, CubePrimer cubePrimer, int relativeColumnX, int relativeColumnZ) {
        super(chunkPos, upgradeData, levelHeightAccessor);
        this.cubePrimer = cubePrimer;
        this.relativeColumnX = relativeColumnX;
        this.relativeColumnZ = relativeColumnZ;
    }


    @Override public ChunkPos getPos() {
        return this.cubePrimer.getCubePos().asChunkPos(relativeColumnX, relativeColumnZ);
    }

    @Override public LevelChunkSection getOrCreateSection(int y) {
        LevelChunkSection[] cubeSections = this.cubePrimer.getCubeSections();
        int sectionIndex = this.getSectionIndex(y);

        if (cubeSections[sectionIndex] == LevelChunk.EMPTY_SECTION) {
            cubeSections[sectionIndex] = new LevelChunkSection(this.getSectionYFromSectionIndex(y));
        }
        return cubeSections[sectionIndex];
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return super.getOrCreateHeightmapUnprimed(type); //TODO
    }



//    @Override public int getSectionsCount() {
//        return IBigCube.DIAMETER_IN_SECTIONS;
//    }

    @Override public int getSectionIndex(int y) {
        return Coords.sectionToIndex(relativeColumnX, Coords.blockToSection(y), relativeColumnZ);
    }

    @Override public void addLight(BlockPos pos) {
        super.addLight(pos); //TODO
    }
}
