package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunkSection.class)
public interface LevelChunkSectionAccess {
    @Accessor short getNonEmptyBlockCount();
    @Accessor short getTickingBlockCount();
    @Accessor short getTickingFluidCount();

    @Mutable @Accessor void setStates(PalettedContainer<BlockState> states);
}