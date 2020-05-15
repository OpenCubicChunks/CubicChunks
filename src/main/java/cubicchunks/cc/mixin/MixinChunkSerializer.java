package cubicchunks.cc.mixin;

import cubicchunks.cc.CubicChunks;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.ITickList;
import net.minecraft.world.LightType;
import net.minecraft.world.SerializableTickList;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Objects;

import static net.minecraft.world.chunk.storage.ChunkSerializer.*;

@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer {
    /*
    TODO: Clean up this mess and make this much much better and less busy.
      Purpose of this was to change the variable "achunksection" to be 32.
     */

    @Inject(at = @At("HEAD"), method = "read", cancellable = true)
    private static void read(ServerWorld worldIn, TemplateManager templateManagerIn, PointOfInterestManager poiManager, ChunkPos pos, CompoundNBT compound, CallbackInfoReturnable<ChunkPrimer> cir) {
        ChunkGenerator<?> chunkgenerator = worldIn.getChunkProvider().getChunkGenerator();
        BiomeProvider biomeprovider = chunkgenerator.getBiomeProvider();
        CompoundNBT compoundnbt = compound.getCompound("Level");
        ChunkPos chunkpos = new ChunkPos(compoundnbt.getInt("xPos"), compoundnbt.getInt("zPos"));
        if (!Objects.equals(pos, chunkpos)) {
            CubicChunks.LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, chunkpos);
        }

        BiomeContainer biomecontainer = new BiomeContainer(pos, biomeprovider, compoundnbt.contains("Biomes", 11) ? compoundnbt.getIntArray("Biomes") : null);
        UpgradeData upgradedata = compoundnbt.contains("UpgradeData", 10) ? new UpgradeData(compoundnbt.getCompound("UpgradeData")) : UpgradeData.EMPTY;
        ChunkPrimerTickList<Block> chunkprimerticklist = new ChunkPrimerTickList<>((p_222652_0_) -> p_222652_0_ == null || p_222652_0_.getDefaultState().isAir(), pos, compoundnbt.getList("ToBeTicked", 9));
        ChunkPrimerTickList<Fluid> chunkprimerticklist1 = new ChunkPrimerTickList<>((p_222646_0_) -> p_222646_0_ == null || p_222646_0_ == Fluids.EMPTY, pos, compoundnbt.getList("LiquidsToBeTicked", 9));
        boolean flag = compoundnbt.getBoolean("isLightOn");
        ListNBT listnbt = compoundnbt.getList("Sections", 10);
        int i = 16;
        ChunkSection[] achunksection = new ChunkSection[32];
        boolean flag1 = worldIn.getDimension().hasSkyLight();
        AbstractChunkProvider abstractchunkprovider = worldIn.getChunkProvider();
        WorldLightManager worldlightmanager = abstractchunkprovider.getLightManager();
        if (flag) {
            worldlightmanager.retainData(pos, true);
        }

        for (int j = 0; j < listnbt.size(); ++j) {
            CompoundNBT compoundnbt1 = listnbt.getCompound(j);
            int k = compoundnbt1.getByte("Y");
            if (compoundnbt1.contains("Palette", 9) && compoundnbt1.contains("BlockStates", 12)) {
                ChunkSection chunksection = new ChunkSection(k << 4);
                chunksection.getData().readChunkPalette(compoundnbt1.getList("Palette", 10), compoundnbt1.getLongArray("BlockStates"));
                chunksection.recalculateRefCounts();
                if (!chunksection.isEmpty()) {
                    achunksection[k] = chunksection;
                }

                poiManager.checkConsistencyWithBlocks(pos, chunksection);
            }

            if (flag) {
                if (compoundnbt1.contains("BlockLight", 7)) {
                    worldlightmanager.setData(LightType.BLOCK, SectionPos.from(pos, k), new NibbleArray(compoundnbt1.getByteArray("BlockLight")));
                }

                if (flag1 && compoundnbt1.contains("SkyLight", 7)) {
                    worldlightmanager.setData(LightType.SKY, SectionPos.from(pos, k), new NibbleArray(compoundnbt1.getByteArray("SkyLight")));
                }
            }
        }

        long k1 = compoundnbt.getLong("InhabitedTime");
        ChunkStatus.Type chunkstatus$type = getChunkStatus(compound);
        IChunk ichunk;
        if (chunkstatus$type == ChunkStatus.Type.LEVELCHUNK) {
            ITickList<Block> iticklist;
            if (compoundnbt.contains("TileTicks", 9)) {
                iticklist = SerializableTickList.create(compoundnbt.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::getOrDefault);
            } else {
                iticklist = chunkprimerticklist;
            }

            ITickList<Fluid> iticklist1;
            if (compoundnbt.contains("LiquidTicks", 9)) {
                iticklist1 = SerializableTickList.create(compoundnbt.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::getOrDefault);
            } else {
                iticklist1 = chunkprimerticklist1;
            }

            ichunk = new Chunk(worldIn.getWorld(), pos, biomecontainer, upgradedata, iticklist, iticklist1, k1, achunksection, (p_222648_1_) -> {
                readEntities(compoundnbt, p_222648_1_);
            });
            if (compoundnbt.contains("ForgeCaps"))
                ((Chunk) ichunk).readCapsFromNBT(compoundnbt.getCompound("ForgeCaps"));
        } else {
            ChunkPrimer chunkprimer = new ChunkPrimer(pos, upgradedata, achunksection, chunkprimerticklist, chunkprimerticklist1);
            chunkprimer.func_225548_a_(biomecontainer);
            ichunk = chunkprimer;
            chunkprimer.setInhabitedTime(k1);
            chunkprimer.setStatus(ChunkStatus.byName(compoundnbt.getString("Status")));
            if (chunkprimer.getStatus().isAtLeast(ChunkStatus.FEATURES)) {
                chunkprimer.setLightManager(worldlightmanager);
            }

            if (!flag && chunkprimer.getStatus().isAtLeast(ChunkStatus.LIGHT)) {
                for (BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.getXStart(), 0, pos.getZStart(), pos.getXEnd(), 255, pos.getZEnd())) {
                    if (ichunk.getBlockState(blockpos).getLightValue(ichunk, blockpos) != 0) {
                        chunkprimer.addLightPosition(blockpos);
                    }
                }
            }
        }

        ichunk.setLight(flag);
        CompoundNBT compoundnbt3 = compoundnbt.getCompound("Heightmaps");
        EnumSet<Heightmap.Type> enumset = EnumSet.noneOf(Heightmap.Type.class);

        for (Heightmap.Type heightmap$type : ichunk.getStatus().getHeightMaps()) {
            String s = heightmap$type.getId();
            if (compoundnbt3.contains(s, 12)) {
                ichunk.setHeightmap(heightmap$type, compoundnbt3.getLongArray(s));
            } else {
                enumset.add(heightmap$type);
            }
        }

        Heightmap.updateChunkHeightmaps(ichunk, enumset);
        CompoundNBT compoundnbt4 = compoundnbt.getCompound("Structures");
        ichunk.setStructureStarts(unpackStructureStart(chunkgenerator, templateManagerIn, compoundnbt4));
        ichunk.setStructureReferences(unpackStructureReferences(pos, compoundnbt4));
        if (compoundnbt.getBoolean("shouldSave")) {
            ichunk.setModified(true);
        }

        ListNBT listnbt3 = compoundnbt.getList("PostProcessing", 9);

        for (int l1 = 0; l1 < listnbt3.size(); ++l1) {
            ListNBT listnbt1 = listnbt3.getList(l1);

            for (int l = 0; l < listnbt1.size(); ++l) {
                ichunk.func_201636_b(listnbt1.getShort(l), l1);
            }
        }

        if (chunkstatus$type == ChunkStatus.Type.LEVELCHUNK) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(ichunk, compoundnbt, chunkstatus$type));
            cir.setReturnValue(new ChunkPrimerWrapper((Chunk) ichunk));
        } else {
            ChunkPrimer chunkprimer1 = (ChunkPrimer) ichunk;
            ListNBT listnbt4 = compoundnbt.getList("Entities", 10);

            for (int i2 = 0; i2 < listnbt4.size(); ++i2) {
                chunkprimer1.addEntity(listnbt4.getCompound(i2));
            }

            ListNBT listnbt5 = compoundnbt.getList("TileEntities", 10);

            for (int i1 = 0; i1 < listnbt5.size(); ++i1) {
                CompoundNBT compoundnbt2 = listnbt5.getCompound(i1);
                ichunk.addTileEntity(compoundnbt2);
            }

            ListNBT listnbt6 = compoundnbt.getList("Lights", 9);

            for (int j2 = 0; j2 < listnbt6.size(); ++j2) {
                ListNBT listnbt2 = listnbt6.getList(j2);

                for (int j1 = 0; j1 < listnbt2.size(); ++j1) {
                    chunkprimer1.addLightValue(listnbt2.getShort(j1), j2);
                }
            }

            CompoundNBT compoundnbt5 = compoundnbt.getCompound("CarvingMasks");

            for (String s1 : compoundnbt5.keySet()) {
                GenerationStage.Carving generationstage$carving = GenerationStage.Carving.valueOf(s1);
                chunkprimer1.setCarvingMask(generationstage$carving, BitSet.valueOf(compoundnbt5.getByteArray(s1)));
            }

            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(ichunk, compoundnbt, chunkstatus$type));

            cir.setReturnValue(chunkprimer1);
        }
    }
}
