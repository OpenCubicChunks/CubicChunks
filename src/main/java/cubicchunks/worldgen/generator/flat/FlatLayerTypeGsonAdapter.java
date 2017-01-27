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

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.common.base.Optional;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

@SuppressWarnings({"unchecked", "rawtypes"})
public class FlatLayerTypeGsonAdapter extends TypeAdapter<Layer> {

    @Override
    public void write(JsonWriter out, Layer value) throws IOException {
        out.beginObject();
        out.name("fromY");
        out.value(value.fromY);
        out.name("toY");
        out.value(value.toY);
        out.name("blockRegistryName");
        String blockRegistryName = Block.REGISTRY.getNameForObject(value.blockState.getBlock()).toString();
        out.value(blockRegistryName);
        value.blockState.getProperties().forEach((p, v) -> {
            try {
                out.name(p.getName());
                out.value(getValueName(p, v));
            } catch (IOException e) {
                throw new UncheckedIOException("Input error while converting to Json a BlockState instance of block " + blockRegistryName
                        + " for config of flat cube type world.", e);
            }
        });
        out.endObject();
    }

    private String getValueName(IProperty property, Comparable v) {
        return property.getName(v);
    }

    @Override
    public Layer read(JsonReader in) throws IOException {
        in.beginObject();
        in.nextName();
        int fromY = in.nextInt();
        in.nextName();
        int toY = in.nextInt();
        in.nextName();
        Block block = Block.getBlockFromName(in.nextString());
        IBlockState blockState = block.getBlockState().getBaseState();
        while (in.hasNext()) {
            IProperty property = block.getBlockState().getProperty(in.nextName());
            blockState = blockState.withProperty(property, findPropertyValueByName(property, in.nextString()));
        }
        Layer layer = new Layer(fromY, toY, blockState);
        in.endObject();
        return layer;
    }

    private Comparable findPropertyValueByName(IProperty property, String valueIn) {
        Optional<Comparable> value = property.parseValue(valueIn);
        if (value.isPresent()) {
            return value.get();
        } else {
            for (Object v : property.getAllowedValues()) {
                if (isValueEqualTo(property, (Comparable) v, valueIn)) {
                    return (Comparable) v;
                }
            }
        }
        return null;
    }

    private boolean isValueEqualTo(IProperty property, Comparable value, String valueIn) {
        return getValueName(property, value).equals(valueIn);
    }
}
