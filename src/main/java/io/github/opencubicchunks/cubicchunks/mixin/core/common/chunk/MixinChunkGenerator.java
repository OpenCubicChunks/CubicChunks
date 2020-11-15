package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.OverworldBiomeSourceAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.biome.BiomeGetter;
import io.github.opencubicchunks.cubicchunks.world.biome.StripedBiomeSource;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.noise.module.source.Perlin;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator implements ICubeGenerator {

    @Mutable
    @Shadow
    @Final
    protected BiomeSource biomeSource;
    @Mutable
    @Shadow
    @Final
    protected BiomeSource runtimeBiomeSource;

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
    public void onGenerateStructures(RegistryAccess p_242707_1_, StructureFeatureManager p_242707_2_, ChunkAccess p_242707_3_, StructureManager p_242707_4_, long p_242707_5_, CallbackInfo ci) {
        ci.cancel();
    }


    @Inject(method = "createReferences", at = @At("HEAD"), cancellable = true)
    public void generateStructureStarts(WorldGenLevel p_235953_1_, StructureFeatureManager p_235953_2_, ChunkAccess p_235953_3_, CallbackInfo ci) {
        ci.cancel();
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

                        Biome biome = worldIn.getBiome(new BlockPos(blockX, 0, blockZ));

                        for (int dy = 0; dy < IBigCube.DIAMETER_IN_BLOCKS; dy++) {
                            int blockY = cube.getCubePos().minCubeY() + dy;
                            if (blockY == height) {
                                cube.setBlock(new BlockPos(dx, dy, dz), biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
                            } else if (blockY >= height - 4 && blockY < height) {
                                cube.setBlock(new BlockPos(dx, dy, dz), biome.getGenerationSettings().getSurfaceBuilderConfig().getUnderMaterial(), false);
                            } else if (blockY < height) {
                                cube.setBlock(new BlockPos(dx, dy, dz), Blocks.STONE.defaultBlockState(), false);
                            } else if (blockY <= CubicChunks.SEA_LEVEL) {
                                cube.setBlock(new BlockPos(dx, dy, dz), Blocks.WATER.defaultBlockState(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager) {
        int mainCubeX = region.getMainCubeX();
        int mainCubeY = region.getMainCubeY();
        int mainCubeZ = region.getMainCubeZ();
        int yEnd = Coords.cubeToMinBlock(mainCubeY);

        int xStart = Coords.cubeToMinBlock(mainCubeX);
        int yStart = Coords.cubeToMinBlock(mainCubeY);
        int zStart = Coords.cubeToMinBlock(mainCubeZ);

        //Y value stays 32
        CubeWorldGenRandom worldgenRandom = new CubeWorldGenRandom();

        //Get each individual column from a given cube no matter the size. Where y height is the same per column.
        //Feed the given columnMinPos into the feature decorators.
        for (int columnX = 0; columnX < IBigCube.DIAMETER_IN_SECTIONS; columnX++) {
            for (int columnZ = 0; columnZ < IBigCube.DIAMETER_IN_SECTIONS; columnZ++) {
                BlockPos columnMinPos = new BlockPos(xStart + (Coords.sectionToMinBlock(columnX)), yStart, zStart + (Coords.sectionToMinBlock(columnZ)));

                long seed = worldgenRandom.setDecorationSeed(region.getSeed(), columnMinPos.getX(), columnMinPos.getY(), columnMinPos.getZ());

                Biome biome = ((ChunkGenerator) (Object) this).getBiomeSource().getPrimaryBiome(Coords.cubeToSection(mainCubeX, columnX), Coords.cubeToSection(mainCubeZ, columnZ));
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