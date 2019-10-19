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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DecorateCubeBiomeEvent extends Event {

    private final World world;
    private final Random rand;
    private final CubePos cubePos;

    public DecorateCubeBiomeEvent(World world, Random rand, CubePos cubePos) {
        this.world = world;
        this.rand = rand;
        this.cubePos = cubePos;
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public CubePos getCubePos() {
        return cubePos;
    }

    /**
     * This event is fired before a cube is decorated with a biome feature.
     */
    public static class Pre extends DecorateCubeBiomeEvent {
        public Pre(World world, Random rand, CubePos cubePos) {
            super(world, rand, cubePos);
        }
    }

    /**
     * This event is fired after a cube is decorated with a biome feature.
     */
    public static class Post extends DecorateCubeBiomeEvent {
        public Post(World world, Random rand, CubePos cubePos) {
            super(world, rand, cubePos);
        }
    }

    /**
     * This event is fired when a cube is decorated with a biome feature.
     * <p>
     * You can set the result to DENY to prevent the default biome decoration.
     */
    @Event.HasResult
    public static class Decorate extends DecorateCubeBiomeEvent {

        private final DecorateBiomeEvent.Decorate.EventType type;
        @Nullable private final BlockPos placementPos;

        public Decorate(World world, Random rand, CubePos cubePos, @Nullable BlockPos placementPos,
                DecorateBiomeEvent.Decorate.EventType type) {
            super(world, rand, cubePos);
            this.type = type;
            this.placementPos = placementPos;
        }

        public DecorateBiomeEvent.Decorate.EventType getType() {
            return type;
        }

        /**
         * This may be anywhere inside the 2x2x2 cube area for generation.
         * To get the original chunk position of the generation before a random location was chosen, use {@link #getCubePos()} ()}.
         *
         * @return the position used for original decoration, or null if it is not specified.
         */
        @Nullable public BlockPos getPlacementPos() {
            return this.placementPos;
        }
    }

}
