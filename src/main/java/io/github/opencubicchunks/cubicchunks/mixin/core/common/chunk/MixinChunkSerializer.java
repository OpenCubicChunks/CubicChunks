package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ColumnBiomeContainer;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkSerializer.class, priority = 900)
public abstract class MixinChunkSerializer {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag chunkNBT) {
        throw new Error("Mixin didn't apply");
    }

    /**
     * @author Barteks2x
     * @reason CubicChunks doesn't need much real data in columns
     */
    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private static void read(ServerLevel serverLevel, StructureManager structureManager, PoiManager poiManager, ChunkPos pos, CompoundTag compound, CallbackInfoReturnable<ProtoChunk> cir) {
        if (!((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            return;
        }
        cir.cancel();

        CompoundTag level = compound.getCompound("Level");
        ChunkPos loadedPos = new ChunkPos(level.getInt("xPos"), level.getInt("zPos"));
        if (!Objects.equals(pos, loadedPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, loadedPos);
        }
        long inhabitedTime = level.getLong("InhabitedTime");
        ChunkStatus.ChunkType statusType = getChunkTypeFromTag(compound);
        ChunkAccess newChunk;
        //TODO: Double Check that this is proper
        ColumnBiomeContainer biomeContainerIn = new ColumnBiomeContainer(serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), serverLevel, serverLevel);
        if (statusType == ChunkStatus.ChunkType.LEVELCHUNK) {
            newChunk = new LevelChunk(serverLevel.getLevel(), pos, biomeContainerIn, UpgradeData.EMPTY,
                new ChunkTickList<>(Registry.BLOCK::getKey, new ArrayList<>(), 0), // TODO: supply game time
                new ChunkTickList<>(Registry.FLUID::getKey, new ArrayList<>(), 0),
                inhabitedTime, new LevelChunkSection[16], (chunk) -> {});
        } else {
            ProtoChunk chunkprimer = new ProtoChunk(pos, UpgradeData.EMPTY, new LevelChunkSection[16],
                new ProtoTickList<>((block) -> block == null || block.defaultBlockState().isAir(), pos, serverLevel),
                new ProtoTickList<>((fluid) -> fluid == null || fluid == Fluids.EMPTY, pos, serverLevel), serverLevel);

            chunkprimer.setBiomes(biomeContainerIn); // setBiomes
            newChunk = chunkprimer;
            chunkprimer.setInhabitedTime(inhabitedTime);
            chunkprimer.setStatus(ChunkStatus.byName(level.getString("Status")));
            if (chunkprimer.getStatus().isOrAfter(ChunkStatus.FEATURES)) {
                chunkprimer.setLightEngine(serverLevel.getChunkSource().getLightEngine());
            }
        }
        newChunk.setLightCorrect(true);
//        CompoundTag heightmaps = level.getCompound("Heightmaps");
//
//        for(Heightmap.Types heightmapType : newChunk.getStatus().heightmapsAfter()) {
//            String s = heightmapType.getSerializationKey();
//            if (heightmaps.contains(s, 12)) { // nbt long array
//                newChunk.setHeightmap(heightmapType, heightmaps.getLongArray(s));
//            }
//        }
        // for ChunkSerializerMixin from fabric-structure-api-v1.mixins to target
        newChunk.setAllReferences(new HashMap<>());
        if (level.getBoolean("shouldSave")) {
            newChunk.setUnsaved(true);
        }
        if (statusType == ChunkStatus.ChunkType.LEVELCHUNK) {
            cir.setReturnValue(new ImposterProtoChunk((LevelChunk) newChunk));
        } else {
            ProtoChunk primer = (ProtoChunk) newChunk;
            cir.setReturnValue(primer);
        }
    }

    /**
     * @author Barteks2x
     * @reason Save only columns
     */
    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private static void write(ServerLevel serverLevel, ChunkAccess column, CallbackInfoReturnable<CompoundTag> cir) {
        if (!((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            return;
        }
        cir.cancel();

        ChunkPos chunkpos = column.getPos();
        CompoundTag compoundnbt = new CompoundTag();
        CompoundTag level = new CompoundTag();
        compoundnbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        compoundnbt.put("Level", level);
        level.putInt("xPos", chunkpos.x);
        level.putInt("zPos", chunkpos.z);
        level.putLong("InhabitedTime", column.getInhabitedTime());
        level.putString("Status", column.getStatus().getName());

//        CompoundTag heightmaps = new CompoundTag();
//        for(Map.Entry<Heightmap.Types, Heightmap> entry : chunkIn.getHeightmaps()) {
//            if (chunkIn.getStatus().heightmapsAfter().contains(entry.getKey())) {
//                heightmaps.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
//            }
//        }
//        level.put("Heightmaps", heightmaps);
        cir.setReturnValue(compoundnbt);
    }
}