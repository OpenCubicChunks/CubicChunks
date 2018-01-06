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
package cubicchunks.asm.mixin.core.client;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Queues;

import cubicchunks.client.IVisGraph;
import cubicchunks.client.RenderVariables;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IntegerCache;
import net.minecraft.util.math.BlockPos;

@Mixin(VisGraph.class)
@Implements(@Interface(iface = IVisGraph.class, prefix = "visGraph$"))
public class MixinVisGraph implements IVisGraph {

    private int blocksAmount = RenderVariables.getRenderChunkBlocksAmount();
    private BitSet opaqueBlocksBitSet = new BitSet(blocksAmount);
    
    @Shadow
    private int empty;
    
    @Inject(method = "<init>", at = @At(value = "RETURN"), cancellable = false)
    public void init(CallbackInfo ci) {
        this.empty = blocksAmount;
    }

    @Overwrite
    public void setOpaqueCube(BlockPos pos) {
        this.opaqueBlocksBitSet.set(RenderVariables.getIndex(pos), true);
        --this.empty;
    }

    @Override
    public void setOpaqueCube(int localX, int localY, int localZ) {
        this.opaqueBlocksBitSet.set(RenderVariables.getIndex(localX, localY, localZ), true);
        --this.empty;
    }

    @Overwrite
    public SetVisibility computeVisibility() {
        SetVisibility setvisibility = new SetVisibility();
        if (blocksAmount - this.empty < blocksAmount >> 4) {
            setvisibility.setAllVisible(true);
        } else if (this.empty == 0) {
            setvisibility.setAllVisible(false);
        } else {
            for (int i : RenderVariables.getVisGraphIndexOfEdges()) {
                if (!this.opaqueBlocksBitSet.get(i)) {
                    setvisibility.setManyVisible(this.floodFill(i));
                }
            }
        }
        return setvisibility;
    }

    @Overwrite
    public Set<EnumFacing> getVisibleFacings(BlockPos pos) {
        return this.floodFill(RenderVariables.getIndex(pos));
    }

    private Set<EnumFacing> floodFill(int pos) {
        Set<EnumFacing> set = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
        Queue<Integer> queue = Queues.<Integer>newArrayDeque();
        queue.add(IntegerCache.getInteger(pos));
        this.opaqueBlocksBitSet.set(pos, true);

        while (!queue.isEmpty()) {
            int i = ((Integer) queue.poll()).intValue();
            RenderVariables.addEdges(i, set);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                int j = RenderVariables.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !this.opaqueBlocksBitSet.get(j)) {
                    this.opaqueBlocksBitSet.set(j, true);
                    queue.add(IntegerCache.getInteger(j));
                }
            }
        }

        return set;
    }
}
