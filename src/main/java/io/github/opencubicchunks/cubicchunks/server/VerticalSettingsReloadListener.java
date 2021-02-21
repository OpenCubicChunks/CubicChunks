package io.github.opencubicchunks.cubicchunks.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.gen.structure.CubicStructureConfiguration;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.feature.StructureFeature;

public class VerticalSettingsReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override public void onResourceManagerReload(ResourceManager manager) {
        Map<StructureFeature<?>, CubicStructureConfiguration> newMap = new HashMap<>();

        ResourceLocation location = new ResourceLocation(CubicChunks.MODID, "vertical_settings.json");
        try {
            Collection<Resource> verticalSettings = manager.getResources(location);

            for (Resource settings : verticalSettings) {
                try (Reader reader = new BufferedReader(new InputStreamReader(settings.getInputStream()))) {
                    JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();

                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        ResourceLocation structureID = new ResourceLocation(entry.getKey());

                        if (Registry.STRUCTURE_FEATURE.keySet().contains(structureID)) {
                            CubicStructureConfiguration.CubicStructureConfigurationBuilder cubicStructureConfigurationBuilder =
                                new CubicStructureConfiguration.CubicStructureConfigurationBuilder();

                            JsonObject data = entry.getValue().getAsJsonObject();

                            if (data.has("vertical_spacing")) {
                                cubicStructureConfigurationBuilder.setYSpacing(data.get("vertical_spacing").getAsInt());
                            }
                            if (data.has("vertical_separation")) {
                                cubicStructureConfigurationBuilder.setYSeparation(data.get("vertical_separation").getAsInt());
                            }
                            if (data.has("maxY")) {
                                cubicStructureConfigurationBuilder.setMaxY(data.get("maxY").getAsInt());
                            }
                            if (data.has("minY")) {
                                cubicStructureConfigurationBuilder.setMinY(data.get("maxY").getAsInt());
                            }

                            if (cubicStructureConfigurationBuilder.test(structureID.toString())) {
                                newMap.put(Registry.STRUCTURE_FEATURE.get(structureID), cubicStructureConfigurationBuilder.build());
                            }
                        } else {
                            CubicChunks.LOGGER.error("\"" + structureID.toString() + "\" was not found in the \"STRUCTURE_FEATURE\" registry, skipping vertical settings...");
                        }
                    }

                } catch (IOException e) {
                    CubicChunks.LOGGER.error("Could not parse vertical settings for: \"" + settings + "\". Entries in this file will not be added...");
                }
            }
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not get resources for: " + location.toString());
        }
        CubicStructureConfiguration.DATA_FEATURE_VERTICAL_SETTINGS = newMap;
    }

    @Override public ResourceLocation getFabricId() {
        return new ResourceLocation(CubicChunks.MODID, "vertical_settings_loader");
    }

    public static void registerVerticalSettingsReloadListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new VerticalSettingsReloadListener());
    }
}
