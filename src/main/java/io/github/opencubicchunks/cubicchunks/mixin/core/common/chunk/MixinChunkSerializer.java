package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.SerializableTickList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Mixin(ChunkSerializer.class)
public abstract class MixinChunkSerializer {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public static ChunkStatus.Type getChunkStatus(@Nullable CompoundNBT chunkNBT) {
        throw new Error("Mixin didn't apply");
    }

    /**
     * @author Barteks2x
     * @reason CubicChunks doesn't need much real data in columns
     */
    @Overwrite
    public static ChunkPrimer read(ServerWorld worldIn, TemplateManager templateManagerIn, PointOfInterestManager poiManager, ChunkPos pos, CompoundNBT compound) {
        CompoundNBT level = compound.getCompound("Level");
        ChunkPos loadedPos = new ChunkPos(level.getInt("xPos"), level.getInt("zPos"));
        if (!Objects.equals(pos, loadedPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, loadedPos);
        }
        long inhabitedTime = level.getLong("InhabitedTime");
        ChunkStatus.Type statusType = getChunkStatus(compound);
        IChunk newChunk;
        Biome[] biomesIn = new Biome[BiomeContainer.BIOMES_SIZE];
        Arrays.fill(biomesIn, WorldGenRegistries.field_243657_i.func_243576_d(Biomes.FOREST));
        //TODO: Double Check that this is proper
        BiomeContainer biomeContainerIn = new BiomeContainer(worldIn.func_241828_r().func_243612_b(Registry.BIOME_KEY), biomesIn);
        if (statusType == ChunkStatus.Type.LEVELCHUNK) {
            newChunk = new Chunk(worldIn.getWorld(), pos, biomeContainerIn, UpgradeData.EMPTY,
                    new SerializableTickList<>(Registry.BLOCK::getKey, new ArrayList<>(), 0), // TODO: supply game time
                    new SerializableTickList<>(Registry.FLUID::getKey, new ArrayList<>(), 0),
                    inhabitedTime, new ChunkSection[16], (chunk) -> { });
            if (level.contains("ForgeCaps")) ((Chunk)newChunk).readCapsFromNBT(level.getCompound("ForgeCaps"));
        } else {
            ChunkPrimer chunkprimer = new ChunkPrimer(pos, UpgradeData.EMPTY, new ChunkSection[16],
                    new ChunkPrimerTickList<>((block) -> block == null || block.getDefaultState().isAir(), pos),
                    new ChunkPrimerTickList<>((fluid) -> fluid == null || fluid == Fluids.EMPTY, pos));

            chunkprimer.setBiomes(biomeContainerIn); // setBiomes
            newChunk = chunkprimer;
            chunkprimer.setInhabitedTime(inhabitedTime);
            chunkprimer.setStatus(ChunkStatus.byName(level.getString("Status")));
            if (chunkprimer.getStatus().isAtLeast(ChunkStatus.FEATURES)) {
                chunkprimer.setLightManager(worldIn.getChunkProvider().getLightManager());
            }
        }
        newChunk.setLight(true);
        CompoundNBT heightmaps = level.getCompound("Heightmaps");

        for(Heightmap.Type heightmapType : newChunk.getStatus().getHeightMaps()) {
            String s = heightmapType.getId();
            if (heightmaps.contains(s, Constants.NBT.TAG_LONG_ARRAY)) {
                newChunk.setHeightmap(heightmapType, heightmaps.getLongArray(s));
            }
        }
        if (level.getBoolean("shouldSave")) {
            newChunk.setModified(true);
        }
        if (statusType == ChunkStatus.Type.LEVELCHUNK) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(newChunk, level, statusType));
            return new ChunkPrimerWrapper((Chunk)newChunk);
        } else {
            ChunkPrimer primer = (ChunkPrimer)newChunk;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(newChunk, level, statusType));
            return primer;
        }
    }

    /**
     * @author Barteks2x
     * @reason Save only columns
     */
    @Overwrite
    public static CompoundNBT write(ServerWorld worldIn, IChunk chunkIn) {
        ChunkPos chunkpos = chunkIn.getPos();
        CompoundNBT compoundnbt = new CompoundNBT();
        CompoundNBT level = new CompoundNBT();
        compoundnbt.putInt("DataVersion", SharedConstants.getVersion().getWorldVersion());
        compoundnbt.put("Level", level);
        level.putInt("xPos", chunkpos.x);
        level.putInt("zPos", chunkpos.z);
        level.putLong("InhabitedTime", chunkIn.getInhabitedTime());
        level.putString("Status", chunkIn.getStatus().getName());

        if (chunkIn.getStatus().getType() == ChunkStatus.Type.LEVELCHUNK) {
            Chunk chunk = (Chunk)chunkIn;
            chunk.setHasEntities(false);
            try {
                final CompoundNBT capTag = chunk.writeCapsToNBT();
                if (capTag != null) level.put("ForgeCaps", capTag);
            } catch (Exception exception) {
                LogManager.getLogger().error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
            }
        }

        CompoundNBT heightmaps = new CompoundNBT();
        for(Map.Entry<Heightmap.Type, Heightmap> entry : chunkIn.getHeightmaps()) {
            if (chunkIn.getStatus().getHeightMaps().contains(entry.getKey())) {
                heightmaps.put(entry.getKey().getId(), new LongArrayNBT(entry.getValue().getDataArray()));
            }
        }
        level.put("Heightmaps", heightmaps);
        return compoundnbt;
    }
}
