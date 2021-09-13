package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import net.minecraft.world.level.levelgen.placement.FeatureDecorator;

public class SurfaceProjectedDecorator extends FeatureDecorator<CubicLakePlacementConfig> {

    public SurfaceProjectedDecorator(Codec<CubicLakePlacementConfig> codec) {
        super(codec);
    }

    @Override
    public Stream<BlockPos> getPositions(DecorationContext decorationContext, Random random, CubicLakePlacementConfig config, BlockPos blockPos) {
        List<BlockPos> positions = new ArrayList<>();

        for (int i = 0; i < IBigCube.DIAMETER_IN_SECTIONS; i++) {

            int x = blockPos.getX() + random.nextInt(16);
            int y = blockPos.getY() + random.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
            int z = blockPos.getZ() + random.nextInt(16);
            int surfaceHeight = decorationContext.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

            if (surfaceHeight < blockPos.getY()) {
                continue;
            }

            if (surfaceHeight >= blockPos.getY() + IBigCube.DIAMETER_IN_BLOCKS) {
                float probability = config.getSurfaceProbability().getValue(surfaceHeight);
                if (random.nextFloat() < probability) {
                    positions.add(new BlockPos(x, surfaceHeight, z));
                }
            } else {
                float probability = config.getMainProbability().getValue(surfaceHeight);
                if (random.nextFloat() < probability) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions.stream();
    }
}
