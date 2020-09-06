package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements IServerWorld {

    @Shadow private boolean tickingEntities;

    @Shadow @Final private Int2ObjectMap<Entity> entitiesById;

    protected MixinServerWorld(ISpawnWorldInfo p_i231617_1_, RegistryKey<World> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<IProfiler> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    @Shadow @Deprecated public abstract void onEntityRemoved(Entity entityIn);


    @Override
    public void onCubeUnloading(BigCube cubeIn) {
        this.blockEntitiesToUnload.addAll(cubeIn.getTileEntityMap().values());
        ClassInheritanceMultiMap<Entity>[] aclassinheritancemultimap = cubeIn.getEntityLists();
        int i = aclassinheritancemultimap.length;

        for(int j = 0; j < i; ++j) {
            for(Entity entity : aclassinheritancemultimap[j]) {
                if (!(entity instanceof ServerPlayerEntity)) {
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