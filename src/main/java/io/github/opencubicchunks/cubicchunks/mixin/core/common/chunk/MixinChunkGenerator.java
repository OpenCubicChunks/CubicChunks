package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.SectionSizeCubeAccessWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.OverworldBiomeSourceAccess;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.biome.BiomeGetter;
import io.github.opencubicchunks.cubicchunks.world.biome.StripedBiomeSource;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.noise.module.source.Perlin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

@Mixin(ChunkGenerator.class)
public abstract class MixinChunkGenerator implements ICubeGenerator {

    @Mutable
    @Shadow
    @Final
    protected BiomeSource biomeSource;
    @Mutable
    @Shadow
    @Final
    protected BiomeSource runtimeBiomeSource;

    @Shadow
    @Final
    private StructureSettings settings;

    @Shadow
    protected abstract void generateStrongholds();

    @Shadow
    @Final
    private List<ChunkPos> strongholdPositions;


    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/levelgen/StructureSettings;J)V")
    private void switchBiomeSource(BiomeSource biomeSource, BiomeSource biomeSource2, StructureSettings structureSettings, long l, CallbackInfo ci) {
        if (System.getProperty("cubicchunks.debug.biomes", "false").equalsIgnoreCase("true")) {
            if (this.biomeSource instanceof OverworldBiomeSource)
                this.biomeSource = new StripedBiomeSource(((OverworldBiomeSourceAccess) this.biomeSource).getBiomes());
            if (this.runtimeBiomeSource instanceof OverworldBiomeSource)
                this.runtimeBiomeSource = new StripedBiomeSource(((OverworldBiomeSourceAccess) this.runtimeBiomeSource).getBiomes());
        }
    }


    // TODO: check which one is which
    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    public void onGenerateStructures(RegistryAccess registry, StructureFeatureManager featureManager, ChunkAccess chunkAccess, StructureManager manager, long seed, CallbackInfo ci) {
        if (!(chunkAccess instanceof IBigCube))
            return;
        ci.cancel();


        //TODO: Patch entity game crashes in order to spawn villages(village pieces spawn villagers)
        //TODO: Setup a 2D and 3D placement.

        IBigCube cube = (IBigCube) chunkAccess;

        CubePos cubePos = cube.getCubePos();

        Biome biome = this.biomeSource.getPrimaryBiome(cube.getCubePos().asSectionPos().getX(), cube.getCubePos().asSectionPos().getZ());
        this.createCCStructure(StructureFeatures.STRONGHOLD, registry, featureManager, cube, manager, seed, cubePos, biome);

        for (Supplier<ConfiguredStructureFeature<?, ?>> configuredStructureFeatureSupplier : biome.getGenerationSettings().structures()) {
            this.createCCStructure(configuredStructureFeatureSupplier.get(), registry, featureManager, cube, manager, seed, cubePos, biome);
        }
    }


    private void createCCStructure(ConfiguredStructureFeature<?, ?> configuredStructureFeature, RegistryAccess registryAccess, StructureFeatureManager structureFeatureManager, IBigCube cube, StructureManager structureManager, long seed, CubePos cubePos, Biome biome) {
        StructureStart<?> structureStart = structureFeatureManager.getStartForFeature(/*SectionPos.of(cube.getPos(), 0) We return null as a sectionPos Arg is not used in the method*/null, configuredStructureFeature.feature, cube);
        int i = structureStart != null ? structureStart.getReferences() : 0;
        StructureFeatureConfiguration structureFeatureConfiguration = this.settings.getConfig(configuredStructureFeature.feature);
        if (structureFeatureConfiguration != null) {
            StructureStart<?> structureStart2 = configuredStructureFeature.generate(registryAccess, ((ChunkGenerator) (Object) this), this.biomeSource, structureManager, seed, cubePos.asChunkPos(), biome, i, structureFeatureConfiguration);
            structureFeatureManager.setStartForFeature(/* SectionPos.of(cube.getPos(), 0) We return null as a sectionPos Arg is not used in the method*/null, configuredStructureFeature.feature, structureStart2, cube);
        }

    }


    @Inject(method = "createReferences", at = @At("HEAD"), cancellable = true)
    public void createReferences(WorldGenLevel worldGenLevel, StructureFeatureManager featureManager, ChunkAccess chunkAccess, CallbackInfo ci) {
        if (!(chunkAccess instanceof IBigCube))
            return;

        ci.cancel();

        IBigCube cube = (IBigCube) chunkAccess;

        CubeWorldGenRegion world = (CubeWorldGenRegion) worldGenLevel;

        int cubeX = world.getMainCubeX();
        int cubeY = world.getMainCubeY();
        int cubeZ = world.getMainCubeZ();

        int blockX = cubeToMinBlock(cubeX);
        int blockY = cubeToMinBlock(cubeY);
        int blockZ = cubeToMinBlock(cubeZ);

        for (int x = cubeX - 8 / IBigCube.DIAMETER_IN_SECTIONS; x <= cubeX + 8 / IBigCube.DIAMETER_IN_SECTIONS; ++x) {
            for (int y = cubeY - 8 / IBigCube.DIAMETER_IN_SECTIONS; y <= cubeY + 8 / IBigCube.DIAMETER_IN_SECTIONS; ++y) {
                for (int z = cubeZ - 8 / IBigCube.DIAMETER_IN_SECTIONS; z <= cubeZ + 8 / IBigCube.DIAMETER_IN_SECTIONS; ++z) {
                    long cubePosAsLong = CubePos.asLong(x, y, z);

                    for (StructureStart<?> structureStart : world.getCube(CubePos.of(x, y, z)).getAllCubeStructureStarts().values()) {
                        try {
                            if (structureStart != StructureStart.INVALID_START && structureStart.getBoundingBox().intersects(
                                    //We use a new Bounding Box and check if it intersects a given cube.
                                    new BoundingBox(blockX, blockY, blockZ,
                                            blockX + IBigCube.DIAMETER_IN_BLOCKS - 1,
                                            blockY + IBigCube.DIAMETER_IN_BLOCKS - 1,
                                            blockZ + IBigCube.DIAMETER_IN_BLOCKS - 1))) {
                                //The First Param is a SectionPos arg that is not used anywhere so we make it null.
                                featureManager.addReferenceForFeature(null, structureStart.getFeature(), cubePosAsLong, cube);
                                DebugPackets.sendStructurePacket(world, structureStart);
                            }
                        } catch (Exception e) {
                            CrashReport crashReport = CrashReport.forThrowable(e, "Generating structure reference");
                            CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
                            crashReportCategory.setDetail("Id", () -> Registry.STRUCTURE_FEATURE.getKey(structureStart.getFeature()).toString());
                            crashReportCategory.setDetail("Name", () -> structureStart.getFeature().getFeatureName());
                            crashReportCategory.setDetail("Class", () -> structureStart.getFeature().getClass().getCanonicalName());
                            throw new ReportedException(crashReport);
                        }
                    }
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "findNearestMapFeature(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/feature/StructureFeature;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;", cancellable = true)
    private void do3DLocateStructure(ServerLevel serverLevel, StructureFeature<?> structureFeature, BlockPos blockPos, int radius, boolean skipExistingChunks, CallbackInfoReturnable<BlockPos> cir) {
        if (!this.biomeSource.canGenerateStructure(structureFeature)) {
            cir.setReturnValue(null);
        } else if (structureFeature == StructureFeature.STRONGHOLD) {
            this.generateStrongholds();
            BlockPos blockPos2 = null;
            double d = Double.MAX_VALUE;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for (ChunkPos chunkPos : this.strongholdPositions) {
                mutable.set(SectionPos.sectionToBlockCoord(chunkPos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkPos.z, 8));
                double e = mutable.distSqr(blockPos);
                if (blockPos2 == null) {
                    blockPos2 = new BlockPos(mutable);
                    d = e;
                } else if (e < d) {
                    blockPos2 = new BlockPos(mutable);
                    d = e;
                }
            }

            cir.setReturnValue(blockPos2);
        } else {
            StructureFeatureConfiguration structureFeatureConfiguration = this.settings.getConfig(structureFeature);
            cir.setReturnValue(structureFeatureConfiguration == null ? null : structureFeature.getNearestGeneratedFeature(serverLevel, serverLevel.structureFeatureManager(), blockPos, radius, skipExistingChunks, serverLevel.getSeed(), structureFeatureConfiguration));
        }
    }

    @Inject(method = "createBiomes", at = @At("HEAD"), cancellable = true)
    public void generateBiomes(Registry<Biome> registry, ChunkAccess chunkIn, CallbackInfo ci) {
        /* This can only be a  CubePrimer at this point due to the inject in MixinChunkStatus#cubicChunksBiome  */
        IBigCube iCube = (IBigCube)chunkIn;
        CubePos cubePos = ((IBigCube) chunkIn).getCubePos();
        ((CubePrimer)iCube).setCubeBiomes(new CubeBiomeContainer(registry, cubePos, this.runtimeBiomeSource));
        ci.cancel();
    }

    private Perlin perlin1 = null;
    private Perlin perlin2 = null;
    private Perlin perlin3 = null;
    private final double frequencyDivisor = 1;

    private void setSeed(long seed) {
        if (perlin1 == null) {
            this.perlin1 = new Perlin();
            this.perlin1.setSeed((int) seed);
            this.perlin1.setFrequency(0.01 / frequencyDivisor);
        }
        if (perlin2 == null) {
            this.perlin2 = new Perlin();
            this.perlin2.setSeed((int) seed + 1111);
            this.perlin2.setFrequency(0.01 / frequencyDivisor);
        }
        if (perlin3 == null) {
            this.perlin3 = new Perlin();
            this.perlin3.setSeed((int) seed + 1598968687);
            this.perlin3.setFrequency(0.01138 / frequencyDivisor);
        }
    }


    private static final float[] BIOME_WEIGHTS = Util.make(new float[25], (fs) -> {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                float f = 10.0F / Mth.sqrt((float) (i * i + j * j) + 0.2F);
                fs[i + 2 + (j + 2) * 5] = f;
            }
        }

    });

    @Override
    public void makeBase(LevelAccessor worldIn, StructureFeatureManager var2, IBigCube cube) {
        setSeed(1000);
        double perlin1Max = perlin1.getMaxValue();
        double perlin2Max = perlin2.getMaxValue();
        double perlin3Max = perlin3.getMaxValue();

        int size = IBigCube.DIAMETER_IN_BLOCKS / 4 + 1;
        float[] densities = new float[size * size];

        int[] storedHeights = new int[IBigCube.DIAMETER_IN_BLOCKS * IBigCube.DIAMETER_IN_BLOCKS];

        CubePrimer cubeAbove = new CubePrimer(CubePos.of(cube.getCubePos().getX(), cube.getCubePos().getY() + 1, cube.getCubePos().getZ()), UpgradeData.EMPTY, worldIn);
        SectionSizeCubeAccessWrapper cubeWrapper = new SectionSizeCubeAccessWrapper(cube, cubeAbove);

        fillNoise(cube, perlin1Max, perlin2Max, perlin3Max, size, densities);

        setBaseBlocks(cube, size, densities, storedHeights);
        setBaseBlocks(cubeAbove, size, densities, storedHeights);

        buildSurface(worldIn, cube, cubeWrapper, storedHeights);
    }

    private void buildSurface(LevelAccessor worldIn, IBigCube cube, SectionSizeCubeAccessWrapper cubeWrapper, int[] storedHeights) {
        for (int x = 0; x < IBigCube.DIAMETER_IN_BLOCKS; x++) {
            for (int z = 0; z < IBigCube.DIAMETER_IN_BLOCKS; z++) {
                int realX = cube.getCubePos().minCubeX() + (x);
                int realZ = cube.getCubePos().minCubeZ() + (z);

                int height = storedHeights[x + z * IBigCube.DIAMETER_IN_BLOCKS];

                Biome biome = worldIn.getBiome(new BlockPos(realX, 0, realZ));

                cubeWrapper.setLocalSectionPos(blockToSection(x), blockToSection(z));

                ConfiguredSurfaceBuilder<?> configuredSurfaceBuilder = biome.getGenerationSettings().getSurfaceBuilder().get();
                configuredSurfaceBuilder.initNoise(1000);
                int surfaceHeight = height - cubeToMinBlock(cube.getCubePos().getY()) + 1;
                if (surfaceHeight >= 0 && surfaceHeight < 2 * IBigCube.DIAMETER_IN_BLOCKS)
                    configuredSurfaceBuilder.apply(worldIn.getRandom(), cubeWrapper, biome, realX, realZ,
                            surfaceHeight + 1, 1,
                            Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), CubicChunks.SEA_LEVEL - cubeToMinBlock(cube.getCubePos().getY()) + 1,
                            1000);
            }
        }
    }

    private void setBaseBlocks(IBigCube cube, int size, float[] densities, int[] storedHeights) {
        for (int sectionX = 0; sectionX < size - 1; sectionX++) {
            for (int sectionZ = 0; sectionZ < size - 1; sectionZ++) {

                float d00 = densities[sectionX + sectionZ * size];
                float d01 = densities[sectionX + (sectionZ + 1) * size];
                float d10 = densities[(sectionX + 1) + sectionZ * size];
                float d11 = densities[(sectionX + 1) + (sectionZ + 1) * size];

                for (int x = 0; x < 4; x++) {
                    float dx0 = (float) Mth.lerp(x * 0.25, d00, d10);
                    float dx1 = (float) Mth.lerp(x * 0.25, d01, d11);
                    int dx = sectionX * 4 + x;
                    int blockX = cube.getCubePos().minCubeX() + (dx);


                    for (int z = 0; z < 4; z++) {
                        int height = (int) Mth.lerp(z * 0.25, dx0, dx1);
                        int dz = sectionZ * 4 + z;
                        int blockZ = cube.getCubePos().minCubeZ() + (dz);

                        storedHeights[dx + dz * IBigCube.DIAMETER_IN_BLOCKS] = height;

                        for (int dy = 0; dy < IBigCube.DIAMETER_IN_BLOCKS; dy++) {
                            int blockY = cube.getCubePos().minCubeY() + dy;

                            LevelChunkSection cubeSection = cube.getCubeSections()[blockToIndex(dx, dy, dz)];
                            if (cubeSection == null) {
                                cube.getCubeSections()[blockToIndex(dx, dy, dz)] = cubeSection = new LevelChunkSection(blockToSection(blockY));
                            }

                            if (blockY < height) {
                                cubeSection.setBlockState(dx & 0xF, dy & 0xF, dz & 0xF,
                                        Blocks.STONE.defaultBlockState(), false);
                            } else if (blockY <= CubicChunks.SEA_LEVEL) {
                                cubeSection.setBlockState(dx & 0xF, dy & 0xF, dz & 0xF,
                                        Blocks.WATER.defaultBlockState(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillNoise(IBigCube cube, double perlin1Max, double perlin2Max, double perlin3Max, int size, float[] densities) {
        for (int dx = 0; dx < size; dx++) {
            int blockX = cube.getCubePos().minCubeX() + (dx * 4);
            for (int dz = 0; dz < size; dz++) {
                int blockZ = cube.getCubePos().minCubeZ() + (dz * 4);

                Biome biome = ((ChunkGenerator) (Object) this).getBiomeSource().getNoiseBiome(blockX >> 2, 0, blockZ >> 2);

                float mainHeight = biome.getDepth();
                float totalDepth = 0.0F;
                float totalScale = 0.0F;
                float totalWeight = 0.0F;

                for (int biomeX = -2; biomeX <= 2; ++biomeX) {
                    for (int biomeZ = -2; biomeZ <= 2; ++biomeZ) {
                        Biome biome2 = ((ChunkGenerator) (Object) this).getBiomeSource().getNoiseBiome((blockX >> 2) + biomeX, 0, (blockZ >> 2) + biomeZ);
                        float biomeDepth = biome2.getDepth();
                        float biomeScale = biome2.getScale();

                        float w = biomeDepth > mainHeight ? 0.5F : 1.0F;
                        float x = w * BIOME_WEIGHTS[biomeX + 2 + (biomeZ + 2) * 5] / (biomeDepth + 2.0F);
                        totalDepth += biomeDepth * x;
                        totalScale += biomeScale * x;
                        totalWeight += x;
                    }
                }

                float biomeHeightVariation = totalScale / totalWeight * 2.4F * 64 + 32;
                float biomeBaseHeight = totalDepth / totalWeight * 17 + CubicChunks.SEA_LEVEL;


                double v1 = (perlin1.getValue(blockX, blockZ, 0) * 2 - perlin1Max) * biomeHeightVariation + biomeBaseHeight;
                double v2 = (perlin2.getValue(blockX, blockZ, 0) * 2 - perlin2Max) * biomeHeightVariation + biomeBaseHeight;
                double v3 = (perlin3.getValue(blockX, blockZ, 0) * 2 - perlin3Max) * 20 + 0.5;

                float height = (float) Mth.clampedLerp(v1, v2, v3);

                if (height < biomeBaseHeight)
                    height = (height - biomeBaseHeight) * 0.25F + biomeBaseHeight;

                densities[dx + dz * size] = height;
            }
        }
    }

    @Override
    public void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager) {
        int mainCubeX = region.getMainCubeX();
        int mainCubeY = region.getMainCubeY();
        int mainCubeZ = region.getMainCubeZ();

        int xStart = cubeToMinBlock(mainCubeX);
        int yStart = cubeToMinBlock(mainCubeY);
        int zStart = cubeToMinBlock(mainCubeZ);

        //Y value stays 32
        CubeWorldGenRandom worldgenRandom = new CubeWorldGenRandom();

        //Get each individual column from a given cube no matter the size. Where y height is the same per column.
        //Feed the given columnMinPos into the feature decorators.
        for (int columnX = 0; columnX < IBigCube.DIAMETER_IN_SECTIONS; columnX++) {
            for (int columnZ = 0; columnZ < IBigCube.DIAMETER_IN_SECTIONS; columnZ++) {
                BlockPos columnMinPos = new BlockPos(xStart + (sectionToMinBlock(columnX)), yStart, zStart + (sectionToMinBlock(columnZ)));

                long seed = worldgenRandom.setDecorationSeed(region.getSeed(), columnMinPos.getX(), columnMinPos.getY(), columnMinPos.getZ());

                Biome biome = ((ChunkGenerator) (Object) this).getBiomeSource().getPrimaryBiome(cubeToSection(mainCubeX, columnX), cubeToSection(mainCubeZ, columnZ));
                try {
                    ((BiomeGetter) (Object) biome).generate(structureManager, ((ChunkGenerator) (Object) this), region, seed, worldgenRandom, columnMinPos);
                } catch (Exception e) {
                    CrashReport crashReport = CrashReport.forThrowable(e, "Biome decoration");
                    crashReport.addCategory("Generation").setDetail("CubeX", mainCubeX).setDetail("CubeY", mainCubeY).setDetail("CubeZ", mainCubeZ).setDetail("Seed", seed).setDetail("Biome", biome);
                    throw new ReportedException(crashReport);
                }
            }
        }
    }
}