package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.block.Blocks;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.OctavesNoiseGenerator;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.foliageplacer.DarkOakFoliagePlacer;
import net.minecraft.world.gen.trunkplacer.DarkOakTrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator implements ICubeGenerator {

    private final OctavesNoiseGenerator gen1 = new OctavesNoiseGenerator(new SharedSeedRandom(42), createOctaveList());
    private final OctavesNoiseGenerator gen2 = new OctavesNoiseGenerator(new SharedSeedRandom(4242), createOctaveList());
    private final OctavesNoiseGenerator gen3 = new OctavesNoiseGenerator(new SharedSeedRandom(424242), createOctaveList());

    private static List<Integer> createOctaveList() {
        return Arrays.asList(3, 2, 1, 0);
    }
    // TODO: check which one is which
    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    public void onGenerateStructures(DynamicRegistries p_242707_1_, StructureManager p_242707_2_, IChunk p_242707_3_, TemplateManager p_242707_4_, long p_242707_5_, CallbackInfo ci) {

        ci.cancel();
    }


    @Inject(method = "createReferences", at = @At("HEAD"), cancellable = true)
    public void generateStructureStarts(ISeedReader p_235953_1_, StructureManager p_235953_2_, IChunk p_235953_3_, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "createBiomes", at = @At("HEAD"), cancellable = true)
    public void generateBiomes(Registry<Biome> p_242706_1_, IChunk chunkIn, CallbackInfo ci) {
        if (chunkIn instanceof IBigCube) {
            ci.cancel();
        }
    }

    @Override
    public void makeBase(IWorld worldIn, StructureManager var2, IBigCube cube) {
        // noiseAt = getValue2D(x, z, yDiscontinuityDistance, maxYDiscontinuityFactor)
        // getValue = getValue3D(x, y, z, yDiscontinuityDistance, maxYDiscontinuityFactor, is2dNoise)
        for (int dx = 0; dx < IBigCube.DIAMETER_IN_BLOCKS; dx++) {
            int blockX = cube.getCubePos().minCubeX() + dx;
            for (int dz = 0; dz < IBigCube.DIAMETER_IN_BLOCKS; dz++) {
                int blockZ = cube.getCubePos().minCubeZ() + dz;

                double v1 = gen1.getSurfaceNoiseValue(blockX * 0.004567, blockZ * 0.004567, 0, 0) * 400;
                double v2 = gen2.getSurfaceNoiseValue(blockX * 0.004567, blockZ * 0.004567, 0, 0) * 400;
                double v3 = gen3.getSurfaceNoiseValue(blockX * 0.008567, blockZ * 0.008567, 0, 0) * 20 + 0.5;
                int height = (int) MathHelper.clampedLerp(v1, v2, v3);
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

    @Override public void decorate(CubeWorldGenRegion region, StructureManager structureManager) {
        int mainCubeX = region.getMainCubeX();
        int mainCubeY = region.getMainCubeY();
        int mainCubeZ = region.getMainCubeZ();

        int yStart = Coords.cubeToMinBlock(mainCubeY + 1);
        int yEnd = Coords.cubeToMinBlock(mainCubeY);

        Random r = new Random(mainCubeX * 678321 + mainCubeZ * 56392 + mainCubeY * 32894345);
        int treeCount = Math.abs((int) (gen1.getValue(mainCubeX * 0.00354, 8765, mainCubeZ * 0.00354, 0, 0, false) * 12*50));
        for (int i = 0; i < treeCount; i++) {
            int x = Coords.cubeToMinBlock(mainCubeX) + r.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
            int z = Coords.cubeToMinBlock(mainCubeZ) + r.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
            BlockPos pos = new BlockPos(x, yStart, z);
            if (!region.getBlockState(pos).isAir(region, pos)) {
                continue;
            }
            for (int y = yStart - 1; y >= yEnd; y--) {
                pos = new BlockPos(x, y, z);
                if (!region.getBlockState(pos).isAir(region, pos)) {
                    //Removed bc 1.16.2 has removed the config fields and creates them within the configuration instead
//                    BaseTreeFeatureConfig[] configs = {
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
//                            DefaultBiomeFeatures.field_230132_o_,
//                            DefaultBiomeFeatures.field_230133_p_,
//                            DefaultBiomeFeatures.BIRCH_TREE_CONFIG,
//                            DefaultBiomeFeatures.SWAMP_TREE_CONFIG,
//                            DefaultBiomeFeatures.SPRUCE_TREE_CONFIG,
//                            DefaultBiomeFeatures.FANCY_TREE_WITH_MORE_BEEHIVES_CONFIG,
//                    };
                    Feature.TREE.configured(((new BaseTreeFeatureConfig.Builder(new SimpleBlockStateProvider(Blocks.DARK_OAK_LOG.defaultBlockState()), new SimpleBlockStateProvider(Blocks.DARK_OAK_LEAVES.defaultBlockState()), new DarkOakFoliagePlacer(FeatureSpread.fixed(0), FeatureSpread.fixed(0)), new DarkOakTrunkPlacer(6, 2, 1), new ThreeLayerFeature(1, 1, 0, 1, 2, OptionalInt.empty()))).maxWaterDepth(Integer.MAX_VALUE).heightmap(Heightmap.Type.MOTION_BLOCKING).ignoreVines().build())).place(region,
                            (ChunkGenerator) (Object) this, r, pos.above());
                }
            }
        }

    }
}