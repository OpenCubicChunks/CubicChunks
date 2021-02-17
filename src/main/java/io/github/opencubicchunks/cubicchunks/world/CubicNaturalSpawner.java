package io.github.opencubicchunks.cubicchunks.world;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.function.Consumer;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.NaturalSpawnerAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.SpawnStateAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.NearestNeighborBiomeZoomer;
import net.minecraft.world.level.chunk.ChunkAccess;

@SuppressWarnings("JavaReflectionMemberAccess")
public class CubicNaturalSpawner {

    public static final int SPAWN_RADIUS = (int) (16 * (Math.sqrt((NaturalSpawnerAccess.getMagicNumber())) - 1) / 2);

    private static final Method SPAWN_FOR_CUBE;

    public static void spawnForCube(ServerLevel world, ChunkAccess chunk, NaturalSpawner.SpawnState info, boolean spawnAnimals, boolean spawnMonsters, boolean shouldSpawnAnimals) {
        try {
            SPAWN_FOR_CUBE.invoke(null, world, chunk, info, spawnAnimals, spawnMonsters, shouldSpawnAnimals);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    public static NaturalSpawner.SpawnState createState(int spawningChunkCount, Iterable<Entity> entities, ChunkGetter chunkAccessSource) {
        PotentialCalculator potentialCalculator = new PotentialCalculator();
        Object2IntOpenHashMap<MobCategory> object2IntOpenHashMap = new Object2IntOpenHashMap();
        Iterator var5 = entities.iterator();

        while (true) {
            Entity entity;
            Mob mob;
            do {
                if (!var5.hasNext()) {
                    return SpawnStateAccess.createNew(spawningChunkCount, object2IntOpenHashMap, potentialCalculator);
                }

                entity = (Entity) var5.next();
                if (!(entity instanceof Mob)) {
                    break;
                }

                mob = (Mob) entity;
            } while (mob.isPersistenceRequired() || mob.requiresCustomPersistence());

            MobCategory mobCategory = entity.getType().getCategory();
            if (mobCategory != MobCategory.MISC) {
                BlockPos blockPos = entity.blockPosition();
                long cubePosLong = CubePos.asLong(Coords.blockToCube(blockPos.getX()), Coords.blockToCube(blockPos.getY()), Coords.blockToCube(blockPos.getZ()));

                Entity entityFinal = entity;
                chunkAccessSource.query(cubePosLong, (levelChunk) -> {
                    Biome roughBiome = getRoughBiomeForCube(blockPos, levelChunk);
                    MobSpawnSettings.MobSpawnCost mobSpawnCost = roughBiome.getMobSettings().getMobSpawnCost(entityFinal.getType());
                    if (mobSpawnCost != null) {
                        potentialCalculator.addCharge(entityFinal.blockPosition(), mobSpawnCost.getCharge());
                    }

                    object2IntOpenHashMap.addTo(mobCategory, 1);
                });
            }
        }
    }

    public static Biome getRoughBiomeForCube(BlockPos pos, ChunkAccess chunk) {
        return NearestNeighborBiomeZoomer.INSTANCE.getBiome(0L, pos.getX(), pos.getY(), pos.getZ(), ((BigCube) chunk).getCubeBiomes());
    }

    static {
        try {
            SPAWN_FOR_CUBE =
                NaturalSpawner.class.getMethod("spawnForCube", ServerLevel.class, ChunkAccess.class, NaturalSpawner.SpawnState.class, boolean.class, boolean.class, boolean.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("ASM did not apply", e);
        }
    }

    @FunctionalInterface
    public interface ChunkGetter {
        void query(long pos, Consumer<ChunkAccess> chunkConsumer);
    }
}
