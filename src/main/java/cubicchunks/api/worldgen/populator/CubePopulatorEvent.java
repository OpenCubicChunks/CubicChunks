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
package cubicchunks.api.worldgen.populator;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event is fired when an {@link Cube} is populated in flat and custom
 * cubic world types. <br> This event is {@link Cancelable}. If canceled, cube
 * will remain untouched by populator. In that case only cube primer and
 * subscribed event handlers will affect cube population. This event is fired on
 * the {@link MinecraftForge#EVENT_BUS}.
 */
@Cancelable
public class CubePopulatorEvent extends Event {

    private final Cube cube;
    private final ICubicWorld world;

    public CubePopulatorEvent(ICubicWorld worldIn, Cube cubeIn) {
        super();
        cube = cubeIn;
        world = worldIn;
    }

    public Cube getCube() {
        return cube;
    }

    public ICubicWorld getWorld() {
        return world;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
