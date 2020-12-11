package io.github.opencubicchunks.cubicchunks.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.cube.FillFromNoiseProtoChunkHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import org.jetbrains.annotations.Nullable;

public final class CCNoiseBasedChunkGenerator extends ChunkGenerator {
    public static final Codec<CCNoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((CCNoiseBasedChunkGenerator) -> {
            return CCNoiseBasedChunkGenerator.biomeSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((CCNoiseBasedChunkGenerator) -> {
            return CCNoiseBasedChunkGenerator.seed;
        }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.settings;
        })).apply(instance, instance.stable(CCNoiseBasedChunkGenerator::new));
    });
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], (array) -> {
        for (int i = 0; i < 24; ++i) {
            for (int j = 0; j < 24; ++j) {
                for (int k = 0; k < 24; ++k) {
                    array[i * 24 * 24 + j * 24 + k] = (float) computeContribution(j - 12, k - 12, i - 12);
                }
            }
        }

    });
    private static final float[] BIOME_WEIGHTS = Util.make(new float[25], (array) -> {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                float f = 10.0F / Mth.sqrt((float) (i * i + j * j) + 0.2F);
                array[i + 2 + (j + 2) * 5] = f;
            }
        }

    });
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final int chunkHeight;
    private final int chunkWidth;
    private final int chunkSizeX;
    private final int chunkSizeY;
    private final int chunkSizeZ;
    protected final WorldgenRandom random;
    private final PerlinNoise minLimitPerlinNoise;
    private final PerlinNoise maxLimitPerlinNoise;
    private final PerlinNoise mainPerlinNoise;
    private final SurfaceNoise surfaceNoise;
    private final PerlinNoise depthNoise;
    @Nullable
    private final SimplexNoise islandNoise;
    protected final BlockState defaultBlock;
    protected final BlockState defaultFluid;
    private final long seed;
    protected final Supplier<NoiseGeneratorSettings> settings;
    private final int height;

    public CCNoiseBasedChunkGenerator(BiomeSource biomeSource, long seed, Supplier<NoiseGeneratorSettings> supplier) {
        this(biomeSource, biomeSource, seed, supplier);
    }

    private CCNoiseBasedChunkGenerator(BiomeSource biomeSource, BiomeSource biomeSource2, long seed, Supplier<NoiseGeneratorSettings> supplier) {
        super(biomeSource, biomeSource2, supplier.get().structureSettings(), seed);
        this.seed = seed;
        NoiseGeneratorSettings noiseGeneratorSettings = supplier.get();
        this.settings = supplier;
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        this.height = /*noiseSettings.height()*/ IBigCube.DIAMETER_IN_BLOCKS/*!*/; //256 in vanilla
        this.chunkHeight = noiseSettings.noiseSizeVertical() * 4; //8 in vanilla
        this.chunkWidth = noiseSettings.noiseSizeHorizontal() * 4; //4 in vanilla
        this.defaultBlock = noiseGeneratorSettings.getDefaultBlock(); //Stone
        this.defaultFluid = noiseGeneratorSettings.getDefaultFluid(); //Water
        this.chunkSizeX = 16 / this.chunkWidth; //4 in vanilla
        this.chunkSizeY = /*noiseSettings.height()*/ IBigCube.DIAMETER_IN_BLOCKS/*!*/ / this.chunkHeight; //32 in vanilla
        this.chunkSizeZ = 16 / this.chunkWidth; //4 in vanilla
        this.random = new WorldgenRandom(seed);
        this.minLimitPerlinNoise = new PerlinNoise(this.random, IntStream.rangeClosed(-15, 0));
        this.maxLimitPerlinNoise = new PerlinNoise(this.random, IntStream.rangeClosed(-15, 0));
        this.mainPerlinNoise = new PerlinNoise(this.random, IntStream.rangeClosed(-7, 0));
        this.surfaceNoise =
            noiseSettings.useSimplexSurfaceNoise() ? new PerlinSimplexNoise(this.random, IntStream.rangeClosed(-3, 0)) : new PerlinNoise(this.random, IntStream.rangeClosed(-3, 0));
        this.random.consumeCount(2620);
        this.depthNoise = new PerlinNoise(this.random, IntStream.rangeClosed(-15, 0));
        if (noiseSettings.islandNoiseOverride()) {
            WorldgenRandom worldgenRandom = new WorldgenRandom(seed);
            worldgenRandom.consumeCount(17292);
            this.islandNoise = new SimplexNoise(worldgenRandom);
        } else {
            this.islandNoise = null;
        }

    }

    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Environment(EnvType.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return new CCNoiseBasedChunkGenerator(this.biomeSource.withSeed(seed), seed, this.settings);
    }

    public boolean stable(long seed, ResourceKey<NoiseGeneratorSettings> settingsKey) {
        return this.seed == seed && this.settings.get().stable(settingsKey);
    }

    /**
     * @param horizontalScale Low and High horizontal frequencies.
     * @param verticalScale Low and High vertical frequencies.
     * @param horizontalStretch Horizontal Selector frequencies.
     * @param verticalStretch Vertical Selector frequencies.
     */
    private double sampleAndClampNoise(int x, int y, int z, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
        double minClampNoise = 0.0D;
        double maxClampNoise = 0.0D;
        double mainClampNoise = 0.0D;
        double octaveFactor = 1.0D; //Acts as frequency & perlin persistence.

        for (int octave = 0; octave < 16; ++octave) {
            //Prevents farlands
            double wrapX = PerlinNoise.wrap((double) x * horizontalScale * octaveFactor);
            double wrapY = PerlinNoise.wrap((double) y * verticalScale * octaveFactor);
            double wrapZ = PerlinNoise.wrap((double) z * horizontalScale * octaveFactor);
            double yDiscontinuityFrequency = verticalScale * octaveFactor;
            ImprovedNoise minNoise = this.minLimitPerlinNoise.getOctaveNoise(octave);
            if (minNoise != null) {
                minClampNoise += minNoise.noise(wrapX, wrapY, wrapZ, yDiscontinuityFrequency, (double) y * yDiscontinuityFrequency) / octaveFactor;
            }

            ImprovedNoise maxNoise = this.maxLimitPerlinNoise.getOctaveNoise(octave);
            if (maxNoise != null) {
                maxClampNoise += maxNoise.noise(wrapX, wrapY, wrapZ, yDiscontinuityFrequency, (double) y * yDiscontinuityFrequency) / octaveFactor;
            }

            if (octave < 8) {
                ImprovedNoise mainNoise = this.mainPerlinNoise.getOctaveNoise(octave);
                if (mainNoise != null) {
                    mainClampNoise += mainNoise
                        .noise(PerlinNoise.wrap((double) x * horizontalStretch * octaveFactor), PerlinNoise.wrap((double) y * verticalStretch * octaveFactor),
                            PerlinNoise.wrap((double) z * horizontalStretch * octaveFactor),
                            verticalStretch * octaveFactor, (double) y * verticalStretch * octaveFactor) / octaveFactor;
                }
            }

            octaveFactor /= 2.0D;
        }

        return Mth.clampedLerp(minClampNoise / 512.0D, maxClampNoise / 512.0D, (mainClampNoise / 10.0D + 1.0D) / 2.0D);
    }

