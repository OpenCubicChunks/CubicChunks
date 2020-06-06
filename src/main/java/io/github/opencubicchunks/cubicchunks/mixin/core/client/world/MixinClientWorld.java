package io.github.opencubicchunks.cubicchunks.mixin.core.client.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.BiFunction;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World {

    protected MixinClientWorld(WorldInfo info, DimensionType dimType,
            BiFunction<World, Dimension, AbstractChunkProvider> provider,
            IProfiler profilerIn, boolean remote) {
        super(info, dimType, provider, profilerIn, remote);
    }

    /**
     * @author NotStirred
     * @reason Vanilla chunks now do not have entities.
     */
    @Overwrite
    public void checkChunk(Entity entityIn) {
        this.getProfiler().startSection("chunkCheck");
        int i = MathHelper.floor(entityIn.getPosX() / 16.0D);
        int j = MathHelper.floor(entityIn.getPosY() / 16.0D);
        int k = MathHelper.floor(entityIn.getPosZ() / 16.0D);
        if (!entityIn.addedToChunk || entityIn.chunkCoordX != i || entityIn.chunkCoordY != j || entityIn.chunkCoordZ != k) {
            if (entityIn.addedToChunk && this.chunkExists(entityIn.chunkCoordX, entityIn.chunkCoordZ)) {
                this.getChunk(entityIn.chunkCoordX, entityIn.chunkCoordZ).removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
            }

            if (!entityIn.setPositionNonDirty() && !this.chunkExists(i, k)) {
                entityIn.addedToChunk = false;
            } else {
                this.getChunk(i, k).addEntity(entityIn);
            }
        }

        this.getProfiler().endSection();
    }

}
