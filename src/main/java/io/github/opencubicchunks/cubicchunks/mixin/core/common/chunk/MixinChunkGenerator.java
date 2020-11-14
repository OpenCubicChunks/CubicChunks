package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.biome.BiomeGetter;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator implements ICubeGenerator {

    @Shadow @Final protected BiomeSource runtimeBiomeSource;

    private final PerlinNoise gen1 = new PerlinNoise(new WorldgenRandom(42), createOctaveList());
    private final PerlinNoise gen2 = new PerlinNoise(new WorldgenRandom(4242), createOctaveList());
    private final PerlinNoise gen3 = new PerlinNoise(new WorldgenRandom(424242), createOctaveList());

    private static List<Integer> createOctaveList() {
        return Arrays.asList(3, 2, 1, 0);
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

    @Override
    public void makeBase(LevelAccessor worldIn, StructureFeatureManager var2, IBigCube cube) {
        // noiseAt = getValue2D(x, z, yDiscontinuityDistance, maxYDiscontinuityFactor)
        // getValue = getValue3D(x, y, z, yDiscontinuityDistance, maxYDiscontinuityFactor, is2dNoise)
        for (int dx = 0; dx < IBigCube.DIAMETER_IN_BLOCKS; dx++) {
            int blockX = cube.getCubePos().minCubeX() + dx;
            for (int dz = 0; dz < IBigCube.DIAMETER_IN_BLOCKS; dz++) {
                int blockZ = cube.getCubePos().minCubeZ() + dz;

                double v1 = gen1.getSurfaceNoiseValue(blockX * 0.0004567, blockZ * 0.0004567, 0, 0) * 4000;
                double v2 = gen2.getSurfaceNoiseValue(blockX * 0.0004567, blockZ * 0.0004567, 0, 0) * 4000;
                double v3 = gen3.getSurfaceNoiseValue(blockX * 0.0008567, blockZ * 0.0008567, 0, 0) * 20 + 0.5;
                int height = (int) Mth.clampedLerp(v1, v2, v3);
                for (int dy = 0; dy < IBigCube.DIAMETER_IN_BLOCKS; dy++) {
                    int blockY = cube.getCubePos().minCubeY() + dy;
                    if (blockY == height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                    } else if (blockY >= height - 4 && blockY < height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.DIRT.defaultBlockState(), false);
                    } else if (blockY < height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.STONE.defaultBlockState(), false);
                    } else if (blockY < 0) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.WATER.defaultBlockState(), false);
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
        int yStart = Coords.cubeToMinBlock(mainCubeY + 1);
        int yEnd = Coords.cubeToMinBlock(mainCubeY);

        int xStart = Coords.cubeToMinBlock(mainCubeX);
        int zStart = Coords.cubeToMinBlock(mainCubeZ);


        BlockPos blockPos = new BlockPos(xStart, 0, zStart);
        WorldgenRandom worldgenRandom = new WorldgenRandom();
        long seed = worldgenRandom.setDecorationSeed(region.getSeed(), xStart, zStart);

        Biome biome = ((ChunkGenerator)(Object)this).getBiomeSource().getPrimaryBiome(mainCubeX, mainCubeZ);
        try {
            ((BiomeGetter)(Object)biome).generate(structureManager, ((ChunkGenerator)(Object)this), region, seed, worldgenRandom, blockPos);
        } catch (Exception var14) {
            CrashReport crashReport = CrashReport.forThrowable(var14, "Biome decoration");
            crashReport.addCategory("Generation").setDetail("CubeX", mainCubeX).setDetail("CubeY", mainCubeY).setDetail("CubeZ", mainCubeZ).setDetail("Seed", seed).setDetail("Biome", biome);
            throw new ReportedException(crashReport);
        }
    }

    //    @Override public void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager) {
//        int mainCubeX = region.getMainCubeX();
//        int mainCubeY = region.getMainCubeY();
//        int mainCubeZ = region.getMainCubeZ();
//
//        int yStart = Coords.cubeToMinBlock(mainCubeY + 1);
//        int yEnd = Coords.cubeToMinBlock(mainCubeY);
//
//        Random r = new Random(mainCubeX * 678321 + mainCubeZ * 56392 + mainCubeY * 32894345);
//        int treeCount = Math.abs((int) (gen1.getValue(mainCubeX * 0.00354, 8765, mainCubeZ * 0.00354, 0, 0, false) * 12*50));
//        for (int i = 0; i < treeCount; i++) {
//            int x = Coords.cubeToMinBlock(mainCubeX) + r.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
//            int z = Coords.cubeToMinBlock(mainCubeZ) + r.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
//            BlockPos pos = new BlockPos(x, yStart, z);
//            if (!region.getBlockState(pos).isAir()) {
//                continue;
//            }
//            for (int y = yStart - 1; y >= yEnd; y--) {
//                pos = new BlockPos(x, y, z);
//                if (!region.getBlockState(pos).isAir()) {
//                    //Removed bc 1.16.2 has removed the config fields and creates them within the configuration instead
////                    BaseTreeFeatureConfig[] configs = {
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
////                            DefaultBiomeFeatures.field_230132_o_,
////                            DefaultBiomeFeatures.field_230133_p_,
////                            DefaultBiomeFeatures.BIRCH_TREE_CONFIG,
////                            DefaultBiomeFeatures.SWAMP_TREE_CONFIG,
////                            DefaultBiomeFeatures.SPRUCE_TREE_CONFIG,
////                            DefaultBiomeFeatures.FANCY_TREE_WITH_MORE_BEEHIVES_CONFIG,
////                    };
//                    Feature.TREE.configured(((new TreeConfiguration.TreeConfigurationBuilder(new SimpleStateProvider(Blocks.DARK_OAK_LOG.defaultBlockState()), new SimpleStateProvider(Blocks.DARK_OAK_LEAVES.defaultBlockState()), new DarkOakFoliagePlacer(UniformInt.fixed(0), UniformInt.fixed(0)), new DarkOakTrunkPlacer(6, 2, 1), new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty()))).maxWaterDepth(Integer.MAX_VALUE).heightmap(Heightmap.Types.MOTION_BLOCKING).ignoreVines().build())).place(region,
//                            (ChunkGenerator) (Object) this, r, pos.above());
//                }
//            }
//        }
//    }
}