//   private double[] makeAndFillNoiseColumn(int x, int z) {
//      double[] ds = new double[this.chunkCountY + 1];
//      this.fillNoiseColumn(ds, x, z);
//      return ds;
//   }

    private void fillNoiseColumn(double[] densities, int x, int z, ChunkAccess chunkAccess/*!*/) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        double biomeDensityOffset;
        double biomeDensityFactor;
        if (this.islandNoise != null) {
            biomeDensityOffset = TheEndBiomeSource.getHeightValue(this.islandNoise, x, z) - 8.0F;
            if (biomeDensityOffset > 0.0D) {
                biomeDensityFactor = 0.25D;
            } else {
                biomeDensityFactor = 1.0D;
            }
        } else {
            float totalDepth = 0.0F;
            float totalScale = 0.0F;
            float totalWeight = 0.0F;
            int seaLevel = this.getSeaLevel();
            float mainHeight = this.biomeSource.getNoiseBiome(x, seaLevel, z).getDepth();

            for (int biomeX = -2; biomeX <= 2; ++biomeX) {
                for (int biomeZ = -2; biomeZ <= 2; ++biomeZ) {
                    Biome biome = this.biomeSource.getNoiseBiome(x + biomeX, seaLevel, z + biomeZ);
                    float rawDepth = biome.getDepth();
                    float rawScale = biome.getScale();

                    if (noiseSettings.isAmplified() && rawDepth > 0.0F) {
                        rawDepth = 1.0F + rawDepth * 2.0F;
                        rawScale = 1.0F + rawScale * 4.0F;
                    }

                    float weightFactor = rawDepth > mainHeight ? 0.5F : 1.0F;
                    float biomeWeight = weightFactor * BIOME_WEIGHTS[biomeX + 2 + (biomeZ + 2) * 5] / (rawDepth + 2.0F);
                    totalDepth += rawDepth * biomeWeight;
                    totalScale += rawScale * biomeWeight;
                    totalWeight += biomeWeight;
                }
            }

            float rawDepth = totalDepth / totalWeight;
            float rawScale = totalScale / totalWeight;
            double biomeDepth = rawDepth * 0.5F - 0.125F;
            double biomeScale = rawScale * 0.9F + 0.1F;
            biomeDensityOffset = biomeDepth * 0.265625D;
            biomeDensityFactor = 96.0D / biomeScale;
        }

        double lowAndHighHorizontalFrequency = 684.412D * noiseSettings.noiseSamplingSettings().xzScale(); //Low and High horizontal frequency.
        double lowAndHighVerticalFrequency = 684.412D * noiseSettings.noiseSamplingSettings().yScale(); //Low and High vertical frequency.
        double horizontalSelectorFrequency = lowAndHighHorizontalFrequency / noiseSettings.noiseSamplingSettings().xzFactor(); //Horizontal Selector frequency.
        double verticalSelectorFrequency = lowAndHighVerticalFrequency / noiseSettings.noiseSamplingSettings().yFactor();//Vertical Selector frequency.
        double topSlideSettingTarget = noiseSettings.topSlideSettings().target();
        double topSlideSettingSize = noiseSettings.topSlideSettings().size();
        double topSlideOffset = noiseSettings.topSlideSettings().offset();
        double bottomSlideTarget = noiseSettings.bottomSlideSettings().target();
        double bottomSlideSize = noiseSettings.bottomSlideSettings().size();
        double bottomSlideOffset = noiseSettings.bottomSlideSettings().offset();
        double randomOffset = noiseSettings.randomDensityOffset() ? this.getRandomDensity(x, z) : 0.0D;
        double densityFactor = noiseSettings.densityFactor();
        double densityOffset = noiseSettings.densityOffset();
        int ySize = Mth.intFloorDiv(/*noiseSettings.minY() or 0*/ chunkAccess.getMinBuildHeight() /*!*/, this.chunkHeight); //Size in blocks

        for (int ySection = 0; ySection <= this.chunkSizeY; ++ySection) {
            int y = ySection + (ySize);
            double height = this.sampleAndClampNoise(x, y, z, lowAndHighHorizontalFrequency, lowAndHighVerticalFrequency, horizontalSelectorFrequency, verticalSelectorFrequency);
            double baseYGradient = 1.0D - (double) y * 2.0D / (double)/*this.chunkSizeY*/ 32/*!*/ + randomOffset; //TODO: 32 constant must change w/ datapacks.
            double configuredYGradient = baseYGradient * densityFactor + densityOffset;
            double biomeYGradient = (configuredYGradient + biomeDensityOffset) * biomeDensityFactor;

            if (biomeYGradient > 0.0D) {
                height += biomeYGradient * 4.0D;
            } else {
                height += biomeYGradient;
            }

            //TODO: Datapacks /*!*/
//            double slideFraction;
//            if (topSlideSettingSize > 0.0D) {
//                slideFraction = ((double) (this.chunkSizeY - y) - topSlideOffset) / topSlideSettingSize;
//                height = Mth.clampedLerp(topSlideSettingTarget, height, slideFraction);
//            }
//
//            if (bottomSlideSize > 0.0D) {
//                slideFraction = ((double) y - bottomSlideOffset) / bottomSlideSize;
//                height = Mth.clampedLerp(bottomSlideTarget, height, slideFraction);
//            }

            densities[ySection] = height;
        }

    }

    //Not really used in current version. Implemented wrong by mojang.
    private double getRandomDensity(int x, int z) {
        double depthNoise = this.depthNoise.getValue(x * 200, 10.0D, z * 200, 1.0D, 0.0D, true);
        double f;
        if (depthNoise < 0.0D) {
            f = -depthNoise * 0.3D;
        } else {
            f = depthNoise;
        }

        double g = f * 24.575625D - 2.0D;
        return g < 0.0D ? g * 0.009486607142857142D : Math.min(g, 1.0D) * 0.006640625D;
    }

    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType) {
        return this.iterateNoiseColumn(x, z, null, heightmapType.isOpaque());
    }

    public NoiseColumn getBaseColumn(int i, int j) {
        BlockState[] blockStates = new BlockState[this.chunkSizeY * this.chunkHeight];
        this.iterateNoiseColumn(i, j, blockStates, null);
        return new NoiseColumn(this.settings.get().noiseSettings().minY(), blockStates);
    }

    private int iterateNoiseColumn(int x, int z, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate) {
//      int i = Math.floorDiv(x, this.chunkWidth);
//      int j = Math.floorDiv(z, this.chunkWidth);
//      int k = Math.floorMod(x, this.chunkWidth);
//      int l = Math.floorMod(z, this.chunkWidth);
//      double d = (double)k / (double)this.chunkWidth;
//      double e = (double)l / (double)this.chunkWidth;
//      double[][] ds = new double[][]{this.makeAndFillNoiseColumn(i, j), this.makeAndFillNoiseColumn(i, j + 1), this.makeAndFillNoiseColumn(i + 1, j), this.makeAndFillNoiseColumn(i + 1,
//      j + 1)};
//
//      for(int m = this.chunkCountY - 1; m >= 0; --m) {
//         double f = ds[0][m];
//         double g = ds[1][m];
//         double h = ds[2][m];
//         double n = ds[3][m];
//         double o = ds[0][m + 1];
//         double p = ds[1][m + 1];
//         double q = ds[2][m + 1];
//         double r = ds[3][m + 1];
//
//         for(int s = this.chunkHeight - 1; s >= 0; --s) {
//            double t = (double)s / (double)this.chunkHeight;
//            double u = Mth.lerp3(t, d, e, f, o, h, q, g, p, n, r);
//            int v = m * this.chunkHeight + s;
//            int w = v + ((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY();
//            BlockState blockState = this.generateBaseState(u, w);
//            if (states != null) {
//               states[v] = blockState;
//            }
//
//            if (predicate != null && predicate.test(blockState)) {
//               return w + 1;
//            }
//         }
//      }

        return 0;
    }

    protected BlockState generateBaseState(double density, int y) {
        BlockState worldBlock;
        if (density > 0.0D) {
            worldBlock = this.defaultBlock;
        } else if (y < this.getSeaLevel()) {
            worldBlock = this.defaultFluid;
        } else {
            worldBlock = AIR;
        }

        return worldBlock;
    }

    public void buildSurfaceAndBedrock(WorldGenRegion region, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        WorldgenRandom worldgenRandom = new WorldgenRandom();
        worldgenRandom.setBaseChunkSeed(chunkX, chunkZ);
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int moveX = 0; moveX < 16; ++moveX) {
            for (int moveZ = 0; moveZ < 16; ++moveZ) {
                int blockX = minX + moveX;
                int blockZ = minZ + moveZ;

                int startHeight = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, moveX, moveZ) + 1;
                double noise = this.surfaceNoise.getSurfaceNoiseValue((double) blockX * 0.0625D, (double) blockZ * 0.0625D, 0.0625D, (double) moveX * 0.0625D) * 15.0D;
                Biome biome = region.getBiome(mutableBlockPos.set(minX + moveX, startHeight, minZ + moveZ));

                try {
                    biome.buildSurfaceAt(worldgenRandom, chunk, blockX, blockZ, startHeight, noise, this.defaultBlock, this.defaultFluid, this.getSeaLevel(), region
                        .getSeed());
                } catch (SectionSizeCubeAccessWrapper.StopGeneratingThrowable ignored) {

                }
            }
        }
    }

    public void fillFromNoise(LevelAccessor world, StructureFeatureManager featureManager, ChunkAccess chunk) {
        ObjectList<StructurePiece> structurePiecesList = new ObjectArrayList<>(10);
        ObjectList<JigsawJunction> jigsawJunctionsList = new ObjectArrayList<>(32);
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        int ySectionCoordX = SectionPos.sectionToBlockCoord(chunkX);
        int ySectionCoordZ = SectionPos.sectionToBlockCoord(chunkZ);

//        for (StructureFeature<?> structureFeature : StructureFeature.NOISE_AFFECTING_FEATURES) {
//            featureManager.startsForFeature(SectionPos.of(chunkPos, 0), structureFeature).forEach((start) -> {
//                Iterator<StructurePiece> structurePieceIterator = start.getPieces().iterator();
//
//                while (true) {
//                    StructurePiece structurePiece;
//                    do {
//                        if (!structurePieceIterator.hasNext()) {
//                            return;
//                        }
//
//                        structurePiece = structurePieceIterator.next();
//                    } while (!structurePiece.isCloseToChunk(chunkPos, 12));
//
//                    if (structurePiece instanceof PoolElementStructurePiece) {
//                        PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece) structurePiece;
//                        StructureTemplatePool.Projection projection = poolElementStructurePiece.getElement().getProjection();
//                        if (projection == StructureTemplatePool.Projection.RIGID) {
//                            structurePiecesList.add(poolElementStructurePiece);
//                        }
//
//                        for (JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
//                            int jigsawJunctionSourceX = jigsawJunction.getSourceX();
//                            int jigsawJunctionSourceZ = jigsawJunction.getSourceZ();
//                            if (jigsawJunctionSourceX > ySectionCoordX - 12 && jigsawJunctionSourceZ > ySectionCoordZ - 12 && jigsawJunctionSourceX < ySectionCoordX + 15 + 12
//                                && jigsawJunctionSourceZ
//                                < ySectionCoordZ + 15 + 12) {
//                                jigsawJunctionsList.add(jigsawJunction);
//                            }
//                        }
//                    } else {
//                        structurePiecesList.add(structurePiece);
//                    }
//                }
//            });
//        }

        double[][][] zSliceDensities = new double[2][this.chunkSizeZ + 1][this.chunkSizeY + 1];

        for (int zCount = 0; zCount < this.chunkSizeZ + 1; ++zCount) {
            zSliceDensities[0][zCount] = new double[this.chunkSizeY + 1];
            this.fillNoiseColumn(zSliceDensities[0][zCount], chunkX * this.chunkSizeX, chunkZ * this.chunkSizeZ + zCount, chunk);
            zSliceDensities[1][zCount] = new double[this.chunkSizeY + 1];
        }

        ProtoChunk protoChunk = (ProtoChunk) chunk;
//        Heightmap oceanFloorHeightmap = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
//        Heightmap worldSurfaceHeightmap = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        ObjectListIterator<StructurePiece> structurePieceIterator = structurePiecesList.iterator();
        ObjectListIterator<JigsawJunction> jigsawJunctionIterator = jigsawJunctionsList.iterator();

        for (int dx = 0; dx < this.chunkSizeX; ++dx) {
            for (int dz = 0; dz < this.chunkSizeZ + 1; ++dz) {
                this.fillNoiseColumn(zSliceDensities[1][dz], chunkX * this.chunkSizeX + dx + 1, chunkZ * this.chunkSizeZ + dz, chunk);
            }

            for (int dz = 0; dz < this.chunkSizeZ; ++dz) {
                LevelChunkSection topSection = protoChunk.getOrCreateSection(protoChunk.getSectionsCount() - 1);
                topSection.acquire();

                for (int dy = this.chunkSizeY - 1; dy >= 0; --dy) {

                    //These doubles represent the corners of the 4x8x4 segments.
                    double y0x0z0 = zSliceDensities[0][dz][dy];
                    double y0x0z1 = zSliceDensities[0][dz + 1][dy];
                    double y0x1z0 = zSliceDensities[1][dz][dy];
                    double y0x1z1 = zSliceDensities[1][dz + 1][dy];
                    double y1x0z0 = zSliceDensities[0][dz][dy + 1];
                    double y1x0z1 = zSliceDensities[0][dz + 1][dy + 1];
                    double y1x1z0 = zSliceDensities[1][dz][dy + 1];
                    double y1x1z1 = zSliceDensities[1][dz + 1][dy + 1];

                    for (int chunkHeight = this.chunkHeight - 1; chunkHeight >= 0; --chunkHeight) {
                        int blockY = dy * this.chunkHeight + chunkHeight + chunk.getMinBuildHeight()/*!*/   /* + this.settings.get().noiseSettings().minY()*/;
                        int yLocal = blockY & 15;
                        int ySectionIDX = protoChunk.getSectionIndex(blockY);
                        if (protoChunk.getSectionIndex(topSection.bottomBlockY()) != ySectionIDX) {
                            topSection.release();
                            topSection = protoChunk.getOrCreateSection(ySectionIDX);
                            topSection.acquire();
                        }

                        double yFraction = (double) chunkHeight / (double) this.chunkHeight;
                        double yx0z0 = Mth.lerp(yFraction, y0x0z0, y1x0z0);
                        double yx0z1 = Mth.lerp(yFraction, y0x1z0, y1x1z0);
                        double yx1z0 = Mth.lerp(yFraction, y0x0z1, y1x0z1);
                        double yx1z1 = Mth.lerp(yFraction, y0x1z1, y1x1z1);

                        for (int xWidth = 0; xWidth < this.chunkWidth; ++xWidth) {
                            int xCoord = ySectionCoordX + dx * this.chunkWidth + xWidth;
                            int xLocal = xCoord & 15;
                            double xFraction = (double) xWidth / (double) this.chunkWidth; //Is this a good name?
                            double dx0 = Mth.lerp(xFraction, yx0z0, yx0z1);
                            double dx1 = Mth.lerp(xFraction, yx1z0, yx1z1);

                            for (int zWidth = 0; zWidth < this.chunkWidth; ++zWidth) {
                                int zCoord = ySectionCoordZ + dz * this.chunkWidth + zWidth;
                                int zLocal = zCoord & 15;
                                double zFraction = (double) zWidth / (double) this.chunkWidth; //Is this a good name?
                                double height = Mth.lerp(zFraction, dx0, dx1);
                                double density = Mth.clamp(height / 200.0D, -1.0D, 1.0D);

                                density = density / 2.0D - density * density * density / 24.0D;
//                                while (structurePieceIterator.hasNext()) {
//                                    StructurePiece structurePiece = structurePieceIterator.next();
//                                    BoundingBox structurePieceBoundingBox = structurePiece.getBoundingBox();
//                                    int xDistance = Math.max(0, Math.max(structurePieceBoundingBox.x0 - xCoord, xCoord - structurePieceBoundingBox.x1));
//                                    int yDistance = blockY - (structurePieceBoundingBox.y0 + (structurePiece instanceof PoolElementStructurePiece ?
//                                        ((PoolElementStructurePiece) structurePiece).getGroundLevelDelta() : 0));
//                                    int zDistance = Math.max(0, Math.max(structurePieceBoundingBox.z0 - zCoord, zCoord - structurePieceBoundingBox.z1));
//                                    density += getContribution(xDistance, yDistance, zDistance) * 0.8D;
//                                }
//
//                                structurePieceIterator.back(structurePiecesList.size());
//
//                                while (jigsawJunctionIterator.hasNext()) {
//                                    JigsawJunction jigsawJunction = jigsawJunctionIterator.next();
//                                    int xDistance = xCoord - jigsawJunction.getSourceX();
//                                    int yDistance = blockY - jigsawJunction.getSourceGroundY();
//                                    int zDistance = zCoord - jigsawJunction.getSourceZ();
//                                    density += getContribution(xDistance, yDistance, zDistance) * 0.4D;
//                                }
//
//                                jigsawJunctionIterator.back(jigsawJunctionsList.size());
                                BlockState baseState = this.generateBaseState(density, blockY);

                                //Light engine
//                                if (baseState != AIR) {
//                                    if (baseState.getLightEmission() != 0) {
//                                        mutableBlockPos.set(xCoord, blockY, zCoord);
//                                        cubePrimer.addLight(mutableBlockPos);
//                                    }
//
//                                getMinAndMaxNoise(height);

                                topSection.setBlockState(xLocal, yLocal, zLocal, baseState, false);


//                                topSection.setBlockState(xLocal, yLocal, zLocal, baseState, false);
//                                    oceanFloorHeightmap.update(xLocal, blockY, zLocal, baseState);
//                                    worldSurfaceHeightmap.update(xLocal, blockY, zLocal, baseState);
//                                }
                            }
                        }
                    }
                }
                topSection.release();
            }

            double[][] temp = zSliceDensities[0];
            zSliceDensities[0] = zSliceDensities[1];
            zSliceDensities[1] = temp;
        }
    }

    static double min = 1000;
    static double max = -11111;

    private void getMinAndMaxNoise(double noise) {
        if (noise < min) {
            min = noise;
            CubicChunks.LOGGER.info("Min height: " + min);
        }

        if (noise > max) {
            max = noise;
            CubicChunks.LOGGER.info("Max height: " + max);
        }
    }

    private static double getContribution(int xDistance, int yDistance, int zDistance) {
        int xIDX = xDistance + 12;
        int yIDX = yDistance + 12;
        int zIDX = zDistance + 12;

        int lookupTableSize = 24;
        if (xIDX >= 0 && xIDX < lookupTableSize) {
            if (yIDX >= 0 && yIDX < lookupTableSize) {
                return zIDX >= 0 && zIDX < lookupTableSize ? (double) BEARD_KERNEL[zIDX * lookupTableSize * lookupTableSize + xIDX * lookupTableSize + yIDX] : 0.0D;
            } else {
                return 0.0D;
            }
        } else {
            return 0.0D;
        }
    }

    private static double computeContribution(int x, int y, int z) {
        double d = (double) (x * x + z * z);
        double e = (double) y + 0.5D;
        double f = e * e;
        double g = Math.pow(2.718281828459045D, -(f / 16.0D + d / 16.0D));
        double h = -e * Mth.fastInvSqrt(f / 2.0D + d / 2.0D) / 2.0D;
        return h * g;
    }

    public int getGenDepth() {
        return this.height;
    }

    public int getSeaLevel() {
        return this.settings.get().seaLevel();
    }

    public List<MobSpawnSettings.SpawnerData> getMobsAt(Biome biome, StructureFeatureManager accessor, MobCategory group, BlockPos pos) {
        if (accessor.getStructureAt(pos, true, StructureFeature.SWAMP_HUT).isValid()) {
            if (group == MobCategory.MONSTER) {
                return StructureFeature.SWAMP_HUT.getSpecialEnemies();
            }

            if (group == MobCategory.CREATURE) {
                return StructureFeature.SWAMP_HUT.getSpecialAnimals();
            }
        }

        if (group == MobCategory.MONSTER) {
            if (accessor.getStructureAt(pos, false, StructureFeature.PILLAGER_OUTPOST).isValid()) {
                return StructureFeature.PILLAGER_OUTPOST.getSpecialEnemies();
            }

            if (accessor.getStructureAt(pos, false, StructureFeature.OCEAN_MONUMENT).isValid()) {
                return StructureFeature.OCEAN_MONUMENT.getSpecialEnemies();
            }

            if (accessor.getStructureAt(pos, true, StructureFeature.NETHER_BRIDGE).isValid()) {
                return StructureFeature.NETHER_BRIDGE.getSpecialEnemies();
            }
        }

        return super.getMobsAt(biome, accessor, group, pos);
    }

    public void spawnOriginalMobs(WorldGenRegion region) {
        if (true) {
            int centerX = region.getCenterX();
            int centerZ = region.getCenterZ();
            Biome biome = region.getBiome((new ChunkPos(centerX, centerZ)).getWorldPosition());
            WorldgenRandom worldgenRandom = new WorldgenRandom();
            worldgenRandom.setDecorationSeed(region.getSeed(), SectionPos.sectionToBlockCoord(centerX), SectionPos.sectionToBlockCoord(centerZ));
            NaturalSpawner.spawnMobsForChunkGeneration(region, biome, centerX, centerZ, worldgenRandom);
        }
    }
}
