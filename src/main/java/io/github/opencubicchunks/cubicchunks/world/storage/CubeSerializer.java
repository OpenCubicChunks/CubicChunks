package io.github.opencubicchunks.cubicchunks.world.storage;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkSerializerAccess;
import io.github.opencubicchunks.cubicchunks.utils.ChunkIoMainThreadTaskUtils;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubicServerTickList;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ImposterProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.storage.AsyncSaveData;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.storage.PoiDeserializationContext;
import io.github.opencubicchunks.cubicchunks.world.lighting.CubicLevelLightEngine;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CubeSerializer {

    public static CubeAccess read(ServerLevel serverLevel, StructureManager structureManager, PoiManager poiManager, CubePos expectedCubePos, CompoundTag root) {
        CompoundTag level = root.getCompound("Level");

        CubePos cubePos = CubePos.of(level.getInt("xPos"), level.getInt("yPos"), level.getInt("zPos"));
        if (!Objects.equals(cubePos, expectedCubePos)) {
            CubicChunks.LOGGER.error("LevelCube file at {} is in the wrong location; relocating. (Expected {}, got {})", cubePos, expectedCubePos, cubePos);
        }

        int[] biomes1 = level.getIntArray("Biomes");
        CubeBiomeContainer cubeBiomeContainer = new CubeBiomeContainer(serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY),
            new CubeBoundsLevelHeightAccessor(LevelCube.DIAMETER_IN_BLOCKS, expectedCubePos.minCubeY(), ((CubicLevelHeightAccessor) serverLevel)), biomes1);
//            UpgradeData upgradedata = level.contains("UpgradeData", 10) ? new UpgradeData(level.getCompound("UpgradeData")) : UpgradeData.EMPTY;

        ImposterChunkPos imposterChunkPos = new ImposterChunkPos(cubePos);
        CubeProtoTickList<Block> blockProtoTickList = new CubeProtoTickList<>((block) -> {
            return block == null || block.defaultBlockState().isAir();
        }, imposterChunkPos, root.getList("ToBeTicked", 9), new CubeProtoTickList.CubeProtoTickListHeightAccess(imposterChunkPos.toCubePos(), (CubicLevelHeightAccessor) serverLevel));
        CubeProtoTickList<Fluid> fluidProtoTickList = new CubeProtoTickList<>((fluid) -> {
            return fluid == null || fluid == Fluids.EMPTY;
        }, imposterChunkPos, root.getList("LiquidsToBeTicked", 9), new CubeProtoTickList.CubeProtoTickListHeightAccess(imposterChunkPos.toCubePos(), (CubicLevelHeightAccessor) serverLevel));


        boolean isLightOn = level.getBoolean("isLightOn");
        ListTag sectionsNBTList = level.getList("Sections", 10);
        LevelChunkSection[] sections = new LevelChunkSection[CubeAccess.SECTION_COUNT];
        ChunkSource chunkSource = serverLevel.getChunkSource();
        LevelLightEngine lightEngine = chunkSource.getLightEngine();

        if (isLightOn) {
            ((CubicLevelLightEngine) lightEngine).retainData(cubePos, true);
        }

        for (int i = 0; i < sectionsNBTList.size(); ++i) {
            CompoundTag sectionNBT = sectionsNBTList.getCompound(i);
            int cubeIndex = sectionNBT.getShort("i");

            SectionPos sectionPos = Coords.sectionPosByIndex(cubePos, cubeIndex);
            if (sectionNBT.contains("Palette", 9) && sectionNBT.contains("BlockStates", 12)) {
                int sectionY = sectionPos.getY();
                LevelChunkSection section = new LevelChunkSection(sectionY);
                section.getStates().read(sectionNBT.getList("Palette", 10), sectionNBT.getLongArray("BlockStates"));
                section.recalcBlockCounts();
                if (!section.isEmpty()) {
                    sections[cubeIndex] = section;
                }
                ChunkIoMainThreadTaskUtils.executeMain(() -> ((PoiDeserializationContext) poiManager).checkConsistencyWithBlocksForCube(sectionPos, section));
            }

            if (isLightOn) {
                if (sectionNBT.contains("BlockLight", 7)) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, new DataLayer(sectionNBT.getByteArray("BlockLight")), true);
                }
                //TODO: reimplement
                if (serverLevel.dimensionType().hasSkyLight() && sectionNBT.contains("SkyLight", 7)) {
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, new DataLayer(sectionNBT.getByteArray("SkyLight")), true);
                }
            }
        }

        long inhabitedTime = level.getLong("InhabitedTime");
        ChunkStatus.ChunkType chunkType = getChunkStatus(root);
        CubeAccess cube;
        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            TickList<Block> blockTickList;
            if (level.contains("TileTicks", 9)) {
                blockTickList = ChunkTickList.create(level.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::get);
            } else {
                blockTickList = blockProtoTickList;
            }

            TickList<Fluid> fluidTickList;
            if (level.contains("LiquidTicks", 9)) {
                fluidTickList = ChunkTickList.create(level.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::get);
            } else {
                fluidTickList = fluidProtoTickList;
            }

            cube = new LevelCube(serverLevel.getLevel(), cubePos, cubeBiomeContainer, null, blockTickList, fluidTickList, inhabitedTime, sections,
                c -> readEntities(serverLevel, level, c));
            //TODO: reimplement forge capabilities in save format
