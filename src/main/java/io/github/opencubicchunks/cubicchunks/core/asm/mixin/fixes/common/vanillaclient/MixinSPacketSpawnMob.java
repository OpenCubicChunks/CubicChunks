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
import net.minecraft.network.play.server.SPacketSpawnMob;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SPacketSpawnMob.class)
public class MixinSPacketSpawnMob implements IPositionPacket {

    @Shadow private double x;
    @Shadow private double y;
    @Shadow private double z;
    private BlockPos posOffset = BlockPos.ORIGIN;

    @Override public void setPosOffset(BlockPos posOffset) {
        this.posOffset = posOffset;
    }

    @Override public boolean hasPosOffset() {
        return this.posOffset != BlockPos.ORIGIN;
    }

    @Redirect(method = "writePacketData", at = @At(value = "FIELD", target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;x:D"))
    private double preprocessPacketX(SPacketSpawnMob _this) {
        return this.x + this.posOffset.getX();
    }

    @Redirect(method = "writePacketData", at = @At(value = "FIELD", target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;y:D"))
    private double preprocessPacketY(SPacketSpawnMob _this) {
        return this.y + this.posOffset.getY();
    }

    @Redirect(method = "writePacketData", at = @At(value = "FIELD", target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;z:D"))
    private double preprocessPacketZ(SPacketSpawnMob _this) {
        return this.z + this.posOffset.getZ();
    }
}
