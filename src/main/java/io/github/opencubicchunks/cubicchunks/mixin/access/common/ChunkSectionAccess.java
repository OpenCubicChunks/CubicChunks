package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.block.BlockState;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSection.class)
public interface ChunkSectionAccess {
    @Accessor short getBlockRefCount();
    @Accessor short getBlockTickRefCount();
    @Accessor short getFluidRefCount();

    @Accessor void setBlockRefCount(short value);
    @Accessor void setBlockTickRefCount(short value);
    @Accessor void setFluidRefCount(short value);

    @Accessor void setData(PalettedContainer<BlockState> data);
}