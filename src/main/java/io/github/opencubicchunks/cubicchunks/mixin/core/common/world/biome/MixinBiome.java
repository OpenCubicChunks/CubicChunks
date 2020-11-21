package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.biome;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.SetupCubeStructureStart;
import io.github.opencubicchunks.cubicchunks.world.biome.BiomeGetter;
import io.github.opencubicchunks.cubicchunks.world.gen.feature.CCFeatures;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;
import java.util.function.Supplier;

@Mixin(Biome.class)
public class MixinBiome implements BiomeGetter {
    @Shadow
    @Final
    private BiomeGenerationSettings generationSettings;

    @Shadow
    @Final
    private Map<Integer, List<StructureFeature<?>>> structuresByStep;


    private final Set<ResourceLocation> featureBlacklist = new HashSet<>();

    @Override
    public void generate(StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, CubeWorldGenRegion cubeWorldGenRegion, long seed, CubeWorldGenRandom worldgenRandom, BlockPos blockPos) {
        List<List<Supplier<ConfiguredFeature<?, ?>>>> list = this.generationSettings.features();

        for (int genStepIDX = 0; genStepIDX < GenerationStep.Decoration.values().length; ++genStepIDX) {
            int k = 0;
            if (structureFeatureManager.shouldGenerateFeatures()) {

                for (StructureFeature<?> structure : this.structuresByStep.getOrDefault(genStepIDX, Collections.emptyList())) {

                    worldgenRandom.setDecorationSeed(seed, k, genStepIDX);
                    int minSectionX = Coords.sectionToMinBlock(Coords.blockToSection(blockPos.getX()));
                    int minSectionY = Coords.sectionToMinBlock(Coords.blockToSection(blockPos.getY()));
                    int minSectionZ = Coords.sectionToMinBlock(Coords.blockToSection(blockPos.getZ()));

                    try {
                        structureFeatureManager.startsForFeature(SectionPos.of(blockPos), structure).forEach((structureStart) -> {
                            ((SetupCubeStructureStart) structureStart).placeInCube(cubeWorldGenRegion, structureFeatureManager, chunkGenerator, worldgenRandom, new BoundingBox(minSectionX, minSectionY, minSectionZ, minSectionX + 15, minSectionY + IBigCube.DIAMETER_IN_BLOCKS - 1, minSectionZ + 15), blockPos);
                        });
                    } catch (Exception e) {
                        CrashReport crashReport = CrashReport.forThrowable(e, "Structure Feature placement");
                        crashReport.addCategory("Feature").setDetail("Id", Registry.STRUCTURE_FEATURE.getKey(structure)).setDetail("Description", () -> {
                            return structure.toString();
                        });
                        throw new ReportedException(crashReport);
                    }
                }
            }

            if (featureBlacklist.isEmpty())
                getBlacklist(cubeWorldGenRegion);


            if (list.size() > genStepIDX) {
                for (Supplier<ConfiguredFeature<?, ?>> configuredFeatureSupplier : list.get(genStepIDX)) {
                    ConfiguredFeature<?, ?> configuredFeature = configuredFeatureSupplier.get();

                    ResourceLocation key = cubeWorldGenRegion.getLevel().getServer().registryAccess().registry(Registry.CONFIGURED_FEATURE_REGISTRY).get().getKey(configuredFeature);
                    if (key != null) {
                        if (key.equals(new ResourceLocation("lake_lava")))
                            configuredFeature = CCFeatures.CC_LAVA_LAKE;
                        else if (key.equals(new ResourceLocation("lake_water")))
                            configuredFeature = CCFeatures.CC_WATER_LAKE;
                    }

                    ConfiguredFeature<?, ?> configuredFeature1 = configuredFeature;

                    if (!featureBlacklist.contains(key)) {
                        try {
                            configuredFeature.place(cubeWorldGenRegion, chunkGenerator, worldgenRandom, blockPos);
                        } catch (Exception e) {
                            CrashReport crashReport2 = CrashReport.forThrowable(e, "Feature placement");
                            crashReport2.addCategory("Feature").setDetail("Id", key).setDetail("Config", configuredFeature.config).setDetail("Description", () -> {
                                return configuredFeature1.feature.toString();
                            });
                            CubicChunks.LOGGER.fatal(crashReport2.getFriendlyReport());
                            throw new ReportedException(crashReport2);
                        }
                    }
                }
            }
        }
    }

    //TODO: Remove this blacklist.
    private void getBlacklist(CubeWorldGenRegion cubeWorldGenRegion) {

        List<ResourceLocation> resourceLocationList = Arrays.asList(

                //Broken Features
                new ResourceLocation("ice_spike"), /**{@link net.minecraft.world.level.levelgen.feature.IceSpikeFeature}**/ //Handles its placement in its own class w/ a while loop.
                new ResourceLocation("spring_water"), //Requires similar 1.12 implementation, see: https://github.com/OpenCubicChunks/CubicWorldGen/blob/27de56d2f792513873584b2f8fd9f3082fb259ec/src/main/java/io/github/opencubicchunks/cubicchunks/cubicgen/customcubic/populator/DefaultDecorator.java#L331-L361
                new ResourceLocation("spring_lava"), //Requires similar 1.12 implementation, see: https://github.com/OpenCubicChunks/CubicWorldGen/blob/27de56d2f792513873584b2f8fd9f3082fb259ec/src/main/java/io/github/opencubicchunks/cubicchunks/cubicgen/customcubic/populator/DefaultDecorator.java#L331-L361
                new ResourceLocation("seagrass_simple"), //Requires Carving mask
                new ResourceLocation("fossil"),
                new ResourceLocation("desert_well"), //Iterates downwards in its placement
                new ResourceLocation("ice_patch"), //Iterates downwards in its placement


                //Ores //TODO: OPTIMIZE ORE GEN
                new ResourceLocation("ore_magma"),
                new ResourceLocation("ore_soul_sand"),
                new ResourceLocation("ore_gold_deltas"),
                new ResourceLocation("ore_quartz_deltas"),
                new ResourceLocation("ore_gold_nether"),
                new ResourceLocation("ore_quartz_nether"),
                new ResourceLocation("ore_gravel_nether"),
                new ResourceLocation("ore_blackstone"),
                new ResourceLocation("ore_dirt"),
                new ResourceLocation("ore_gravel"),
                new ResourceLocation("ore_granite"),
                new ResourceLocation("ore_diorite"),
                new ResourceLocation("ore_andesite"),
                new ResourceLocation("ore_coal"),
                new ResourceLocation("ore_iron"),
                new ResourceLocation("ore_gold_extra"),
                new ResourceLocation("ore_gold"),
                new ResourceLocation("ore_redstone"),
                new ResourceLocation("ore_diamond"),
                new ResourceLocation("ore_lapis"),
                new ResourceLocation("ore_infested"),
                new ResourceLocation("ore_emerald"),
                new ResourceLocation("ore_debris_large"),
                new ResourceLocation("ore_debris_small"),
                new ResourceLocation("ore_copper")
        );

        featureBlacklist.addAll(resourceLocationList);
    }
}
