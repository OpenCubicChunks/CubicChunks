package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunkSection.class)
public interface ChunkSectionAccess {
    @Accessor short getNonEmptyBlockCount();
    @Accessor short getTickingBlockCount();
    @Accessor short getTickingFluidCount();

    @Accessor void setNonEmptyBlockCount(short value);
    @Accessor void setTickingBlockCount(short value);
    @Accessor void setTickingFluidCount(short value);

    @Accessor void setStates(PalettedContainer<BlockState> states);
}