/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.world;

import net.minecraft.nbt.NBTTagCompound;

/**
 * CubicChunks equivalent of {@link net.minecraftforge.event.world.ChunkDataEvent}
 */
public class CubeDataEvent extends CubeEvent {

    private final NBTTagCompound data;

    public CubeDataEvent(ICube cube, NBTTagCompound data) {
        super(cube);
        this.data = data;
    }

    public NBTTagCompound getData() {
        return data;
    }

    /**
     * CubicChunks equivalent of {@link net.minecraftforge.event.world.ChunkDataEvent.Load}
     */
    public static class Load extends CubeDataEvent {
        public Load(ICube cube, NBTTagCompound data) {
            super(cube, data);
        }
    }

    /**
     * CubicChunks equivalent of {@link net.minecraftforge.event.world.ChunkDataEvent.Save}
     */
    public static class Save extends CubeDataEvent {
        public Save(ICube cube, NBTTagCompound data) {
            super(cube, data);
        }
    }
}
