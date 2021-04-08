package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ProtoTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProtoTickList.class)
public interface ProtoTickListAccess {

    @Accessor
    ShortList[] getToBeTicked();

    @Accessor
    LevelHeightAccessor getLevelHeightAccessor();

    @Accessor
    ChunkPos getChunkPos();
}
