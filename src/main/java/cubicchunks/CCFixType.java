/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
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
