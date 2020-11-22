package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.function.Predicate;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Heightmap.class)
public interface HeightmapAccess {
    @Accessor void setData(BitStorage value);
    @Accessor void setIsOpaque(Predicate<BlockState> value);
    @Accessor void setChunk(ChunkAccess value);

    @Accessor BitStorage getData();
    @Accessor Predicate<BlockState> getIsOpaque();
    @Accessor ChunkAccess getChunk();

    @Invoker void invokeSetHeight(int x, int z, int newY);
}
