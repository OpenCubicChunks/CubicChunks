package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld extends Level implements IServerWorld {

    @Shadow private boolean tickingEntities;

    @Shadow @Final private Int2ObjectMap<Entity> entitiesById;

    protected MixinServerWorld(WritableLevelData p_i231617_1_, ResourceKey<Level> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<ProfilerFiller> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    @Shadow @Deprecated public abstract void onEntityRemoved(Entity entityIn);


    @Override
    public void onCubeUnloading(BigCube cubeIn) {
        this.blockEntitiesToUnload.addAll(cubeIn.getTileEntityMap().values());
        ClassInstanceMultiMap<Entity>[] aclassinheritancemultimap = cubeIn.getEntityLists();
        int i = aclassinheritancemultimap.length;

        for(int j = 0; j < i; ++j) {
            for(Entity entity : aclassinheritancemultimap[j]) {
                if (!(entity instanceof ServerPlayer)) {
                    if (this.tickingEntities) {
                        throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("Removing entity while ticking!"));
                    }

                    this.entitiesById.remove(entity.getId());
                    this.onEntityRemoved(entity);
                }
            }
        }
    }
}