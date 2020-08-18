package io.github.opencubicchunks.cubicchunks.mixin.debug;

import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldEntitySpawner.class)
public class MixinDebug {

    /**
     * @author Cyclonit
     */
    @Overwrite
    public static void spawnEntitiesInChunk(EntityClassification p_226701_0_, ServerWorld p_226701_1_, Chunk p_226701_2_, BlockPos p_226701_3_) {
        // TODO: This is enabled as it causes world crashes using the new heightmap. Needs to be fixed.
    }

}
