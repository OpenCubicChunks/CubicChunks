package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.block.Blocks;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.OctavesNoiseGenerator;
import net.minecraft.world.gen.feature.BaseTreeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
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
    @Inject(method = "func_235954_a_", at = @At("HEAD"), cancellable = true)
    public void onGenerateStructures(StructureManager p_235954_1_, IChunk p_235954_2_, TemplateManager p_235954_3_, long p_235954_4_, CallbackInfo ci) {

        ci.cancel();
    }


    @Inject(method = "func_235953_a_", at = @At("HEAD"), cancellable = true)
    public void generateStructureStarts(IWorld p_235953_1_, StructureManager p_235953_2_, IChunk p_235953_3_, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "generateBiomes", at = @At("HEAD"), cancellable = true)
    public void generateBiomes(IChunk chunkIn, CallbackInfo ci) {
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

                double v1 = gen1.noiseAt(blockX * 0.004567, blockZ * 0.004567, 0, 0) * 400;
                double v2 = gen2.noiseAt(blockX * 0.004567, blockZ * 0.004567, 0, 0) * 400;
                double v3 = gen3.noiseAt(blockX * 0.008567, blockZ * 0.008567, 0, 0) * 20 + 0.5;
                int height = (int) MathHelper.clampedLerp(v1, v2, v3);
                for (int dy = 0; dy < IBigCube.DIAMETER_IN_BLOCKS; dy++) {
                    int blockY = cube.getCubePos().minCubeY() + dy;
                    if (blockY == height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.GRASS_BLOCK.getDefaultState(), false);
                    } else if (blockY >= height - 4 && blockY < height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.DIRT.getDefaultState(), false);
                    } else if (blockY < height) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.STONE.getDefaultState(), false);
                    } else if (blockY < 0) {
                        cube.setBlock(new BlockPos(dx, dy, dz), Blocks.WATER.getDefaultState(), false);
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
                    BaseTreeFeatureConfig[] configs = {
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.OAK_TREE_CONFIG,
                            DefaultBiomeFeatures.field_230132_o_,
                            DefaultBiomeFeatures.field_230133_p_,
                            DefaultBiomeFeatures.BIRCH_TREE_CONFIG,
                            DefaultBiomeFeatures.SWAMP_TREE_CONFIG,
                            DefaultBiomeFeatures.SPRUCE_TREE_CONFIG,
                            DefaultBiomeFeatures.FANCY_TREE_WITH_MORE_BEEHIVES_CONFIG,
                    };
                    Feature.field_236291_c_.withConfiguration(configs[r.nextInt(configs.length)]).func_236265_a_(region, structureManager,
                            (ChunkGenerator) (Object) this, r, pos.up());
                }
            }
        }

    }
}
