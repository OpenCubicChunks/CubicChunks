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
package cubicchunks.worldgen.generator.flat;

import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cubicchunks.CubicChunks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class FlatGeneratorSettings {

    public TreeMap<Integer, Layer> layers = new TreeMap<Integer, Layer>();

    public FlatGeneratorSettings() {
        addLayer(CubicChunks.MIN_BLOCK_Y + 1, Blocks.BEDROCK.getDefaultState());
        addLayer(-8, Blocks.STONE.getDefaultState());
        addLayer(-1, Blocks.DIRT.getDefaultState());
        addLayer(0, Blocks.GRASS.getDefaultState());
    }

    public void addLayer(int toY, IBlockState block) {
        int fromY = CubicChunks.MIN_BLOCK_Y;
        if (layers.floorEntry(toY) != null) {
            fromY = layers.floorEntry(toY).getValue().toY;
        }
        layers.put(fromY, new Layer(fromY, toY, block));
    }

    public String toJson() {
        Gson gson = createGsonBuilder();
        return gson.toJson(this);
    }

    public static FlatGeneratorSettings fromJson(String json) {
        if (json.isEmpty()) {
            return defaults();
        }
        Gson gson = createGsonBuilder();
        return gson.fromJson(json, FlatGeneratorSettings.class);
    }

    private static Gson createGsonBuilder() {
        return new GsonBuilder().registerTypeAdapter(Layer.class, new FlatLayerTypeGsonAdapter()).serializeSpecialFloatingPointValues().create();
    }

    public static FlatGeneratorSettings defaults() {
        return new FlatGeneratorSettings();
    }
}
