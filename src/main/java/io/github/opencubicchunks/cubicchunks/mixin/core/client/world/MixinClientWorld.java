package io.github.opencubicchunks.cubicchunks.mixin.core.client.world;

import io.github.opencubicchunks.cubicchunks.world.client.IClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ClientLevel.class)
public abstract class MixinClientWorld extends Level implements IClientWorld {

    protected MixinClientWorld(WritableLevelData p_i231617_1_, ResourceKey<Level> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<ProfilerFiller> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    /**
     * @author NotStirred
     * @reason Vanilla chunks now do not have entities.
     */
    @Overwrite
    private void updateChunkPos(Entity entityIn) {
        this.getProfiler().push("chunkCheck");
        int i = Mth.floor(entityIn.getX() / 16.0D);
        int j = Mth.floor(entityIn.getY() / 16.0D);
        int k = Mth.floor(entityIn.getZ() / 16.0D);
        if (!entityIn.inChunk || entityIn.xChunk != i || entityIn.yChunk != j || entityIn.zChunk != k) {
            if (entityIn.inChunk && this.hasChunk(entityIn.xChunk, entityIn.zChunk)) {
                this.getChunk(entityIn.xChunk, entityIn.zChunk).removeEntity(entityIn, entityIn.yChunk);
            }

            if (!entityIn.checkAndResetForcedChunkAdditionFlag() && !this.hasChunk(i, k)) {
                entityIn.inChunk = false;
            } else {
                this.getChunk(i, k).addEntity(entityIn);
            }
        }

        this.getProfiler().pop();
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