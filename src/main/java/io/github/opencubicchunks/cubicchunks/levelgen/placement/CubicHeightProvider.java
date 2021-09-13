package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import java.util.OptionalInt;
import java.util.Random;

import net.minecraft.world.level.levelgen.WorldGenerationContext;

public interface CubicHeightProvider {

    OptionalInt sampleCubic(Random random, WorldGenerationContext context, int minY, int maxY);
}
