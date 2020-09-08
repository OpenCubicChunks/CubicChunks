package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.block.BlockState;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSection.class)
public interface ChunkSectionAccess {
    @Accessor short getNonEmptyBlockCount();
    @Accessor short getTickingBlockCount();
    @Accessor short getTickingFluidCount();

    @Accessor void setNonEmptyBlockCount(short value);
    @Accessor void setTickingBlockCount(short value);
    @Accessor void setTickingFluidCount(short value);

    @Accessor void setStates(PalettedContainer<BlockState> states);
}