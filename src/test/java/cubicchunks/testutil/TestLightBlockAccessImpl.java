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
package cubicchunks.testutil;

import static cubicchunks.testutil.LightingMatchers.pos;

import com.google.common.collect.Lists;
import cubicchunks.lighting.ILightBlockAccess;
import cubicchunks.util.Coords;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestLightBlockAccessImpl implements ILightBlockAccess {

    private final int size;
    @Nonnull private final Map<BlockPos, Integer> emittedLightBlock = new HashMap<>();
    @Nonnull private final Map<BlockPos, Integer> lightValuesSky = new HashMap<>();
    @Nonnull private final Map<BlockPos, Integer> lightValuesBlock = new HashMap<>();
    @Nonnull private final Map<BlockPos, Integer> opacities = new HashMap<>();

    public TestLightBlockAccessImpl(int size) {
        this.size = size;
    }

    @Override public int getBlockLightOpacity(BlockPos pos) {
        Integer opacity = opacities.get(pos);
        return opacity == null ? 0 : opacity;
    }

    @Override public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        Integer light;
        if (lightType == EnumSkyBlock.BLOCK) {
            light = lightValuesBlock.get(pos);
        } else {
            light = lightValuesSky.get(pos);
        }
        return light == null ? 0 : light;
    }

    @Override public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
        if (lightType == EnumSkyBlock.BLOCK) {
            lightValuesBlock.put(pos, val);
        } else {
            lightValuesSky.put(pos, val);
        }
    }

    @Override public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
        Integer ret;
        if (type == EnumSkyBlock.BLOCK) {
            ret = this.emittedLightBlock.get(pos);
        } else {
            ret = canSeeSky(pos) ? 15 : 0;
        }
        return ret == null ? 0 : ret;
    }

    @Override public int getTopBlockY(BlockPos pos) {
        for (BlockPos p = new BlockPos(pos.getX(), size, pos.getZ()); p.getY() >= -size; p = p.down()) {
            if (getBlockLightOpacity(p) > 0) {
                return p.getY();
            }
        }
        return Coords.NO_HEIGHT;
    }

    public void setBlockLightSource(BlockPos pos, int value) {
        this.emittedLightBlock.put(pos, value);
    }

    public void setOpacity(BlockPos pos, int opacity) {
        this.opacities.put(pos, opacity);
    }

    public static LightBlockAccessBuilder lightAccess(int size) {
        return new LightBlockAccessBuilder(size);
    }


    public static LightBlockAccessBuilder lightAccess(int size, int lightXSize, int lightYSize, int lightZSize) {
        return new LightBlockAccessBuilder(size, lightXSize, lightYSize, lightZSize);
    }

    public static class LightBlockAccessBuilder {

        private TestLightBlockAccessImpl access;
        private int size;
        private int lightXSize;
        private int lightYSize;
        private int lightZSize;

        LightBlockAccessBuilder(int size) {
            this(size, size, size, size);
        }

        public LightBlockAccessBuilder(int size, int lightXSize, int lightYSize, int lightZSize) {
            this.access = new TestLightBlockAccessImpl(size);
            this.size = size;
            this.lightXSize = lightXSize;
            this.lightYSize = lightYSize;
            this.lightZSize = lightZSize;
        }

        public LightBlockAccessBuilder withFullBlockLight(BlockPos... pos) {
            for (BlockPos p : pos) {
                access.setBlockLightSource(p, 15);
            }
            return this;
        }

        public LightBlockAccessBuilder withBlockLight(int value, BlockPos... pos) {
            for (BlockPos p : pos) {
                access.setBlockLightSource(p, value);
            }
            return this;
        }

        public LightBlockAccessBuilder withOpaque(BlockPos... pos) {
            for (BlockPos p : pos) {
                access.setOpacity(p, 255);
            }
            return this;
        }

        public LightBlockAccessBuilder withTransparent(BlockPos... pos) {
            for (BlockPos p : pos) {
                access.setOpacity(p, 0);
            }
            return this;
        }

        public LightBlockAccessBuilder currentHeightsForInitSkyLight() {
            BlockPos.getAllInBox(pos(-lightXSize, -lightYSize, -lightZSize), pos(lightXSize, lightYSize, lightZSize)).forEach(pos ->
                    access.setLightFor(EnumSkyBlock.SKY, pos, access.getEmittedLight(pos, EnumSkyBlock.SKY)));
            return this;
        }

        public TestLightBlockAccessImpl make() {
            return access;
        }
    }
}
