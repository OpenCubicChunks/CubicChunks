package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccess {

    @Accessor
    int getWriteRadiusCutoff();

    @Accessor
    ChunkStatus getGeneratingStatus();

    @Nullable
    @Accessor
    Supplier<String> getCurrentlyGenerating();
}