package io.github.opencubicchunks.cubicchunks.mixin.core.client.world;

import io.github.opencubicchunks.cubicchunks.world.client.IClientWorld;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World implements IClientWorld {

    protected MixinClientWorld(ISpawnWorldInfo p_i231617_1_, RegistryKey<World> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<IProfiler> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
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

            // func_233577_ch_ = setPositionDirty
            if (!entityIn.func_233577_ch_() && !this.chunkExists(i, k)) {
                entityIn.addedToChunk = false;
            } else {
                this.getChunk(i, k).addEntity(entityIn);
            }
        }

        this.getProfiler().endSection();
    }

    @Override
    public void onCubeLoaded(int cubeX, int cubeY, int cubeZ)
    {
        //TODO: implement colorCaches in onCubeLoaded
//        this.colorCaches.forEach((p_228316_2_, p_228316_3_) -> {
//            p_228316_3_.invalidateChunk(chunkX, chunkZ);
//        });
    }
}
