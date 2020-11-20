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
import net.minecraft.data.BuiltinRegistries;
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


    private final Set<ResourceLocation> featureIDWhitelist = new HashSet<>();

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

            if (featureIDWhitelist.isEmpty())
                getWhitelist(cubeWorldGenRegion);


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

                    if (featureIDWhitelist.contains(key) || genStepIDX == GenerationStep.Decoration.VEGETAL_DECORATION.ordinal() || genStepIDX == GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal()) {
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

    private void getWhitelist(CubeWorldGenRegion cubeWorldGenRegion) {

        List<ResourceLocation> resourceLocationList = Arrays.asList(
                //Trees
                new ResourceLocation("oak"),
                new ResourceLocation("dark_oak"),
                new ResourceLocation("birch"),
                new ResourceLocation("acacia"),
                new ResourceLocation("spruce"),
                new ResourceLocation("pine"),
                new ResourceLocation("jungle_tree"),
                new ResourceLocation("fancy_oak"),
                new ResourceLocation("jungle_tree_no_vine"),
                new ResourceLocation("mega_jungle_tree"),
                new ResourceLocation("mega_spruce"),
                new ResourceLocation("mega_pine"),
                new ResourceLocation("super_birch_bees_0002"),
                new ResourceLocation("swamp_tree"),
                new ResourceLocation("jungle_bush"),
                new ResourceLocation("oak_bees_0002"),
                new ResourceLocation("oak_bees_002"),
                new ResourceLocation("oak_bees_005"),
                new ResourceLocation("birch_bees_0002"),
                new ResourceLocation("birch_bees_002"),
                new ResourceLocation("birch_bees_005"),
                new ResourceLocation("fancy_oak_bees_0002"),
                new ResourceLocation("fancy_oak_bees_002"),
                new ResourceLocation("fancy_oak_bees_005"),
                new ResourceLocation("oak_badlands"),
                new ResourceLocation("spruce_snowy"),
                new ResourceLocation("flower_warm"),
                new ResourceLocation("flower_default"),
                new ResourceLocation("flower_forest"),
                new ResourceLocation("flower_swamp"),
                new ResourceLocation("flower_plain"),
                new ResourceLocation("flower_plain_decorated"),
                new ResourceLocation("forest_flower_vegetation_common"),
                new ResourceLocation("forest_flower_vegetation"),

                //Trees(Random Selectors)
                new ResourceLocation("forest_flower_trees"),
                new ResourceLocation("taiga_vegetation"),
                new ResourceLocation("trees_shattered_savanna"),
                new ResourceLocation("trees_savanna"),
                new ResourceLocation("birch_tall"),
                new ResourceLocation("trees_birch"),
                new ResourceLocation("trees_mountain_edge"),
                new ResourceLocation("trees_mountain"),
                new ResourceLocation("trees_water"),
                new ResourceLocation("birch_other"),
                new ResourceLocation("plain_vegetation"),
                new ResourceLocation("trees_jungle_edge"),
                new ResourceLocation("trees_giant_spruce"),
                new ResourceLocation("trees_giant"),
                new ResourceLocation("trees_jungle"),
                new ResourceLocation("dark_forest_vegetation_brown"),
                new ResourceLocation("dark_forest_vegetation_red"),
                new ResourceLocation("warm_ocean_vegetation"),
                new ResourceLocation("forest_flower_vegetation_common"),
                new ResourceLocation("mushroom_field_vegetation"),

                //Random Patches
                new ResourceLocation("patch_fire"),
                new ResourceLocation("patch_soul_fire"),
                new ResourceLocation("patch_brown_mushroom"),
                new ResourceLocation("patch_red_mushroom"),
                new ResourceLocation("patch_crimson_roots"),
                new ResourceLocation("patch_sunflower"),
                new ResourceLocation("patch_pumpkin"),
                new ResourceLocation("patch_taiga_grass"),
                new ResourceLocation("patch_berry_bush"),
                new ResourceLocation("patch_grass_plain"),
                new ResourceLocation("patch_grass_forest"),
                new ResourceLocation("patch_grass_badlands"),
                new ResourceLocation("patch_grass_savanna"),
                new ResourceLocation("patch_grass_normal"),
                new ResourceLocation("patch_grass_taiga_2"),
                new ResourceLocation("patch_grass_taiga"),
                new ResourceLocation("patch_grass_jungle"),
                new ResourceLocation("patch_dead_bush_2"),
                new ResourceLocation("patch_dead_bush"),
                new ResourceLocation("patch_dead_bush_badlands"),
                new ResourceLocation("patch_melon"),
                new ResourceLocation("patch_berry_sparse"),
                new ResourceLocation("patch_berry_decorated"),
                new ResourceLocation("patch_waterlilly"),
                new ResourceLocation("patch_tall_grass_2"),
                new ResourceLocation("patch_tall_grass"),
                new ResourceLocation("patch_large_fern"),
                new ResourceLocation("patch_cactus"),
                new ResourceLocation("patch_cactus_desert"),
                new ResourceLocation("patch_cactus_decorated"),
                new ResourceLocation("patch_sugar_cane_swamp"),
                new ResourceLocation("patch_sugar_cane_desert"),
                new ResourceLocation("patch_sugar_cane_badlands"),
                new ResourceLocation("patch_sugar_cane"),

                //Lakes
                new ResourceLocation("lake_lava"),
                new ResourceLocation("lake_water")

//                    Ores //TODO: OPTIMIZE ORE GEN
//                    new ResourceLocation("ore_magma"),
//                    new ResourceLocation("ore_soul_sand"),
//                    new ResourceLocation("ore_gold_deltas"),
//                    new ResourceLocation("ore_quartz_deltas"),
//                    new ResourceLocation("ore_gold_nether"),
//                    new ResourceLocation("ore_quartz_nether"),
//                    new ResourceLocation("ore_gravel_nether"),
//                    new ResourceLocation("ore_blackstone"),
//                    new ResourceLocation("ore_dirt"),
//                    new ResourceLocation("ore_gravel"),
//                    new ResourceLocation("ore_granite"),
//                    new ResourceLocation("ore_diorite"),
//                    new ResourceLocation("ore_andesite"),
//                    new ResourceLocation("ore_coal"),
//                    new ResourceLocation("ore_iron"),
//                    new ResourceLocation("ore_gold_extra"),
//                    new ResourceLocation("ore_gold"),
//                    new ResourceLocation("ore_redstone"),
//                    new ResourceLocation("ore_diamond"),
//                    new ResourceLocation("ore_lapis"),
//                    new ResourceLocation("ore_infested"),
//                    new ResourceLocation("ore_emerald"),
//                    new ResourceLocation("ore_debris_large"),
//                    new ResourceLocation("ore_debris_small"),
//                    new ResourceLocation("ore_copper")

        );

        for (ResourceLocation keyFromRegistry : cubeWorldGenRegion.getLevel().getServer().registryAccess().registry(Registry.CONFIGURED_FEATURE_REGISTRY).get().keySet()) {
            if (keyFromRegistry.getNamespace().equals("byg"))
                CubicChunks.LOGGER.info("BYG: " + keyFromRegistry);

            if (keyFromRegistry.toString().contains("tree"))
                featureIDWhitelist.add(keyFromRegistry);
        }
        CubicChunks.LOGGER.info("==================================Finished----------------================--");


        featureIDWhitelist.addAll(resourceLocationList);
    }
}