//                if (level.contains("ForgeCaps")) ((LevelChunk)cube).readCapsFromNBT(level.getCompound("ForgeCaps"));
        } else {
            //TODO: updatedata
            ProtoCube protoCube = new ProtoCube(cubePos, null, sections, blockProtoTickList, fluidProtoTickList, serverLevel);
            protoCube.setCubeBiomeContainer(cubeBiomeContainer);

            cube = protoCube;
            protoCube.setInhabitedTime(inhabitedTime);
            protoCube.setCubeStatus(ChunkStatus.byName(level.getString("Status")));
            if (protoCube.getCubeStatus().isOrAfter(ChunkStatus.FEATURES)) {
                protoCube.setCubeLightEngine(lightEngine);
            }

            //if (!isLightOn && cubePrimer.getCubeStatus().isOrAfter(ChunkStatus.LIGHT)) {
            //    for (BlockPos blockpos : BlockPos.betweenClosed(cubePos.minCubeX(), cubePos.minCubeY(), cubePos.minCubeZ(), cubePos.maxCubeX(), cubePos.maxCubeY(), cubePos.maxCubeZ())) {
            //        if (cube.getBlockState(blockpos).getLightEmission() != 0) {
            //            //TODO: reimplement light positions in save format
            //                cubePrimer.addLightPosition(blockpos);
            //        }
            //    }
            //}
        }
        cube.setCubeLight(isLightOn);
        //TODO: reimplement heightmap in save format
