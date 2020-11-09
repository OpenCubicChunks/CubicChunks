package io.github.opencubicchunks.cubicchunks.world.storage;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class CubeSerializer {

    public static IBigCube read(ServerLevel worldIn, StructureManager structureManager, PoiManager poiManager, CubePos expectedCubePos, CompoundTag root) {
//        Path cubePath = worldDir.resolve("cubes32/" + cubePos.getX() + "_" + cubePos.getY() + "_" + cubePos.getZ() + ".bin");
//        if (!Files.exists(cubePath)) {
//            return null;
//        }
//        InputStream input = new FileInputStream(cubePath.toString());
//        DataInputStream datainputstream = new DataInputStream(input);
//
        IBigCube cube = null;
        CompoundTag level = root.getCompound("Level");

        CubePos cubePos = CubePos.of(level.getInt("xPos"), level.getInt("yPos"), level.getInt("zPos"));
        if (!Objects.equals(cubePos, expectedCubePos)) {
            CubicChunks.LOGGER.error("LevelChunk file at {} is in the wrong location; relocating. (Expected {}, got {})", cubePos, cubePos, cubePos);
        }

        ChunkGenerator chunkgenerator = worldIn.getChunkSource().getGenerator();
        BiomeSource biomeprovider = chunkgenerator.getBiomeSource();

        CubeBiomeContainer biomecontainer = new CubeBiomeContainer(worldIn.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), cubePos.asSectionPos(), biomeprovider, level.contains("Biomes", 11) ? level.getIntArray("Biomes") : null);
//            UpgradeData upgradedata = level.contains("UpgradeData", 10) ? new UpgradeData(level.getCompound("UpgradeData")) : UpgradeData.EMPTY;
//            ChunkPrimerTickList<Block> chunkprimerticklist = new ChunkPrimerTickList<>((p_222652_0_) -> {
//                return p_222652_0_ == null || p_222652_0_.getDefaultState().isAir();
//            }, cubePos, level.getList("ToBeTicked", 9));
//            ChunkPrimerTickList<Fluid> chunkprimerticklist1 = new ChunkPrimerTickList<>((p_222646_0_) -> {
//                return p_222646_0_ == null || p_222646_0_ == Fluids.EMPTY;
//            }, cubePos, level.getList("LiquidsToBeTicked", 9));
        boolean isLightOn = level.getBoolean("isLightOn");
        ListTag sectionsNBTList = level.getList("Sections", 10);
        LevelChunkSection[] sections = new LevelChunkSection[IBigCube.SECTION_COUNT];
        //TODO: 1.16 dimensions stuff
//            boolean worldHasSkylight = worldIn.getDimension().hasSkyLight();
        ChunkSource abstractchunkprovider = worldIn.getChunkSource();
        LevelLightEngine worldlightmanager = abstractchunkprovider.getLightEngine();
//            if (isLightOn) {
//                worldlightmanager.retainData(cubePos, true);
//            }

        for(int i = 0; i < sectionsNBTList.size(); ++i) {
            CompoundTag sectionNBT = sectionsNBTList.getCompound(i);
            int cubeIndex = sectionNBT.getShort("i");

            if (sectionNBT.contains("Palette", 9) && sectionNBT.contains("BlockStates", 12)) {
                int sectionY = Coords.sectionPosByIndex(cubePos, cubeIndex).getY();
                LevelChunkSection chunksection = new LevelChunkSection(sectionY);
                chunksection.getStates().read(sectionNBT.getList("Palette", 10), sectionNBT.getLongArray("BlockStates"));
                chunksection.recalcBlockCounts();
                if (!chunksection.isEmpty()) {
                    sections[cubeIndex] = chunksection;
                }

                //TODO: reimplement poi in save format
                //poiManager.checkConsistencyWithBlocks(cubePos, chunksection);
            }

            if (isLightOn) {
                if (sectionNBT.contains("BlockLight", 7)) {
                    worldlightmanager.queueSectionData(LightLayer.BLOCK, Coords.sectionPosByIndex(cubePos, cubeIndex), new DataLayer(sectionNBT.getByteArray("BlockLight")), true);
                }

                //TODO: reimplement
                if (/*worldHasSkylight &&*/ sectionNBT.contains("SkyLight", 7)) {
                    worldlightmanager.queueSectionData(LightLayer.SKY, Coords.sectionPosByIndex(cubePos, cubeIndex), new DataLayer(sectionNBT.getByteArray("SkyLight")), true);
                }
            }
        }

        long inhabitedTime = level.getLong("InhabitedTime");
        ChunkStatus.ChunkType chunkstatus$type = getChunkStatus(root);
        IBigCube icube;
        if (chunkstatus$type == ChunkStatus.ChunkType.LEVELCHUNK) {
//                ITickList<Block> iticklist;
//                if (level.contains("TileTicks", 9)) {
//                    iticklist = SerializableTickList.create(level.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::getOrDefault);
//                } else {
//                    iticklist = chunkprimerticklist;
//                }
//
//                ITickList<Fluid> iticklist1;
//                if (level.contains("LiquidTicks", 9)) {
//                    iticklist1 = SerializableTickList.create(level.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::getOrDefault);
//                } else {
//                    iticklist1 = chunkprimerticklist1;
//                }

//                icube = new BigCube(worldIn.getWorld(), cubePos, biomecontainer, upgradedata, iticklist, iticklist1, inhabitedTime, sections, (p_222648_1_) -> {
//                    readEntities(level, p_222648_1_);
//                });
            icube = new BigCube(worldIn.getLevel(), cubePos, biomecontainer, null, null, null, inhabitedTime, sections, (p_222648_1_) -> {
                readEntities(worldIn, level, p_222648_1_);
            });
            //TODO: reimplement forge capabilities in save format
//                if (level.contains("ForgeCaps")) ((LevelChunk)icube).readCapsFromNBT(level.getCompound("ForgeCaps"));
        } else {
//                CubePrimer cubePrimer = new CubePrimer(cubePos, upgradedata, sections, chunkprimerticklist, chunkprimerticklist1);
            CubePrimer cubePrimer = new CubePrimer(cubePos, null, sections, null, null, worldIn);
            cubePrimer.setCubeBiomes(biomecontainer);
            icube = cubePrimer;
            cubePrimer.setInhabitedTime(inhabitedTime);
            cubePrimer.setCubeStatus(ChunkStatus.byName(level.getString("Status")));
            if (cubePrimer.getCubeStatus().isOrAfter(ChunkStatus.FEATURES)) {
                cubePrimer.setCubeLightManager(worldlightmanager);
            }

            if (!isLightOn && cubePrimer.getCubeStatus().isOrAfter(ChunkStatus.LIGHT)) {
                for(BlockPos blockpos : BlockPos.betweenClosed(cubePos.minCubeX(), cubePos.minCubeY(), cubePos.minCubeZ(), cubePos.maxCubeX(), cubePos.maxCubeY(), cubePos.maxCubeZ())) {
                    if (icube.getBlockState(blockpos).getLightEmission() != 0) {
                        //TODO: reimplement light positions in save format
//                            cubePrimer.addLightPosition(blockpos);
                    }
                }
            }
        }
        icube.setCubeLight(isLightOn);
        //TODO: reimplement heightmap in save format
//            CompoundTag compoundnbt3 = level.getCompound("Heightmaps");
//            EnumSet<Heightmap.Type> enumset = EnumSet.noneOf(Heightmap.Type.class);
//
//            for(Heightmap.Type heightmap$type : icube.getCubeStatus().getHeightMaps()) {
//                String s = heightmap$type.getId();
//                if (compoundnbt3.contains(s, 12)) {
//                    icube.setHeightmap(heightmap$type, compoundnbt3.getLongArray(s));
//                } else {
//                    enumset.add(heightmap$type);
//                }
//            }
//
//            Heightmap.updateChunkHeightmaps(icube, enumset);
//            CompoundTag structures = level.getCompound("Structures");
//            icube.setStructureStarts(unpackStructureStart(chunkgenerator, templateManagerIn, structures));
//            icube.setStructureReferences(unpackStructureReferences(cubePos, structures));
        if (level.getBoolean("shouldSave")) {
            icube.setDirty(true);
        }

        //TODO: reimplement post processing in save format
//            ListTag postProcessingNBTList = level.getList("PostProcessing", 9);
//
//            for(int l1 = 0; l1 < postProcessingNBTList.size(); ++l1) {
//                ListTag ListTag1 = postProcessingNBTList.getList(l1);
//
//                for(int l = 0; l < ListTag1.size(); ++l) {
//                    icube.addPackedPostProcess(ListTag1.getShort(l), l1);
//                }
//            }

        if (chunkstatus$type == ChunkStatus.ChunkType.LEVELCHUNK) {
            //TODO: reimplement forge chunk load event
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(icube, level, chunkstatus$type));
            return new CubePrimerWrapper((BigCube) icube, worldIn);
        } else {
            CubePrimer cubePrimer = (CubePrimer)icube;
            ListTag entitiesNBT = level.getList("Entities", 10);

            for(int i2 = 0; i2 < entitiesNBT.size(); ++i2) {
                cubePrimer.addCubeEntity(entitiesNBT.getCompound(i2));
            }

            ListTag tileEntitiesNBTList = level.getList("TileEntities", 10);

            for(int i1 = 0; i1 < tileEntitiesNBTList.size(); ++i1) {
                CompoundTag tileEntityNBT = tileEntitiesNBTList.getCompound(i1);
                icube.setCubeBlockEntity(tileEntityNBT);
            }

            ListTag lightsNBTList = level.getList("Lights", 9);

            for(int j2 = 0; j2 < lightsNBTList.size(); ++j2) {
                ListTag ListTag2 = lightsNBTList.getList(j2);

                for(int j1 = 0; j1 < ListTag2.size(); ++j1) {
                    cubePrimer.addCubeLightValue(ListTag2.getShort(j1), j2);
                }
            }

//                CompoundTag compoundnbt5 = level.getCompound("CarvingMasks");
//                for(String s1 : compoundnbt5.keySet()) {
//                    GenerationStage.Carving generationstage$carving = GenerationStage.Carving.valueOf(s1);
//                    cubePrimer.setCubeCarvingMask(generationstage$carving, BitSet.valueOf(compoundnbt5.getByteArray(s1)));
//                }

            //TODO: reimplement forge ChunkDataEvent
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(icube, level, chunkstatus$type));

            return cubePrimer;
        }
    }

    public static CompoundTag write(ServerLevel worldIn, IBigCube icube) {
        CubePos pos = icube.getCubePos();

        CompoundTag root = new CompoundTag();
        CompoundTag level = new CompoundTag();
        root.put("Level", level);

        level.putInt("xPos", pos.getX());
        level.putInt("yPos", pos.getY());
        level.putInt("zPos", pos.getZ());

        level.putLong("LastUpdate", worldIn.getGameTime());
        level.putLong("InhabitedTime", icube.getCubeInhabitedTime());
        level.putString("Status", icube.getCubeStatus().getName());

        LevelChunkSection[] sections = icube.getCubeSections();
        ListTag sectionsNBTList = new ListTag();
        LevelLightEngine worldlightmanager = worldIn.getChunkSource().getLightEngine();
        boolean cubeHasLight = icube.hasCubeLight();

        for(int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
            LevelChunkSection section = sections[i];

            DataLayer blockData = worldlightmanager.getLayerListener(LightLayer.BLOCK).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            DataLayer skyData = worldlightmanager.getLayerListener(LightLayer.SKY).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            CompoundTag sectionNBT = new CompoundTag();
            if (section != LevelChunk.EMPTY_SECTION || blockData != null || skyData != null) {

                sectionNBT.putShort("i", (byte)(i));
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

        ChunkBiomeContainer biomecontainer = icube.getCubeBiomes();
        if (biomecontainer != null) {
            level.putIntArray("Biomes", biomecontainer.writeBiomes());
        }

        ListTag tileEntitiesNBTList = new ListTag();

        for(BlockPos blockpos : icube.getCubeTileEntitiesPos()) {
            CompoundTag tileEntityNBT = icube.getCubeBlockEntityNbtForSaving(blockpos);
            if (tileEntityNBT != null) {
            CubicChunks.LOGGER.debug("Saving tile entity at " + blockpos.toString());
            tileEntitiesNBTList.add(tileEntityNBT);
            }
        }

        level.put("TileEntities", tileEntitiesNBTList);
        if (icube.getCubeStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            CubePrimer cubePrimer = (CubePrimer)icube;
            ListTag listTag3 = new ListTag();
            listTag3.addAll(cubePrimer.getCubeEntities());
            level.put("Entities", listTag3);
//            level.put("Lights", packOffsets(cubePrimer.getPackedLights()));

            CompoundTag carvingMasksNBT = new CompoundTag();
            GenerationStep.Carving[] carvingSteps = GenerationStep.Carving.values();

            for (GenerationStep.Carving carving : carvingSteps) {
                BitSet bitSet = cubePrimer.getCarvingMask(carving);
                if (bitSet != null) {
                    carvingMasksNBT.putByteArray(carving.toString(), bitSet.toByteArray());
                }
            }

            level.put("CarvingMasks", carvingMasksNBT);
        }

        //TODO: implement missing cube methods and save format
//        ITickList<Block> iticklist = icube.getBlocksToBeTicked();
//        if (iticklist instanceof ChunkPrimerTickList) {
//            level.put("ToBeTicked", ((ChunkPrimerTickList)iticklist).write());
//        } else if (iticklist instanceof SerializableTickList) {
//            level.put("TileTicks", ((SerializableTickList)iticklist).save(worldIn.getGameTime()));
//        } else {
//            level.put("TileTicks", worldIn.getPendingBlockTicks().save(chunkpos));
//        }
//
//        ITickList<Fluid> iticklist1 = icube.getFluidsToBeTicked();
//        if (iticklist1 instanceof ChunkPrimerTickList) {
//            level.put("LiquidsToBeTicked", ((ChunkPrimerTickList)iticklist1).write());
//        } else if (iticklist1 instanceof SerializableTickList) {
//            level.put("LiquidTicks", ((SerializableTickList)iticklist1).save(worldIn.getGameTime()));
//        } else {
//            level.put("LiquidTicks", worldIn.getPendingFluidTicks().save(chunkpos));
//        }

//        level.put("PostProcessing", toNbt(icube.getPackedPositions()));
//        CompoundTag compoundnbt6 = new CompoundTag();
//
//        for(Map.Entry<Heightmap.Type, Heightmap> entry : icube.getHeightmaps()) {
//            if (icube.getCubeStatus().getHeightMaps().contains(entry.getKey())) {
//                compoundnbt6.put(entry.getKey().getId(), new LongArrayNBT(entry.getValue().getDataArray()));
//            }
//        }
//
//        level.put("Heightmaps", compoundnbt6);
//        level.put("Structures", writeStructures(chunkpos, icube.getStructureStarts(), icube.getStructureReferences()));

        return root;
    }

    public static void writeCube(Level worldIn, IBigCube icube, Path worldDir) throws IOException {
        CubePos pos = icube.getCubePos();
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath.getParent())) {
            Files.createDirectories(cubePath.getParent());
        }
        CompoundTag z = new CompoundTag();
        CompoundTag root = new CompoundTag();
        z.put("", root);
        CompoundTag level = new CompoundTag();
        root.put("Level", level);

        level.putInt("xPos", pos.getX());
        level.putInt("yPos", pos.getY());
        level.putInt("zPos", pos.getZ());

        level.putLong("LastUpdate", worldIn.getGameTime());
        level.putLong("InhabitedTime", icube.getCubeInhabitedTime());
        level.putString("Status", icube.getCubeStatus().getName());

        LevelChunkSection[] sections = icube.getCubeSections();
        ListTag sectionsNBTList = new ListTag();
        LevelLightEngine worldlightmanager = worldIn.getChunkSource().getLightEngine();
        boolean cubeHasLight = icube.hasCubeLight();

        for(int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
            LevelChunkSection section = sections[i];

            DataLayer blockData = worldlightmanager.getLayerListener(LightLayer.BLOCK).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            DataLayer skyData = worldlightmanager.getLayerListener(LightLayer.SKY).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            CompoundTag sectionNBT = new CompoundTag();
            if (section != LevelChunk.EMPTY_SECTION || blockData != null || skyData != null) {

                sectionNBT.putShort("i", (byte)(i));
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

        ChunkBiomeContainer biomecontainer = icube.getCubeBiomes();
        if (biomecontainer != null) {
            level.putIntArray("Biomes", biomecontainer.writeBiomes());
        }

        ListTag tileEntitiesNBTList = new ListTag();

        for(BlockPos blockpos : icube.getCubeTileEntitiesPos()) {
            CompoundTag tileEntityNBT = icube.getCubeBlockEntityNbtForSaving(blockpos);
//            if (tileEntityNBT != null) {
                CubicChunks.LOGGER.debug("Saving tile entity at " + blockpos.toString());
                tileEntitiesNBTList.add(tileEntityNBT);
//            }
        }

        level.put("TileEntities", tileEntitiesNBTList);
        ListTag entitiesNBTList = new ListTag();
        if (icube.getCubeStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {//icube.getCubeStatus().getType() == ChunkStatus.ChunkType.LEVELCHUNK) {
            BigCube cube = (BigCube)icube;
//            cube.setHasEntities(false);

            for(int k = 0; k < cube.getCubeEntityLists().length; ++k) {
                for(Entity entity : cube.getCubeEntityLists()[k]) {
                    CompoundTag entityNBT = new CompoundTag();
                    try {
                        if (entity.save(entityNBT)) {
//                            cube.setHasEntities(true);
                            entitiesNBTList.add(entityNBT);
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().error("An Entity type {} has thrown an exception trying to write state. It will not persist. Report this to the mod author", entity.getType(), e);
                    }
                }
            }
            //TODO: reimplement forge capabilities
//            try {
//                final CompoundTag capTag = cube.writeCapsToNBT();
//                if (capTag != null) level.put("ForgeCaps", capTag);
//            } catch (Exception exception) {
//                LogManager.getLogger().error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
//            }
        } else {
            CubePrimer cubePrimer = (CubePrimer)icube;
            entitiesNBTList.addAll(cubePrimer.getCubeEntities());
            //level.put("Lights", toNbt(cubePrimer.getCubePackedLightPositions()));
            CompoundTag carvingMasksNBT = new CompoundTag();

            //TODO: reimplement carving masks
//            for(GenerationStage.Carving generationstage$carving : GenerationStage.Carving.values()) {
//                carvingMasksNBT.putByteArray(generationstage$carving.toString(), icube.getCarvingMask(generationstage$carving).toByteArray());
//            }

            level.put("CarvingMasks", carvingMasksNBT);
        }
        level.put("Entities", entitiesNBTList);

        //TODO: implement missing cube methods and save format
//        ITickList<Block> iticklist = icube.getBlocksToBeTicked();
//        if (iticklist instanceof ChunkPrimerTickList) {
//            level.put("ToBeTicked", ((ChunkPrimerTickList)iticklist).write());
//        } else if (iticklist instanceof SerializableTickList) {
//            level.put("TileTicks", ((SerializableTickList)iticklist).save(worldIn.getGameTime()));
//        } else {
//            level.put("TileTicks", worldIn.getPendingBlockTicks().save(chunkpos));
//        }
//
//        ITickList<Fluid> iticklist1 = icube.getFluidsToBeTicked();
//        if (iticklist1 instanceof ChunkPrimerTickList) {
//            level.put("LiquidsToBeTicked", ((ChunkPrimerTickList)iticklist1).write());
//        } else if (iticklist1 instanceof SerializableTickList) {
//            level.put("LiquidTicks", ((SerializableTickList)iticklist1).save(worldIn.getGameTime()));
//        } else {
//            level.put("LiquidTicks", worldIn.getPendingFluidTicks().save(chunkpos));
//        }

//        level.put("PostProcessing", toNbt(icube.getPackedPositions()));
//        CompoundTag compoundnbt6 = new CompoundTag();
//
//        for(Map.Entry<Heightmap.Type, Heightmap> entry : icube.getHeightmaps()) {
//            if (icube.getCubeStatus().getHeightMaps().contains(entry.getKey())) {
//                compoundnbt6.put(entry.getKey().getId(), new LongArrayNBT(entry.getValue().getDataArray()));
//            }
//        }
//
//        level.put("Heightmaps", compoundnbt6);
//        level.put("Structures", writeStructures(chunkpos, icube.getStructureStarts(), icube.getStructureReferences()));

        //return root;
        try ( DataOutputStream dout = new DataOutputStream(new FileOutputStream(cubePath.toFile())))
        {
            z.write(dout);
        }
    }

    public static ListTag toNbt(ShortList[] list) {
        ListTag ListTag = new ListTag();

        for(ShortList shortlist : list) {
            ListTag ListTag1 = new ListTag();
            if (shortlist != null) {
                for(Short oshort : shortlist) {
                    ListTag1.add(ShortTag.valueOf(oshort));
                }
            }

            ListTag.add(ListTag1);
        }

        return ListTag;
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
    private static void readEntities(ServerLevel serverLevel, CompoundTag compound, BigCube cube) {
        if (compound.contains("Entities", 9)) {
            ListTag entitiesTag = compound.getList("Entities", 10);
            if (!entitiesTag.isEmpty()) {
                serverLevel.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(entitiesTag, serverLevel));
            }
        }

        ListTag tileEntitiesNBT = compound.getList("TileEntities", 10);
        for(int j = 0; j < tileEntitiesNBT.size(); ++j) {
            CompoundTag compoundnbt1 = tileEntitiesNBT.getCompound(j);
            boolean flag = compoundnbt1.getBoolean("keepPacked");
            if (flag) {
                cube.setCubeBlockEntity(compoundnbt1);
            } else {
                BlockPos blockpos = new BlockPos(compoundnbt1.getInt("x"), compoundnbt1.getInt("y"), compoundnbt1.getInt("z"));
                BlockEntity tileentity = BlockEntity.loadStatic(blockpos, cube.getBlockState(blockpos), compoundnbt1);
                if (tileentity != null) {
                    cube.setCubeBlockEntity(tileentity);
                }
            }
        }

    }
}