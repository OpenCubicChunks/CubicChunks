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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SPacketSpawnPosition.class)
public class MixinSPacketSpawnPosition implements IPositionPacket {

    @Shadow private BlockPos spawnBlockPos;
    private BlockPos posOffset = BlockPos.ORIGIN;

    @Override public void setPosOffset(BlockPos posOffset) {
        this.posOffset = posOffset;
    }

    @Override public boolean hasPosOffset() {
        return this.posOffset != BlockPos.ORIGIN;
    }

    @Redirect(method = "writePacketData", at = @At(value = "FIELD",
            target = "Lnet/minecraft/network/play/server/SPacketSpawnPosition;spawnBlockPos:Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos preprocessPacket(SPacketSpawnPosition _this) {
        BlockPos pos = this.posOffset == BlockPos.ORIGIN ? this.spawnBlockPos : this.spawnBlockPos.add(this.posOffset);

        int y = pos.getY();
        if (!this.hasPosOffset() //if the position isn't offset, it means the client has cubic chunks and we should always send the un-clamped Y coord
            || y <= 2047 && y >= -2047)    {
            return pos;
        } else {
            //the spawn position might be outside of the +-2048 range, even after translation, so clamp it to avoid sending an invalid
            //packet
            return new BlockPos(pos.getX(), MathHelper.clamp(y, -2047, 2047), pos.getZ());
        }
    }

}
