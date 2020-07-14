package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiFunction;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements IServerWorld {

    @Shadow private boolean tickingEntities;

    @Shadow @Final private Int2ObjectMap<Entity> entitiesById;

    @Shadow @Deprecated public abstract void onEntityRemoved(Entity entityIn);

    protected MixinServerWorld(WorldInfo info, DimensionType dimType,
            BiFunction<World, Dimension, AbstractChunkProvider> provider,
            IProfiler profilerIn, boolean remote) {
        super(info, dimType, provider, profilerIn, remote);
    }

    @Override
    public void onCubeUnloading(BigCube cubeIn) {
        this.tileEntitiesToBeRemoved.addAll(cubeIn.getTileEntityMap().values());
        ClassInheritanceMultiMap<Entity>[] aclassinheritancemultimap = cubeIn.getEntityLists();
        int i = aclassinheritancemultimap.length;

        for(int j = 0; j < i; ++j) {
            for(Entity entity : aclassinheritancemultimap[j]) {
                if (!(entity instanceof ServerPlayerEntity)) {
                    if (this.tickingEntities) {
                        throw (IllegalStateException) Util.pauseDevMode(new IllegalStateException("Removing entity while ticking!"));
                    }

                    this.entitiesById.remove(entity.getEntityId());
                    this.onEntityRemoved(entity);
                }
            }
        }
    }
}