//            CompoundTag compoundnbt3 = level.getCompound("Heightmaps");
//            EnumSet<Heightmap.Type> enumset = EnumSet.noneOf(Heightmap.Type.class);
//
//            for(Heightmap.Type heightmap$type : cube.getCubeStatus().getHeightMaps()) {
//                String s = heightmap$type.getId();
//                if (compoundnbt3.contains(s, 12)) {
//                    cube.setHeightmap(heightmap$type, compoundnbt3.getLongArray(s));
//                } else {
//                    enumset.add(heightmap$type);
//                }
//            }
//
//            Heightmap.updateChunkHeightmaps(cube, enumset);
        CompoundTag structures = level.getCompound("Structures");
        cube.setAllStarts(ChunkSerializerAccess.invokeUnpackStructureStart(serverLevel, structures, serverLevel.getSeed()));
        cube.setAllReferences(unpackCubeStructureReferences(new ImposterChunkPos(cubePos), structures));
        if (level.getBoolean("shouldSave")) {
            cube.setDirty(true);
        }

        ListTag postProcessingNBTList = level.getList("PostProcessing", 9);

        for (int l1 = 0; l1 < postProcessingNBTList.size(); ++l1) {
            ListTag listTag1 = postProcessingNBTList.getList(l1);

            for (int l = 0; l < listTag1.size(); ++l) {
                cube.addPackedPostProcess(listTag1.getShort(l), l1);
            }
        }

        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            //TODO: reimplement forge chunk load event
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.serverLevel.ChunkDataEvent.Load(cube, level, chunkstatus$type));
            return new ImposterProtoCube((LevelCube) cube, serverLevel);
        } else {
            ProtoCube protoCube = (ProtoCube) cube;
            ListTag entitiesNBT = level.getList("Entities", 10);

            for (int i2 = 0; i2 < entitiesNBT.size(); ++i2) {
                protoCube.addCubeEntity(entitiesNBT.getCompound(i2));
            }

            ListTag tileEntitiesNBTList = level.getList("TileEntities", 10);

            for (int i1 = 0; i1 < tileEntitiesNBTList.size(); ++i1) {
                CompoundTag tileEntityNBT = tileEntitiesNBTList.getCompound(i1);
                cube.setCubeBlockEntity(tileEntityNBT);
            }

            ListTag lightsNBTList = level.getList("Lights", 9);

            for (int j2 = 0; j2 < lightsNBTList.size(); ++j2) {
                ListTag lightList = lightsNBTList.getList(j2);

                for (int j1 = 0; j1 < lightList.size(); ++j1) {
                    protoCube.addCubeLight(lightList.getShort(j1), j2);
                }
            }

            CompoundTag compoundnbt5 = level.getCompound("CarvingMasks");
            for (String key : compoundnbt5.getAllKeys()) {
                GenerationStep.Carving carvingStage = GenerationStep.Carving.valueOf(key);
                protoCube.setCarvingMask(carvingStage, BitSet.valueOf(compoundnbt5.getByteArray(key)));
            }

            ListTag featurestates = level.getList("featurestates", CompoundTag.TAG_COMPOUND);
            featurestates.forEach((tag) -> {
                CompoundTag compoundTag = (CompoundTag) tag;
                protoCube.setFeatureBlocks(new BlockPos(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z")),
                    Block.BLOCK_STATE_REGISTRY.byId(compoundTag.getInt("s")));
            });

            //TODO: reimplement forge ChunkDataEvent
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.serverLevel.ChunkDataEvent.Load(cube, level, chunkstatus$type));

            return protoCube;
        }
    }

    public static CompoundTag write(ServerLevel serverLevel, CubeAccess cube, AsyncSaveData data) {
        CubePos pos = cube.getCubePos();

        CompoundTag root = new CompoundTag();
        CompoundTag level = new CompoundTag();
        root.put("Level", level);

        level.putInt("xPos", pos.getX());
        level.putInt("yPos", pos.getY());
        level.putInt("zPos", pos.getZ());

        level.putLong("LastUpdate", serverLevel.getGameTime());
        level.putLong("InhabitedTime", cube.getCubeInhabitedTime());
        level.putString("Status", cube.getCubeStatus().getName());

        LevelChunkSection[] sections = cube.getCubeSections();
        ListTag sectionsNBTList = new ListTag();
        LevelLightEngine lightEngine = serverLevel.getChunkSource().getLightEngine();
        boolean cubeHasLight = cube.hasCubeLight();

        for (int i = 0; i < CubeAccess.SECTION_COUNT; ++i) {
            LevelChunkSection section = sections[i];

            DataLayer blockData = data != null ? data.blockLight.get(Coords.sectionPosByIndex(pos, i)) :
                lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            DataLayer skyData = data != null ? data.skyLight.get(Coords.sectionPosByIndex(pos, i)) :
                lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            CompoundTag sectionNBT = new CompoundTag();
            if (section != LevelChunk.EMPTY_SECTION || blockData != null || skyData != null) {

                sectionNBT.putShort("i", (byte) (i));
                if (section != LevelChunk.EMPTY_SECTION) {
                    section.getStates().write(sectionNBT, "Palette", "BlockStates");
                }

                if (blockData != null && !blockData.isEmpty()) {
                    sectionNBT.putByteArray("BlockLight", blockData.getData());
                }

                if (skyData != null && !skyData.isEmpty()) {
                    sectionNBT.putByteArray("SkyLight", skyData.getData());
                }
            }
            sectionsNBTList.add(sectionNBT);
        }

        level.put("Sections", sectionsNBTList);

        if (cubeHasLight) {
            level.putBoolean("isLightOn", true);
        }

        ChunkBiomeContainer biomecontainer = cube.getBiomes();
        if (biomecontainer != null) {
            level.putIntArray("Biomes", biomecontainer.writeBiomes());
        }

        ListTag tileEntitiesNBTList = new ListTag();

        if (data != null) {
            data.blockEntities.forEach((blockPos, blockEntity) -> {
                CompoundTag tileEntityNBT = blockEntity.save(new CompoundTag());
                if (cube instanceof LevelCube) tileEntityNBT.putBoolean("keepPacked", false);
                tileEntitiesNBTList.add(tileEntityNBT);
            });
            data.blockEntitiesDeferred.forEach((blockPos, tag) -> {
                if (cube instanceof LevelCube) tag.putBoolean("keepPacked", true);
                tileEntitiesNBTList.add(tag);
            });
        } else {
            for (BlockPos blockpos : cube.getCubeBlockEntitiesPos()) {
                CompoundTag tileEntityNBT = cube.getCubeBlockEntityNbtForSaving(blockpos);
                if (tileEntityNBT != null) {
                    CubicChunks.LOGGER.debug("Saving tile entity at " + blockpos.toString());
                    tileEntitiesNBTList.add(tileEntityNBT);
                }
            }
        }

        level.put("TileEntities", tileEntitiesNBTList);
        if (cube.getCubeStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            ProtoCube protoCube = (ProtoCube) cube;
            ListTag listTag3 = new ListTag();
            listTag3.addAll(protoCube.getCubeEntities());
            level.put("Entities", listTag3);
//            level.put("Lights", packOffsets(cubePrimer.getPackedLights()));

            CompoundTag carvingMasksNBT = new CompoundTag();
            GenerationStep.Carving[] carvingSteps = GenerationStep.Carving.values();

            for (GenerationStep.Carving carving : carvingSteps) {
                BitSet bitSet = protoCube.getCarvingMask(carving);
                if (bitSet != null) {
                    carvingMasksNBT.putByteArray(carving.toString(), bitSet.toByteArray());
                }
            }

            level.put("CarvingMasks", carvingMasksNBT);

            ListTag featuresStates = new ListTag();
            protoCube.getFeaturesStateMap().forEach(((pos1, state) -> {
                CompoundTag tag = new CompoundTag();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                tag.putInt("s", Block.BLOCK_STATE_REGISTRY.getId(state));
                featuresStates.add(tag);
            }));
            level.put("featurestates", featuresStates);
        }

        TickList<Block> blockTicks = cube.getBlockTicks();
        if (blockTicks instanceof CubeProtoTickList) {
            level.put("ToBeTicked", ((ProtoTickList) blockTicks).save());
        } else if (blockTicks instanceof ChunkTickList) {
            level.put("TileTicks", ((ChunkTickList) blockTicks).save());
        } else if (data != null) {
            level.put("TileTicks", data.serverBlockTicks.get());
        } else {
            level.put("TileTicks", ((CubicServerTickList<?>) serverLevel.getBlockTicks()).save(cube.getCubePos()));
        }

        TickList<Fluid> liquidTicks = cube.getLiquidTicks();
        if (liquidTicks instanceof ProtoTickList) {
            level.put("LiquidsToBeTicked", ((ProtoTickList) liquidTicks).save());
        } else if (liquidTicks instanceof ChunkTickList) {
            level.put("LiquidTicks", ((ChunkTickList) liquidTicks).save());
        } else if (data != null) {
            level.put("LiquidTicks", data.serverLiquidTicks.get());
        } else {
            level.put("LiquidTicks", ((CubicServerTickList<?>) serverLevel.getLiquidTicks()).save(cube.getCubePos()));
        }

        level.put("PostProcessing", ChunkSerializer.packOffsets(cube.getPostProcessing()));
//        CompoundTag compoundnbt6 = new CompoundTag();
//
        //TODO: reimplement heightmaps
//        for(Map.Entry<Heightmap.Type, Heightmap> entry : cube.getHeightmaps()) {
//            if (cube.getCubeStatus().getHeightMaps().contains(entry.getKey())) {
//                compoundnbt6.put(entry.getKey().getId(), new LongArrayNBT(entry.getValue().getDataArray()));
//            }
//        }
//
//        level.put("Heightmaps", compoundnbt6);
        level.put("Structures", ChunkSerializerAccess.invokePackStructureData(serverLevel, new ImposterChunkPos(cube.getCubePos()), cube.getAllStarts(), cube.getAllReferences()));

        return root;
    }

    public static ChunkStatus.ChunkType getChunkStatus(@Nullable CompoundTag chunkNBT) {
        if (chunkNBT != null) {
            ChunkStatus chunkstatus = ChunkStatus.byName(chunkNBT.getCompound("Level").getString("Status"));
            if (chunkstatus != null) {
                return chunkstatus.getChunkType();
            }
        }
        return ChunkStatus.ChunkType.PROTOCHUNK;
    }

    private static void readEntities(ServerLevel serverLevel, CompoundTag compound, LevelCube cube) {
        if (compound.contains("Entities", 9)) {
            ListTag entitiesTag = compound.getList("Entities", 10);
            if (!entitiesTag.isEmpty()) {
                serverLevel.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(entitiesTag, serverLevel));
            }
        }

        ListTag blockEntitiesNbt = compound.getList("TileEntities", 10);
        for (int j = 0; j < blockEntitiesNbt.size(); ++j) {
            CompoundTag beNbt = blockEntitiesNbt.getCompound(j);
            boolean flag = beNbt.getBoolean("keepPacked");
            if (flag) {
                cube.setCubeBlockEntity(beNbt);
            } else {
                BlockPos blockpos = new BlockPos(beNbt.getInt("x"), beNbt.getInt("y"), beNbt.getInt("z"));
                BlockEntity blockEntity = BlockEntity.loadStatic(blockpos, cube.getBlockState(blockpos), beNbt);
                if (blockEntity != null) {
                    cube.setCubeBlockEntity(blockEntity);
                }
            }
        }
    }

    private static Map<StructureFeature<?>, LongSet> unpackCubeStructureReferences(ChunkPos pos, CompoundTag nbt) {
        Map<StructureFeature<?>, LongSet> map = Maps.newHashMap();
        CompoundTag compoundTag = nbt.getCompound("References");

        for (String nbtKey : compoundTag.getAllKeys()) {
            map.put(StructureFeature.STRUCTURES_REGISTRY.get(nbtKey.toLowerCase(Locale.ROOT)), new LongOpenHashSet(Arrays.stream(compoundTag.getLongArray(nbtKey)).filter((packedPos) -> {
                ChunkPos chunkPos2 = new ImposterChunkPos(CubePos.from(packedPos));
                if (chunkPos2.getChessboardDistance(pos) > 8) {
                    CubicChunks.LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", nbtKey, chunkPos2, pos);
                    return false;
                } else {
                    return true;
                }
            }).toArray()));
        }

        return map;
    }

    public static class CubeBoundsLevelHeightAccessor implements LevelHeightAccessor, CubicLevelHeightAccessor {

        private final int height;
        private final int minBuildHeight;
        private final WorldStyle worldStyle;
        private final boolean isCubic;
        private final boolean generates2DChunks;

        public CubeBoundsLevelHeightAccessor(int height, int minBuildHeight, CubicLevelHeightAccessor accessor) {
            this.height = height;
            this.minBuildHeight = minBuildHeight;
            this.worldStyle = accessor.worldStyle();
            this.isCubic = accessor.isCubic();
            this.generates2DChunks = accessor.generates2DChunks();
        }


        @Override public int getHeight() {
            return this.height;
        }

        @Override public int getMinBuildHeight() {
            return this.minBuildHeight;
        }

        @Override public WorldStyle worldStyle() {
            return worldStyle;
        }

        @Override public boolean isCubic() {
            return isCubic;
        }

        @Override public boolean generates2DChunks() {
            return generates2DChunks;
        }
    }
}