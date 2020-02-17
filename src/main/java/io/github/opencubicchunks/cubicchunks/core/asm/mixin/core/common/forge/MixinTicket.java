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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.forge;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ForgeChunkManager.Ticket.class, remap = false)
public abstract class MixinTicket implements ICubicTicketInternal {

    private LinkedHashSet<CubePos> forcedCubes = new LinkedHashSet<>();
    private Map<ChunkPos, IntSet> cubePosMap = new HashMap<>();
    private int entityChunkY;
    private int cubeDepth;

    @Override @Accessor public abstract void setModData(NBTTagCompound modData);
    @Override @Accessor public abstract void setPlayer(String player);
    @Override @Accessor public abstract void setEntityChunkX(int chunkX);
    @Override @Accessor public abstract void setEntityChunkZ(int chunkZ);
    @Override @Accessor public abstract int getEntityChunkX();
    @Override @Accessor public abstract int getEntityChunkZ();

    @Override public int getEntityChunkY() {
        return entityChunkY;
    }
    @Override public void setEntityChunkY(int cubeY) {
        this.entityChunkY = cubeY;
    }

    @Inject(
            method = "<init>(Ljava/lang/String;Lnet/minecraftforge/common/ForgeChunkManager$Type;Lnet/minecraft/world/World;)V", at = @At("RETURN"),
            remap = false
    )
    private void onConstruct(String modId, ForgeChunkManager.Type type, World world, CallbackInfo cbi) {
        this.cubeDepth = CubicChunkManager.getCubeDepthFor(modId);
    }

    @Override public void addRequestedCube(CubePos pos) {
        cubePosMap.computeIfAbsent(pos.chunkPos(), chunkPos -> new IntOpenHashSet(32)).add(pos.getY());
    }

    @Override public void removeRequestedCube(CubePos pos) {
        IntSet set = cubePosMap.get(pos.chunkPos());
        if (set != null) {
            set.remove(pos.getY());
            if (set.isEmpty()) {
                cubePosMap.remove(pos.chunkPos());
            }
        }
    }

    @Override public void setForcedChunkCubes(ChunkPos location, IntSet yCoords) {
        cubePosMap.put(location, yCoords);
    }

    @Override public void clearForcedChunkCubes(ChunkPos location) {
        cubePosMap.remove(location);
    }

    @Override public Map<ChunkPos, IntSet> getAllForcedChunkCubes() {
        return Collections.unmodifiableMap(cubePosMap);
    }

    @Override public void setAllForcedChunkCubes(Map<ChunkPos, IntSet> cubePosMap) {
        this.cubePosMap = cubePosMap;
    }

    @Override public int getMaxCubeDepth() {
        return cubeDepth;
    }

    @Override public Set<CubePos> requestedCubes() {
        return forcedCubes;
    }
}
