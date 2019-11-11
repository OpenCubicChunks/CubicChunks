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
package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PopulateCubeEvent extends CubeGeneratorEvent {

    private final World world;
    private final Random rand;
    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;
    private final boolean hasVillageGenerated;

    public PopulateCubeEvent(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
        super(((ICubicWorldServer) world).getCubeGenerator());
        this.world = world;
        this.rand = rand;
        this.cubeX = cubeX;
        this.cubeY = cubeY;
        this.cubeZ = cubeZ;
        this.hasVillageGenerated = hasVillageGenerated;
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public int getCubeX() {
        return cubeX;
    }

    public int getCubeY() {
        return cubeY;
    }

    public int getCubeZ() {
        return cubeZ;
    }

    public boolean isHasVillageGenerated() {
        return hasVillageGenerated;
    }

    public static class Pre extends PopulateCubeEvent {
        public Pre(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
        }
    }

    public static class Post extends PopulateCubeEvent {
        public Post(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
        }
    }

    @Event.HasResult
    public static class Populate extends PopulateCubeEvent {

        public PopulateChunkEvent.Populate.EventType getType() {
            return type;
        }

        private final PopulateChunkEvent.Populate.EventType type;

        public Populate(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated, PopulateChunkEvent.Populate.EventType type) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
            this.type = type;
        }
    }
}
