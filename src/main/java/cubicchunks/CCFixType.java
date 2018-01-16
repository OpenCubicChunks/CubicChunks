package cubicchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixType;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class CCFixType implements IFixType {

    private static Map<WorldType, CCFixType> generatorSettingsWorldTypes = new HashMap<>();

    public static void addFixableWorldType(WorldType type) {
        generatorSettingsWorldTypes.put(type, new CCFixType());
    }

    public static CCFixType forWorldType(String type) {
        return generatorSettingsWorldTypes.get(WorldType.parseWorldType(type));
    }

    public static void registerWalkers() {
        for (WorldType fixableType : generatorSettingsWorldTypes.keySet()) {
            FMLCommonHandler.instance().getDataFixer().registerVanillaWalker(FixTypes.LEVEL, (fixer, compound, versionId) -> {
                String worldTypeName = compound.getString("generatorName");
                WorldType worldType = WorldType.parseWorldType(worldTypeName);
                if (worldType == fixableType) {
                    return fixer.process(forWorldType(worldTypeName), compound, versionId);
                }
                return compound;
            });
        }

    }
}
