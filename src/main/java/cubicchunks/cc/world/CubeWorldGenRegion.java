package cubicchunks.cc.world;

import cubicchunks.cc.chunk.ISection;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.ITickList;
import net.minecraft.world.WorldGenTickList;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

public class CubeWorldGenRegion {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<ISection> sectionPrimers;
    private final int mainCubeX;
    private final int mainCubeY;
    private final int mainCubeZ;
    private final int diameter;
    private final ServerWorld world;
    private final long seed;
    private final int seaLevel;
    private final WorldInfo worldInfo;
    private final Random random;
    private final Dimension dimension;
    private final GenerationSettings chunkGenSettings;
//    private final ITickList<Block> pendingBlockTickList = new WorldGenTickList<>((blockPos) -> {
//        return this.getSection(blockPos).getBlocksToBeTicked();
//    });
//    private final ITickList<Fluid> pendingFluidTickList = new WorldGenTickList<>((blockPos) -> {
//        return this.getSection(blockPos).getFluidsToBeTicked();
//    });
//    private final BiomeManager biomeManager;

    public CubeWorldGenRegion(ServerWorld worldIn, List<ISection> sectionsIn) {
        int i = MathHelper.floor(Math.sqrt(sectionsIn.size()));
        if (i * i != sectionsIn.size()) {
            throw Util.pauseDevMode(new IllegalStateException("Cache size is not a square."));
        } else {
            SectionPos sectionPos = sectionsIn.get(sectionsIn.size() / 2).getSectionPos();
            this.sectionPrimers = sectionsIn;
            this.mainCubeX = sectionPos.getX();
            this.mainCubeY = sectionPos.getY();
            this.mainCubeZ = sectionPos.getZ();
            this.diameter = i;
            this.world = worldIn;
            this.seed = worldIn.getSeed();
            this.chunkGenSettings = worldIn.getChunkProvider().getChunkGenerator().getSettings();
            this.seaLevel = worldIn.getSeaLevel();
            this.worldInfo = worldIn.getWorldInfo();
            this.random = worldIn.getRandom();
            this.dimension = worldIn.getDimension();
            //this.biomeManager = new BiomeManager(this, WorldInfo.byHashing(this.seed), this.dimension.getType().getMagnifier());
        }
    }

    public ISection getSection(BlockPos blockPos)
    {
        return this.getSection(blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, true);
    }

    public ISection getSection(int x, int y, int z, ChunkStatus requiredStatus, boolean nonnull)
    {
        ISection isection;
        if (this.cubeExists(x, y, z)) {
            SectionPos sectionPos = this.sectionPrimers.get(0).getSectionPos();
            int i = x - sectionPos.getX();
            int j = y - sectionPos.getY();
            int k = z - sectionPos.getZ();
            //TODO: index into flat 3dim array correctly
            isection = this.sectionPrimers.get(i + k * this.diameter);
            if (isection.getSectionStatus().isAtLeast(requiredStatus)) {
                return isection;
            }
        } else {
            isection = null;
        }

        if (!nonnull) {
            return null;
        } else {
            ISection isection1 = this.sectionPrimers.get(0);
            ISection isection2 = this.sectionPrimers.get(this.sectionPrimers.size() - 1);
            LOGGER.error("Requested section : {} {} {}", x, y, z);
            LOGGER.error("Region bounds : {} {} {} | {} {} {}",
                    isection1.getSectionPos().getX(), isection1.getSectionPos().getY(), isection1.getSectionPos().getZ(),
                    isection2.getSectionPos().getX(), isection2.getSectionPos().getY(), isection2.getSectionPos().getZ());
            if (isection != null) {
                throw (RuntimeException)Util.pauseDevMode(new RuntimeException(String.format("Section is not of correct status. Expecting %s, got %s "
                        + "| %s %s %s", requiredStatus, isection.getSectionStatus(), x, y, z)));
            } else {
                throw (RuntimeException)Util.pauseDevMode(new RuntimeException(String.format("We are asking a region for a section out of bound | "
                        + "%s %s %s", x, y, z)));
            }
        }
    }

    public boolean cubeExists(int x, int y, int z)
    {
        ISection isection = this.sectionPrimers.get(0);
        ISection isection2 = this.sectionPrimers.get(this.sectionPrimers.size() - 1);
        return x >= isection.getSectionPos().getX() && x <= isection2.getSectionPos().getX() &&
                y >= isection.getSectionPos().getY() && y <= isection2.getSectionPos().getY() &&
                z >= isection.getSectionPos().getZ() && z <= isection2.getSectionPos().getZ();
    }
}
