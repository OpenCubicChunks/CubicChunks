/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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
package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.event;

import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

/**
 * This event is fired on initialization of cubic terrain generators during
 * cubic type world load.<br> This event is not {@link Cancelable}. This event
 * is fired on the {@link MinecraftForge#TERRAIN_GEN_BUS}.
 *
 */
public class InitCubicStructureGeneratorEvent extends Event {

    private final EventType type;
    private final ICubicStructureGenerator originalGen;
    private ICubicStructureGenerator newGen;
    private World world;

    @Deprecated // to be removed!
    public InitCubicStructureGeneratorEvent(EventType type, ICubicStructureGenerator original) {
        this.type = type;
        this.originalGen = original;
        this.setNewGen(original);
    }

    public InitCubicStructureGeneratorEvent(EventType type, ICubicStructureGenerator original, World world) {
        this.type = type;
        this.originalGen = original;
        this.world = world;
        this.setNewGen(original);
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    public EventType getType() {
        return type;
    }

    public ICubicStructureGenerator getOriginalGen() {
        return originalGen;
    }

    public ICubicStructureGenerator getNewGen() {
        return newGen;
    }

    public void setNewGen(ICubicStructureGenerator newGen) {
        this.newGen = newGen;
    }
}
