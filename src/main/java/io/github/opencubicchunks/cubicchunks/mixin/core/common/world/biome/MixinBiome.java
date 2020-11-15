package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.biome;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.biome.BiomeGetter;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(Biome.class)
public class MixinBiome implements BiomeGetter {
    @Shadow
    @Final
    private BiomeGenerationSettings generationSettings;

    @Override
    public void generate(StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, CubeWorldGenRegion worldGenRegion, long seed, WorldgenRandom worldgenRandom, BlockPos blockPos) {
        List<List<Supplier<ConfiguredFeature<?, ?>>>> list = this.generationSettings.features();
        int i = GenerationStep.Decoration.values().length;

        for (int genStepIDX = 0; genStepIDX < i; ++genStepIDX) {
//            if (structureFeatureManager.shouldGenerateFeatures()) {
//                List<StructureFeature<?>> list2 = (List)this.structuresByStep.getOrDefault(j, Collections.emptyList());

//                for(Iterator var13 = list2.iterator(); var13.hasNext(); ++k) {
//                    StructureFeature<?> structureFeature = (StructureFeature)var13.next();
//                    worldgenRandom.setFeatureSeed(l, k, j);
//                    int m = SectionPos.blockToSectionCoord(blockPos.getX());
//                    int n = SectionPos.blockToSectionCoord(blockPos.getZ());
//                    int o = SectionPos.sectionToBlockCoord(m);
//                    int p = SectionPos.sectionToBlockCoord(n);
//
//                    try {
//                        structureFeatureManager.startsForFeature(SectionPos.of(blockPos), structureFeature).forEach((structureStart) -> {
//                            structureStart.placeInChunk(worldGenRegion, structureFeatureManager, chunkGenerator, worldgenRandom, new BoundingBox(o, worldGenRegion.getMinBuildHeight() + 1, p, o + 15, worldGenRegion.getMaxBuildHeight(), p + 15), new ChunkPos(m, n));
//                        });
//                    } catch (Exception var21) {
//                        CrashReport crashReport = CrashReport.forThrowable(var21, "Feature placement");
//                        crashReport.addCategory("Feature").setDetail("Id", (Object)Registry.STRUCTURE_FEATURE.getKey(structureFeature)).setDetail("Description", () -> {
//                            return structureFeature.toString();
//                        });
//                        throw new ReportedException(crashReport);
//                    }
//                }
//            }

            final Set<ResourceLocation> featureIDWhitelist = new HashSet<>(Arrays.asList(
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

                    //Ores
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

            ));

            if (list.size() > genStepIDX) {
                for (Supplier<ConfiguredFeature<?, ?>> configuredFeatureSupplier : list.get(genStepIDX)) {
                    ConfiguredFeature<?, ?> configuredFeature = configuredFeatureSupplier.get();

                    ResourceLocation key = worldGenRegion.getLevel().getServer().registryAccess().registry(Registry.CONFIGURED_FEATURE_REGISTRY).get().getKey(configuredFeature);

                    if (featureIDWhitelist.contains(key)) {
                        try {
                            configuredFeature.place(worldGenRegion, chunkGenerator, worldgenRandom, blockPos);
                        } catch (Exception e) {
                            CrashReport crashReport2 = CrashReport.forThrowable(e, "Feature placement");
                            crashReport2.addCategory("Feature").setDetail("Id", Registry.FEATURE.getKey(configuredFeature.feature)).setDetail("Config", configuredFeature.config).setDetail("Description", () -> {
                                return configuredFeature.feature.toString();
                            });
                            throw new ReportedException(crashReport2);
                        }
                    }
                }

//                for(Iterator<Supplier<ConfiguredFeature<?, ?>>> var23 = (list.get(8)).iterator(); var23.hasNext(); ++k) {
//                    Supplier<ConfiguredFeature<?, ?>> supplier = var23.next();
//                    ConfiguredFeature<?, ?> configuredFeature = supplier.get();
//                    worldgenRandom.setFeatureSeed(seed, k, j);
//
//                    try {
//                        configuredFeature.place(worldGenRegion, chunkGenerator, worldgenRandom, blockPos);
//                    } catch (Exception var22) {
//                        CrashReport crashReport2 = CrashReport.forThrowable(var22, "Feature placement");
//                        crashReport2.addCategory("Feature").setDetail("Id", Registry.FEATURE.getKey(configuredFeature.feature)).setDetail("Config", configuredFeature.config).setDetail("Description", () -> {
//                            return configuredFeature.feature.toString();
//                        });
//                        throw new ReportedException(crashReport2);
//                    }
//                }
            }
        }
    }
}
