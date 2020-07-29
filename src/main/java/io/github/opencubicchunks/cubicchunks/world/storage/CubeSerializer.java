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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CubeSerializer {

    public static IBigCube loadCube(ServerWorld worldIn, CubePos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        InputStream input = new FileInputStream(cubePath.toString());
        DataInputStream datainputstream = new DataInputStream(input);

        IBigCube cube = null;
        try {
            CompoundNBT root = CompressedStreamTools.read(datainputstream);
            CompoundNBT level = root.getCompound("Level");
            CubePos cubePos = CubePos.of(level.getInt("xPos"), level.getInt("yPos"), level.getInt("zPos"));
            if (!Objects.equals(pos, cubePos)) {
                CubicChunks.LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, cubePos);
            }

            ChunkGenerator<?> chunkgenerator = worldIn.getChunkProvider().getChunkGenerator();
            BiomeProvider biomeprovider = chunkgenerator.getBiomeProvider();

            CubeBiomeContainer biomecontainer = new CubeBiomeContainer(pos.asSectionPos(), biomeprovider, level.contains("Biomes", 11) ? level.getIntArray("Biomes") : null);
//            UpgradeData upgradedata = level.contains("UpgradeData", 10) ? new UpgradeData(level.getCompound("UpgradeData")) : UpgradeData.EMPTY;
//            ChunkPrimerTickList<Block> chunkprimerticklist = new ChunkPrimerTickList<>((p_222652_0_) -> {
//                return p_222652_0_ == null || p_222652_0_.getDefaultState().isAir();
//            }, pos, level.getList("ToBeTicked", 9));
//            ChunkPrimerTickList<Fluid> chunkprimerticklist1 = new ChunkPrimerTickList<>((p_222646_0_) -> {
//                return p_222646_0_ == null || p_222646_0_ == Fluids.EMPTY;
//            }, pos, level.getList("LiquidsToBeTicked", 9));
            boolean isLightOn = level.getBoolean("isLightOn");
            ListNBT sectionsNBTList = level.getList("Sections", 10);
            ChunkSection[] sections = new ChunkSection[IBigCube.SECTION_COUNT];
            boolean worldHasSkylight = worldIn.getDimension().hasSkyLight();
            AbstractChunkProvider abstractchunkprovider = worldIn.getChunkProvider();
            WorldLightManager worldlightmanager = abstractchunkprovider.getLightManager();
//            if (isLightOn) {
//                worldlightmanager.retainData(pos, true);
//            }

            for(int i = 0; i < sectionsNBTList.size(); ++i) {
                CompoundNBT sectionNBT = sectionsNBTList.getCompound(i);
                int cubeIndex = sectionNBT.getShort("i");

                if (sectionNBT.contains("Palette", 9) && sectionNBT.contains("BlockStates", 12)) {
                    int sectionY = Coords.sectionPosByIndex(pos, cubeIndex).getY();
                    ChunkSection chunksection = new ChunkSection(sectionY);
                    chunksection.getData().readChunkPalette(sectionNBT.getList("Palette", 10), sectionNBT.getLongArray("BlockStates"));
                    chunksection.recalculateRefCounts();
                    if (!chunksection.isEmpty()) {
                        sections[cubeIndex] = chunksection;
                    }

                    //TODO: reimplement poi in save format
                    //poiManager.checkConsistencyWithBlocks(pos, chunksection);
                }

                if (isLightOn) {
                    if (sectionNBT.contains("BlockLight", 7)) {
                        worldlightmanager.setData(LightType.BLOCK, Coords.sectionPosByIndex(pos, cubeIndex), new NibbleArray(sectionNBT.getByteArray("BlockLight")));
                    }

                    if (worldHasSkylight && sectionNBT.contains("SkyLight", 7)) {
                        worldlightmanager.setData(LightType.SKY, Coords.sectionPosByIndex(pos, cubeIndex), new NibbleArray(sectionNBT.getByteArray("SkyLight")));
                    }
                }
            }

            long inhabitedTime = level.getLong("InhabitedTime");
            ChunkStatus.Type chunkstatus$type = getChunkStatus(root);
            IBigCube icube;
            if (chunkstatus$type == ChunkStatus.Type.LEVELCHUNK) {
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

//                icube = new BigCube(worldIn.getWorld(), pos, biomecontainer, upgradedata, iticklist, iticklist1, inhabitedTime, sections, (p_222648_1_) -> {
//                    readEntities(level, p_222648_1_);
//                });
                icube = new BigCube(worldIn.getWorld(), pos, biomecontainer, null, null, null, inhabitedTime, sections, (p_222648_1_) -> {
                    readEntities(level, p_222648_1_);
                });
                //TODO: reimplement forge capabilities in save format
//                if (level.contains("ForgeCaps")) ((Chunk)icube).readCapsFromNBT(level.getCompound("ForgeCaps"));
            } else {
//                CubePrimer cubePrimer = new CubePrimer(pos, upgradedata, sections, chunkprimerticklist, chunkprimerticklist1);
                CubePrimer cubePrimer = new CubePrimer(pos, null, sections, null, null);
                cubePrimer.setCubeBiomes(biomecontainer);
                icube = cubePrimer;
                cubePrimer.setInhabitedTime(inhabitedTime);
                cubePrimer.setCubeStatus(ChunkStatus.byName(level.getString("Status")));
                if (cubePrimer.getCubeStatus().isAtLeast(ChunkStatus.FEATURES)) {
                    cubePrimer.setCubeLightManager(worldlightmanager);
                }

                if (!isLightOn && cubePrimer.getCubeStatus().isAtLeast(ChunkStatus.LIGHT)) {
                    for(BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.minCubeX(), pos.minCubeY(), pos.minCubeZ(), pos.maxCubeX(), pos.maxCubeY(), pos.maxCubeZ())) {
                        if (icube.getBlockState(blockpos).getLightValue(icube, blockpos) != 0) {
                            //TODO: reimplement light positions in save format
//                            cubePrimer.addLightPosition(blockpos);
                        }
                    }
                }
            }
            icube.setCubeLight(isLightOn);
            //TODO: reimplement heightmap in save format
//            CompoundNBT compoundnbt3 = level.getCompound("Heightmaps");
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
//            CompoundNBT structures = level.getCompound("Structures");
//            icube.setStructureStarts(unpackStructureStart(chunkgenerator, templateManagerIn, structures));
//            icube.setStructureReferences(unpackStructureReferences(pos, structures));
            if (level.getBoolean("shouldSave")) {
                icube.setDirty(true);
            }

            //TODO: reimplement post processing in save format
//            ListNBT postProcessingNBTList = level.getList("PostProcessing", 9);
//
//            for(int l1 = 0; l1 < postProcessingNBTList.size(); ++l1) {
//                ListNBT listnbt1 = postProcessingNBTList.getList(l1);
//
//                for(int l = 0; l < listnbt1.size(); ++l) {
//                    icube.func_201636_b(listnbt1.getShort(l), l1);
//                }
//            }

            if (chunkstatus$type == ChunkStatus.Type.LEVELCHUNK) {
                //TODO: reimplement forge chunk load event
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(icube, level, chunkstatus$type));
                return new CubePrimerWrapper((BigCube) icube);
            } else {
                CubePrimer cubePrimer = (CubePrimer)icube;
                ListNBT entitiesNBT = level.getList("Entities", 10);

                for(int i2 = 0; i2 < entitiesNBT.size(); ++i2) {
                    cubePrimer.addCubeEntity(entitiesNBT.getCompound(i2));
                }

                ListNBT tileEntitiesNBTList = level.getList("TileEntities", 10);

                for(int i1 = 0; i1 < tileEntitiesNBTList.size(); ++i1) {
                    CompoundNBT tileEntityNBT = tileEntitiesNBTList.getCompound(i1);
                    icube.addCubeTileEntity(tileEntityNBT);
                }

                ListNBT lightsNBTList = level.getList("Lights", 9);

                for(int j2 = 0; j2 < lightsNBTList.size(); ++j2) {
                    ListNBT listnbt2 = lightsNBTList.getList(j2);

                    for(int j1 = 0; j1 < listnbt2.size(); ++j1) {
                        cubePrimer.addCubeLightValue(listnbt2.getShort(j1), j2);
                    }
                }

//                CompoundNBT compoundnbt5 = level.getCompound("CarvingMasks");
//                for(String s1 : compoundnbt5.keySet()) {
//                    GenerationStage.Carving generationstage$carving = GenerationStage.Carving.valueOf(s1);
//                    cubePrimer.setCubeCarvingMask(generationstage$carving, BitSet.valueOf(compoundnbt5.getByteArray(s1)));
//                }

                //TODO: reimplement forge ChunkDataEvent
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(icube, level, chunkstatus$type));

                return cubePrimer;
            }
            
        } catch(Exception e) {
            CubicChunks.LOGGER.warn("Exception loading cube, falling back to older indev format!");
            e.printStackTrace();
            return loadCubeOld(worldIn, pos, worldDir);
        }
    }

    @Nullable
    public static IBigCube loadCubeOld(World world, CubePos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(cubePath))))) {
            ChunkStatus status = ChunkStatus.getAll().get(in.readUnsignedByte());
            ChunkSection[] sections = new ChunkSection[IBigCube.SECTION_COUNT];

            for (int i = 0; i < IBigCube.SECTION_COUNT; i++) {
                boolean isEmpty = in.readBoolean();
                if (!isEmpty) {
                    ChunkSection chunkSection = new ChunkSection(pos.minCubeY() + Coords.indexToY(i));
                    sections[i] = chunkSection;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState state = Block.BLOCK_STATE_IDS.getByValue(in.readInt());
                                if (state != null) {
                                    chunkSection.setBlockState(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }

            IBigCube cube;
            if (status.getType() == ChunkStatus.Type.PROTOCHUNK) {
                cube = new CubePrimer(pos, null, sections, null, null);
                cube.setCubeStatus(status);

                if (cube.getCubeStatus().isAtLeast(ChunkStatus.FEATURES)) {
                    ((CubePrimer)cube).setCubeLightManager(world.getChunkProvider().getLightManager());
                }

            } else {
                BigCube cubeIn = new BigCube(world, pos, null, UpgradeData.EMPTY, EmptyTickList.get(), EmptyTickList.get(), 0L, sections, null);
                cube = new CubePrimerWrapper(cubeIn);
            }
            return cube;
        }
    }

    public static void writeCube(World worldIn, IBigCube icube, Path worldDir) throws IOException {
        CubePos pos = icube.getCubePos();
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath.getParent())) {
            Files.createDirectories(cubePath.getParent());
        }
        CompoundNBT z = new CompoundNBT();
        CompoundNBT root = new CompoundNBT();
        z.put("", root);
        CompoundNBT level = new CompoundNBT();
        root.put("Level", level);

        level.putInt("xPos", pos.getX());
        level.putInt("yPos", pos.getY());
        level.putInt("zPos", pos.getZ());

        level.putLong("LastUpdate", worldIn.getGameTime());
        level.putLong("InhabitedTime", icube.getCubeInhabitedTime());
        level.putString("Status", icube.getCubeStatus().getName());

        ChunkSection[] sections = icube.getCubeSections();
        ListNBT sectionsNBTList = new ListNBT();
        WorldLightManager worldlightmanager = worldIn.getChunkProvider().getLightManager();
        boolean cubeHasLight = icube.hasCubeLight();

        for(int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
            ChunkSection section = sections[i];

            NibbleArray blockData = worldlightmanager.getLightEngine(LightType.BLOCK).getData(Coords.sectionPosByIndex(pos, i));
            NibbleArray skyData = worldlightmanager.getLightEngine(LightType.SKY).getData(Coords.sectionPosByIndex(pos, i));
            CompoundNBT sectionNBT = new CompoundNBT();
            if (section != Chunk.EMPTY_SECTION || blockData != null || skyData != null) {

                sectionNBT.putShort("i", (byte)(i));
                if (section != Chunk.EMPTY_SECTION) {
                    section.getData().writeChunkPalette(sectionNBT, "Palette", "BlockStates");
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

        BiomeContainer biomecontainer = icube.getCubeBiomes();
        if (biomecontainer != null) {
            level.putIntArray("Biomes", biomecontainer.getBiomeIds());
        }

        ListNBT tileEntitiesNBTList = new ListNBT();

        for(BlockPos blockpos : icube.getCubeTileEntitiesPos()) {
            CompoundNBT tileEntityNBT = icube.getCubeTileEntityNBT(blockpos);
//            if (tileEntityNBT != null) {
                CubicChunks.LOGGER.debug("Saving tile entity at " + blockpos.toString());
                tileEntitiesNBTList.add(tileEntityNBT);
//            }
        }

        level.put("TileEntities", tileEntitiesNBTList);
        ListNBT entitiesNBTList = new ListNBT();
        if (icube.getCubeStatus().getType() == ChunkStatus.Type.LEVELCHUNK) {//icube.getCubeStatus().getType() == ChunkStatus.Type.LEVELCHUNK) {
            BigCube cube = (BigCube)icube;
            cube.setHasEntities(false);

            for(int k = 0; k < cube.getCubeEntityLists().length; ++k) {
                for(Entity entity : cube.getCubeEntityLists()[k]) {
                    CompoundNBT entityNBT = new CompoundNBT();
                    try {
                        if (entity.writeUnlessPassenger(entityNBT)) {
                            cube.setHasEntities(true);
                            entitiesNBTList.add(entityNBT);
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().error("An Entity type {} has thrown an exception trying to write state. It will not persist. Report this to the mod author", entity.getType(), e);
                    }
                }
            }
            //TODO: reimplement forge capabilities
//            try {
//                final CompoundNBT capTag = cube.writeCapsToNBT();
//                if (capTag != null) level.put("ForgeCaps", capTag);
//            } catch (Exception exception) {
//                LogManager.getLogger().error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
//            }
        } else {
            CubePrimer cubePrimer = (CubePrimer)icube;
            entitiesNBTList.addAll(cubePrimer.getCubeEntities());
            //level.put("Lights", toNbt(cubePrimer.getCubePackedLightPositions()));
            CompoundNBT carvingMasksNBT = new CompoundNBT();

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
//            level.put("TileTicks", worldIn.getPendingBlockTicks().func_219503_a(chunkpos));
//        }
//
//        ITickList<Fluid> iticklist1 = icube.getFluidsToBeTicked();
//        if (iticklist1 instanceof ChunkPrimerTickList) {
//            level.put("LiquidsToBeTicked", ((ChunkPrimerTickList)iticklist1).write());
//        } else if (iticklist1 instanceof SerializableTickList) {
//            level.put("LiquidTicks", ((SerializableTickList)iticklist1).save(worldIn.getGameTime()));
//        } else {
//            level.put("LiquidTicks", worldIn.getPendingFluidTicks().func_219503_a(chunkpos));
//        }

//        level.put("PostProcessing", toNbt(icube.getPackedPositions()));
//        CompoundNBT compoundnbt6 = new CompoundNBT();
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

    public static ListNBT toNbt(ShortList[] list) {
        ListNBT listnbt = new ListNBT();

        for(ShortList shortlist : list) {
            ListNBT listnbt1 = new ListNBT();
            if (shortlist != null) {
                for(Short oshort : shortlist) {
                    listnbt1.add(ShortNBT.valueOf(oshort));
                }
            }

            listnbt.add(listnbt1);
        }

        return listnbt;
    }
    public static ChunkStatus.Type getChunkStatus(@Nullable CompoundNBT chunkNBT) {
        if (chunkNBT != null) {
            ChunkStatus chunkstatus = ChunkStatus.byName(chunkNBT.getCompound("Level").getString("Status"));
            if (chunkstatus != null) {
                return chunkstatus.getType();
            }
        }

        return ChunkStatus.Type.PROTOCHUNK;
    }
    private static void readEntities(CompoundNBT compound, BigCube cube) {
        ListNBT listnbt = compound.getList("Entities", 10);
        World world = cube.getWorld();

        for(int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT compoundnbt = listnbt.getCompound(i);
            EntityType.loadEntityAndExecute(compoundnbt, world, (p_222655_1_) -> {
                cube.addEntity(p_222655_1_);
                return p_222655_1_;
            });
            cube.setHasEntities(true);
        }

        ListNBT listnbt1 = compound.getList("TileEntities", 10);

        for(int j = 0; j < listnbt1.size(); ++j) {
            CompoundNBT compoundnbt1 = listnbt1.getCompound(j);
            boolean flag = compoundnbt1.getBoolean("keepPacked");
            if (flag) {
                cube.addCubeTileEntity(compoundnbt1);
            } else {
                TileEntity tileentity = TileEntity.create(compoundnbt1);
                if (tileentity != null) {
                    cube.addCubeTileEntity(tileentity);
                }
            }
        }

    }
}
