package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccess {


    @Accessor LevelHeightAccessor getLevelHeightAccessor();


}
