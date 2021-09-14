package io.github.opencubicchunks.cubicchunks.chunk.storage;

import java.util.concurrent.CompletableFuture;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.ChunkEntities;

public interface CubicEntityStorage {

    CompletableFuture<ChunkEntities<Entity>> loadCubeEntities(CubePos pos);
}
