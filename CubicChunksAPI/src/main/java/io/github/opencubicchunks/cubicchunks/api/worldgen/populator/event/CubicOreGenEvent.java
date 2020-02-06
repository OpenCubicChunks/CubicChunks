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
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubicOreGenEvent extends Event {

    private final World world;
    private final Random rand;
    private final CubePos pos;

    public CubicOreGenEvent(World world, Random rand, CubePos pos) {
        this.world = world;
        this.rand = rand;
        this.pos = pos;
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public CubePos getPos() {
        return pos;
    }

    /**
     * This event is fired on the {@link MinecraftForge#ORE_GEN_BUS}.<br>
     **/
    public static class Pre extends CubicOreGenEvent {
        public Pre(World world, Random rand, CubePos pos) {
            super(world, rand, pos);
        }
    }

    /**
     * This event is fired on the {@link MinecraftForge#ORE_GEN_BUS}.<br>
     **/
    public static class Post extends CubicOreGenEvent {
        public Post(World world, Random rand, CubePos pos) {
            super(world, rand, pos);
        }
    }

    /**
     * This event is fired on the {@link MinecraftForge#ORE_GEN_BUS}.<br>
     **/
    @HasResult
    public static class GenerateMinable extends CubicOreGenEvent {

        private final IBlockState type;
        private final WorldGenerator generator;

        public GenerateMinable(World world, Random rand, WorldGenerator generator, CubePos pos, IBlockState type) {
            super(world, rand, pos);
            this.generator = generator;
            this.type = type;
        }

        public IBlockState getType() {
            return type;
        }

        public WorldGenerator getGenerator() {
            return generator;
        }
    }
}
