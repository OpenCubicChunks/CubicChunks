/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2020 OpenCubicChunks
 *  Copyright (c) 2015-2020 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class MixinWorld implements ICubicWorldInternal {

    @Shadow @Final protected static Logger LOGGER;

    @Shadow public abstract WorldInfo getWorldInfo();

    @Shadow public abstract WorldType getWorldType();

    @Shadow public abstract int getActualHeight();

    private boolean isCubicWorld;
    private int minY = 0, maxY = 256;

    /**
     * @author Barteks2x
     * @reason Overwrite isYOutOfBounds to always return false. All height checks will be done immediately when needed, with World context
     */
    @Overwrite
    public static boolean isYOutOfBounds(int y) {
        return false;
    }

    @Override public void initCubicWorldCommon() {
        LOGGER.info("Initializing cubic world {} ({})", this.getWorldInfo().getWorldName(), this);
        this.isCubicWorld = true;
    }

    @Override public boolean isCubicWorld() {
        return isCubicWorld;
    }

    @Override public int getMinHeight() {
        return minY;
    }

    @Override public int getMaxHeight() {
        return maxY;
    }
}
