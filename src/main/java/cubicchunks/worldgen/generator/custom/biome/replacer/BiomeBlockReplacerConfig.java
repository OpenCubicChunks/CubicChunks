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
package cubicchunks.worldgen.generator.custom.biome.replacer;

import cubicchunks.api.worldgen.biome.CubicBiome;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Biome block replacer configuration.
 * <p>
 * This should be used in implementation of {@link IBiomeBlockReplacerProvider} to create {@link IBiomeBlockReplacer}
 * with the specified config. It should NOT be used directly in IBiomeBlockReplacer for performance reasons.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BiomeBlockReplacerConfig {

    // NOTE: this class is serialized to json. Don't change the fields.
    private Map<ResourceLocation, Object> defaults = new HashMap<>();
    private Map<ResourceLocation, Object> overrides = new HashMap<>();

    public void set(ResourceLocation location, Object value) {
        overrides.put(location, value);
    }

    public void set(String modid, String name, Object value) {
        overrides.put(new ResourceLocation(modid, name), value);
    }

    public void setDefault(ResourceLocation location, Object defaultValue) {
        defaults.put(location, defaultValue);
    }

    public void setDefault(String modid, String name, Object defaultValue) {
        defaults.put(new ResourceLocation(modid, name), defaultValue);
    }

    /* Double getters */
    public double getDouble(ResourceLocation location) {
        return ((Number) getValue(location)).doubleValue();
    }

    public double getDouble(String modid, String name) {
        return getDouble(new ResourceLocation(modid, name));
    }

    /* String getters */
    public String getString(ResourceLocation location) {
        Object v = getValue(location);
        if (v == null) {
            throw new NullPointerException();
        }
        return (String) v;
    }

    public IBlockState getBlockstate(String modid, String name) {
        return getBlockstate(new ResourceLocation(modid, name));
    }

    /* String getters */
    public IBlockState getBlockstate(ResourceLocation location) {
        Object v = getValue(location);
        if (v == null) {
            throw new NullPointerException();
        }
        return (IBlockState) v;
    }

    public String getString(String modid, String name) {
        return getString(new ResourceLocation(modid, name));
    }

    private Object getValue(ResourceLocation location) {
        if (overrides.containsKey(location)) {
            return overrides.get(location);
        }
        return this.defaults.get(location);
    }

    public void fillDefaults() {
        CubicBiome.REGISTRY.forEach(biome ->
                biome.getReplacerProviders().forEach(prov ->
                        prov.getPossibleConfigOptions().forEach(confOpt ->
                                setDefault(confOpt.getLocation(), confOpt.getDefaultValue()))
                )
        );
    }

    public static BiomeBlockReplacerConfig defaults() {
        BiomeBlockReplacerConfig conf = new BiomeBlockReplacerConfig();
        conf.fillDefaults();
        return conf;
    }

    public Map<ResourceLocation, Object> getDefaults() {
        return new HashMap<>(defaults);
    }

    public Map<ResourceLocation, Object> getOverrides() {
        return new HashMap<>(overrides);
    }
}
