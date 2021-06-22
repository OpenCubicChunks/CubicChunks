package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.function.Predicate;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Heightmap.class)
public interface HeightmapAccess {
    @Mutable @Accessor void setData(BitStorage value);
    @Mutable @Accessor void setIsOpaque(Predicate<BlockState> value);
    @Mutable @Accessor void setChunk(ChunkAccess value);

    @Accessor BitStorage getData();
    @Accessor Predicate<BlockState> getIsOpaque();
    @Accessor ChunkAccess getChunk();

    @Invoker void invokeSetHeight(int x, int z, int newY);
}
