package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.BitSet;
import java.util.Map;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccess {

    @Accessor
    Map<GenerationStep.Carving, BitSet> getCarvingMasks();

    @Accessor Map<Heightmap.Types, Heightmap> getHeightmaps();
}
