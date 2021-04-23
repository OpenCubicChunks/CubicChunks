package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public class ProtoColumnBiomeContainer extends ChunkBiomeContainer {


    protected ProtoColumnBiomeContainer(IdMap<Biome> idMap, LevelHeightAccessor levelHeightAccessor,
                                        Biome[] biomes) {
        super(idMap, levelHeightAccessor, biomes);
    }




    public static class ProtoColumnContainerHeightAccessor implements LevelHeightAccessor {

        private final int minBuildHeight;
        private final int height;

        public ProtoColumnContainerHeightAccessor(IBigCube[] cubes) {
            this.minBuildHeight = cubes[0].getMinBuildHeight();
            this.height = Math.abs(cubes[cubes.length - 1].getMaxBuildHeight() - cubes[0].getMinBuildHeight());
        }



        @Override public int getHeight() {
            return this.height;
        }

        @Override public int getMinBuildHeight() {
            return this.minBuildHeight;
        }
    }
}
