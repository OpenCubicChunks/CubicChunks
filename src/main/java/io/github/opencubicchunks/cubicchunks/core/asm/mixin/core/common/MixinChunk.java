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

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Chunk.class)
public class MixinChunk {

    @Shadow @Final public static ChunkSection EMPTY_SECTION;
    @Shadow @Final private ChunkPos pos;
    @Shadow @Final private ChunkSection[] sections;

    private boolean isCubic;
    private Int2ObjectMap<Cube> cubeMap;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/biome/BiomeContainer;Lnet/minecraft/util/palette/UpgradeData;Lnet/minecraft/world/ITickList;Lnet/minecraft/world/ITickList;J[Lnet/minecraft/world/chunk/ChunkSection;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void afterConstruct(World world, ChunkPos chunkPosIn, BiomeContainer biomeContainerIn,
                                UpgradeData upgradeDataIn, ITickList<Block> tickBlocksIn, ITickList<Fluid> tickFluidsIn,
                                long inhabitedTimeIn, ChunkSection[] sectionsIn, Consumer<Chunk> postLoadConsumerIn,
                                CallbackInfo ci) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        this.isCubic = true;
        this.cubeMap = new Int2ObjectOpenHashMap<>();

        if (sections != null) {
            for (int i = 0; i < sections.length; i++) {
                this.setChunkSection(this.sections, i, sections[i]);
            }
        }
    }

    @Redirect(method = {
        "read",
        "getFluidState(III)Lnet/minecraft/fluid/IFluidState;",
        "getBlockState"
    }, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=get"
    ))
    private ChunkSection getChunkSection(ChunkSection[] sections, int index) {
        if (!isCubic) {
            return sections[index];
        }
        Cube cube = cubeMap.get(index);
        return cube == null ? EMPTY_SECTION : cube.getSection();
    }

    // separate one for setBlockState with height checks for vanilla path, because we removed height checks in world
    @Redirect(method = "setBlockState", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=get"
    ))
    private ChunkSection getChunkSectionForSetBlockState(ChunkSection[] sections, int index) {
        if (!isCubic) {
            return index >= 0 && index < sections.length ? sections[index] : EMPTY_SECTION;
        }
        Cube cube = cubeMap.get(index);
        return cube == null ? EMPTY_SECTION : cube.getSection();
    }

    @Redirect(method = {"setBlockState", "read"}, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=set"
    ))
    private void setChunkSection(ChunkSection[] sections, int index, ChunkSection newValue) {
        if (index >= 0 && index < sections.length) {
            sections[index] = newValue;
        }
        if (isCubic) {
            cubeMap.computeIfAbsent(index, idx -> new Cube(this.pos.x, idx, this.pos.z)).setSection(newValue);
        }
    }

    @Redirect(method = {"getFluidState(III)Lnet/minecraft/fluid/IFluidState;", "getBlockState"}, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=length"
    ))
    private int getChunkSectionCount(ChunkSection[] sections) {
        return isCubic ? Integer.MAX_VALUE / 32 : sections.length;
    }
}
