

package io.github.opencubicchunks.cubicchunks.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PrimaryLevelData;

@SuppressWarnings("deprecation")
public class GenDataCommand {

    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "gendata";
        List<String> modIdList = new ArrayList<>();
        FabricLoader.getInstance().getAllMods().forEach(modContainer -> {
            String modId = modContainer.getMetadata().getId();
            if (!modId.contains("fabric"))
                modIdList.add(modId);
        });

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString).then(Commands.argument("modid", StringArgumentType.string()).suggests((ctx, sb) -> SharedSuggestionProvider.suggest(modIdList.stream(), sb)).executes(cs -> {
            GenDataCommand.createBiomeDatapack(cs.getArgument("modid", String.class), cs);
            return 1;
        })));
        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    public static void createBiomeDatapack(String modId, CommandContext<CommandSourceStack> commandSource) {
        List<Biome> biomeList = new ArrayList<>();
        boolean stopSpamFlag = false;
        Path dataPackPath = dataPackPath(commandSource.getSource().getLevel().getServer().getWorldPath(LevelResource.DATAPACK_DIR), modId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        RegistryAccess manager = commandSource.getSource().getServer().registryAccess();
        Registry<Biome> biomeRegistry = manager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<ConfiguredFeature<?, ?>> featuresRegistry = manager.registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY);
        Registry<ConfiguredStructureFeature<?, ?>> structuresRegistry = manager.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        Registry<ConfiguredWorldCarver<?>> carverRegistry = manager.registryOrThrow(Registry.CONFIGURED_CARVER_REGISTRY);
        Registry<ConfiguredSurfaceBuilder<?>> surfaceBuilderRegistry = manager.registryOrThrow(Registry.CONFIGURED_SURFACE_BUILDER_REGISTRY);
        Registry<StructureProcessorList> structureProcessorRegistry = manager.registryOrThrow(Registry.PROCESSOR_LIST_REGISTRY);

        createConfiguredSurfaceBuilderJson(modId, dataPackPath, gson, surfaceBuilderRegistry);
        createConfiguredFeatureJson(modId, dataPackPath, gson, featuresRegistry);
        createConfiguredCarverJson(modId, dataPackPath, gson, carverRegistry);
        createConfiguredStructureJson(modId, dataPackPath, gson, structuresRegistry);
        createProcessorListJson(modId, dataPackPath, gson, structureProcessorRegistry);
        createBiomeJsonAndPackMcMeta(modId, commandSource, biomeList, stopSpamFlag, dataPackPath, gson, biomeRegistry, featuresRegistry, structuresRegistry, carverRegistry, surfaceBuilderRegistry);
        Function<WorldGenSettings, DataResult<JsonElement>> dimensionGeneratorSettingsCodec = JsonOps.INSTANCE.withEncoder(WorldGenSettings.CODEC);

        DataResult<JsonElement> jsonResult = dimensionGeneratorSettingsCodec.apply(((PrimaryLevelData) commandSource.getSource().getLevel().getLevelData()).worldGenSettings());

        try {
            Path sbPath = worldImportJsonPath(dataPackPath, "yes");
            Files.createDirectories(sbPath.getParent());
            Files.write(sbPath, gson.toJson(jsonResult.get().left().get()).getBytes());
        } catch (IOException e) {

        }

    }


    private static Path worldImportJsonPath(Path path, String jsonName) {
        return path.resolve("import/" + jsonName + ".json");
    }

    private static void createBiomeJsonAndPackMcMeta(String modId, CommandContext<CommandSourceStack> commandSource, List<Biome> biomeList, boolean stopSpamFlag, Path dataPackPath, Gson gson, Registry<Biome> biomeRegistry, Registry<ConfiguredFeature<?, ?>> featuresRegistry, Registry<ConfiguredStructureFeature<?, ?>> structuresRegistry, Registry<ConfiguredWorldCarver<?>> carverRegistry, Registry<ConfiguredSurfaceBuilder<?>> surfaceBuilderRegistry) {
        for (Map.Entry<ResourceKey<Biome>, Biome> biome : biomeRegistry.entrySet()) {
            String biomeKey = Objects.requireNonNull(biomeRegistry.getResourceKey(biome.getValue())).get().location().toString();
            if (biomeKey.contains(modId)) {
                biomeList.add(biome.getValue());
            }
        }

        if (biomeList.size() > 0) {
            for (Biome biome : biomeList) {
                ResourceLocation key = biomeRegistry.getKey(biome);
                if (key != null) {
                    Path biomeJsonPath = biomeJsonPath(dataPackPath, key, modId);
                    Function<Supplier<Biome>, DataResult<JsonElement>> biomeCodec = JsonOps.INSTANCE.withEncoder(Biome.CODEC);
                    try {
//                        if (!Files.exists(biomeJsonPath)) {
                        Files.createDirectories(biomeJsonPath.getParent());
                        Optional<JsonElement> optional = (biomeCodec.apply(() -> biome).result());
                        if (optional.isPresent()) {
                            JsonElement root = optional.get();
                            JsonArray features = new JsonArray();
                            for (List<Supplier<ConfiguredFeature<?, ?>>> list : biome.getGenerationSettings().features()) {
                                JsonArray stage = new JsonArray();
                                for (Supplier<ConfiguredFeature<?, ?>> feature : list) {
                                    featuresRegistry.getResourceKey(feature.get()).ifPresent(featureKey -> stage.add(featureKey.location().toString()));
                                }
                                features.add(stage);
                            }
                            root.getAsJsonObject().add("features", features);
                            String surfaceBuilder = surfaceBuilderRegistry.getResourceKey(biome.getGenerationSettings().getSurfaceBuilder().get()).get().location().toString();
                            root.getAsJsonObject().addProperty("surface_builder", surfaceBuilder);

                            JsonObject carvers = new JsonObject();
                            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                                JsonArray stage = new JsonArray();
                                for (Supplier<ConfiguredWorldCarver<?>> carver : biome.getGenerationSettings().getCarvers(step)) {
                                    carverRegistry.getResourceKey(carver.get()).ifPresent(carverKey -> stage.add(carverKey.location().toString()));
                                }
                                if (stage.size() > 0) {
                                    carvers.add(step.getSerializedName(), stage);
                                }
                            }
                            root.getAsJsonObject().add("carvers", carvers);
                            JsonArray starts = new JsonArray();
                            for (Supplier<ConfiguredStructureFeature<?, ?>> start : biome.getGenerationSettings().structures()) {
                                structuresRegistry.getResourceKey(start.get()).ifPresent(structureKey -> starts.add(structureKey.location().toString()));
                            }
                            root.getAsJsonObject().add("starts", starts);
                            Files.write(biomeJsonPath, gson.toJson(root).getBytes());
                        }
//                        }
                    } catch (IOException e) {
                        if (!stopSpamFlag) {
                            commandSource.getSource().sendSuccess(new TranslatableComponent("commands.gendata.failed", modId).withStyle(text -> text.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))), false);
                            stopSpamFlag = true;
                        }
                    }
                }
            }

            try {
                createPackMCMeta(dataPackPath, modId);
            } catch (IOException e) {
                commandSource.getSource().sendSuccess(new TranslatableComponent("commands.gendata.mcmeta.failed", modId).withStyle(text -> text.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))), false);
            }

            Component filePathText = (new TextComponent(dataPackPath.toString())).withStyle(ChatFormatting.UNDERLINE).withStyle(text -> text.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN)).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, dataPackPath.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("commands.gendata.hovertext"))));

            commandSource.getSource().sendSuccess(new TranslatableComponent("commands.gendata.success", commandSource.getArgument("modid", String.class), filePathText), false);
        } else {
            commandSource.getSource().sendSuccess(new TranslatableComponent("commands.gendata.listisempty", modId).withStyle(text -> text.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))), false);
        }
    }

    private static void createConfiguredFeatureJson(String modId, Path dataPackPath, Gson gson, Registry<ConfiguredFeature<?, ?>> featuresRegistry) {
        for (Map.Entry<ResourceKey<ConfiguredFeature<?, ?>>, ConfiguredFeature<?, ?>> feature : featuresRegistry.entrySet()) {
            Function<Supplier<ConfiguredFeature<?, ?>>, DataResult<JsonElement>> featureCodec = JsonOps.INSTANCE.withEncoder(ConfiguredFeature.CODEC);

            ConfiguredFeature<?, ?> configuredFeature = feature.getValue();
            if (feature.getKey().location().toString().contains(modId)) {
                if (configuredFeature != null) {
                    if (Objects.requireNonNull(featuresRegistry.getResourceKey(configuredFeature)).toString().contains(modId)) {
                        Optional<JsonElement> optional = (featureCodec.apply(() -> configuredFeature).result());
                        if (optional.isPresent()) {
                            try {
                                Path cfPath = configuredFeatureJsonPath(dataPackPath, Objects.requireNonNull(featuresRegistry.getKey(configuredFeature)), modId);
                                Files.createDirectories(cfPath.getParent());

                                Files.write(cfPath, gson.toJson(optional.get()).getBytes());
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createConfiguredSurfaceBuilderJson(String modId, Path dataPackPath, Gson gson, Registry<ConfiguredSurfaceBuilder<?>> surfaceBuildersRegistry) {
        for (Map.Entry<ResourceKey<ConfiguredSurfaceBuilder<?>>, ConfiguredSurfaceBuilder<?>> surfaceBuilder : surfaceBuildersRegistry.entrySet()) {
            Function<Supplier<ConfiguredSurfaceBuilder<?>>, DataResult<JsonElement>> surfaceBuilderCodec = JsonOps.INSTANCE.withEncoder(ConfiguredSurfaceBuilder.CODEC);

            ConfiguredSurfaceBuilder<?> configuredSurfaceBuilder = surfaceBuilder.getValue();
            if (surfaceBuilder.getKey().location().toString().contains(modId)) {
                if (configuredSurfaceBuilder != null) {
                    if (Objects.requireNonNull(surfaceBuildersRegistry.getResourceKey(configuredSurfaceBuilder)).toString().contains(modId)) {
                        Optional<JsonElement> optional = (surfaceBuilderCodec.apply(() -> configuredSurfaceBuilder).result());
                        if (optional.isPresent()) {
                            try {
                                Path sbPath = configuredSurfaceBuilderJsonPath(dataPackPath, Objects.requireNonNull(surfaceBuildersRegistry.getKey(configuredSurfaceBuilder)), modId);
                                Files.createDirectories(sbPath.getParent());
                                Files.write(sbPath, gson.toJson(optional.get()).getBytes());
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createConfiguredCarverJson(String modId, Path dataPackPath, Gson gson, Registry<ConfiguredWorldCarver<?>> carverRegistry) {
        for (Map.Entry<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> carver : carverRegistry.entrySet()) {
            Function<Supplier<ConfiguredWorldCarver<?>>, DataResult<JsonElement>> carverCodec = JsonOps.INSTANCE.withEncoder(ConfiguredWorldCarver.CODEC);

            ConfiguredWorldCarver<?> configuredCarver = carver.getValue();
            if (carver.getKey().location().toString().contains(modId)) {
                if (configuredCarver != null) {
                    if (Objects.requireNonNull(carverRegistry.getResourceKey(configuredCarver)).toString().contains(modId)) {
                        Optional<JsonElement> optional = (carverCodec.apply(() -> configuredCarver).result());
                        if (optional.isPresent()) {
                            try {
                                Path carverPath = configuredCarverJsonPath(dataPackPath, Objects.requireNonNull(carverRegistry.getKey(configuredCarver)), modId);
                                Files.createDirectories(carverPath.getParent());
                                Files.write(carverPath, gson.toJson(optional.get()).getBytes());
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createConfiguredStructureJson(String modId, Path dataPackPath, Gson gson, Registry<ConfiguredStructureFeature<?, ?>> structureRegistry) {
        for (Map.Entry<ResourceKey<ConfiguredStructureFeature<?, ?>>, ConfiguredStructureFeature<?, ?>> structure : structureRegistry.entrySet()) {
            Function<Supplier<ConfiguredStructureFeature<?, ?>>, DataResult<JsonElement>> structureCodec = JsonOps.INSTANCE.withEncoder(ConfiguredStructureFeature.CODEC);

            ConfiguredStructureFeature<?, ?> configuredStructure = structure.getValue();
            if (structure.getKey().location().toString().contains(modId)) {
                if (configuredStructure != null) {
                    if (Objects.requireNonNull(structureRegistry.getResourceKey(configuredStructure)).toString().contains(modId)) {
                        Optional<JsonElement> optional = (structureCodec.apply(() -> configuredStructure).result());
                        if (optional.isPresent()) {
                            try {
                                Path structurePath = configuredStructureFeatureJsonPath(dataPackPath, Objects.requireNonNull(structureRegistry.getKey(configuredStructure)), modId);
                                Files.createDirectories(structurePath.getParent());
                                Files.write(structurePath, gson.toJson(optional.get()).getBytes());
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createProcessorListJson(String modId, Path dataPackPath, Gson gson, Registry<StructureProcessorList> structureProcessorRegistry) {
        for (Map.Entry<ResourceKey<StructureProcessorList>, StructureProcessorList> processor : structureProcessorRegistry.entrySet()) {
            Function<Supplier<StructureProcessorList>, DataResult<JsonElement>> processorCodec = JsonOps.INSTANCE.withEncoder(StructureProcessorType.LIST_CODEC);

            StructureProcessorList processorList = processor.getValue();
            if (processor.getKey().location().toString().contains(modId)) {
                if (processorList != null) {
                    if (Objects.requireNonNull(structureProcessorRegistry.getResourceKey(processorList)).toString().contains(modId)) {
                        Optional<JsonElement> optional = (processorCodec.apply(() -> processorList).result());
                        if (optional.isPresent()) {
                            try {
                                Path processorListPath = configuredProceesorListPath(dataPackPath, Objects.requireNonNull(structureProcessorRegistry.getKey(processorList)), modId);
                                Files.createDirectories(processorListPath.getParent());
                                Files.write(processorListPath, gson.toJson(optional.get()).getBytes());
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static Path configuredFeatureJsonPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/configured_feature/" + identifier.getPath() + ".json");
    }

    private static Path configuredSurfaceBuilderJsonPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/configured_surface_builder/" + identifier.getPath() + ".json");
    }

    private static Path configuredCarverJsonPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/configured_carver/" + identifier.getPath() + ".json");
    }

    private static Path configuredStructureFeatureJsonPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/configured_structure_feature/" + identifier.getPath() + ".json");
    }

    private static Path configuredProceesorListPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/processor_list/" + identifier.getPath() + ".json");
    }

    private static Path biomeJsonPath(Path path, ResourceLocation identifier, String modId) {
        return path.resolve("data/" + modId + "/worldgen/biome/" + identifier.getPath() + ".json");
    }

    private static Path dataPackPath(Path path, String modId) {
        return path.resolve("gendata/" + modId + "-custom");
    }

    //Generate the pack.mcmeta file required for datapacks.
    private static void createPackMCMeta(Path dataPackPath, String modID) throws IOException {
        String fileString = "{\n" +
                "\t\"pack\":{\n" +
                "\t\t\"pack_format\": 6,\n" +
                "\t\t\"description\": \"Custom biome datapack for " + modID + ".\"\n" +
                "\t}\n" +
                "}\n";

        Files.write(dataPackPath.resolve("pack.mcmeta"), fileString.getBytes());
    }
}

