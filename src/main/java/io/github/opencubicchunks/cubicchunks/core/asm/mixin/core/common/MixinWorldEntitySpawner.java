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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(WorldEntitySpawner.class)
public class MixinWorldEntitySpawner implements IWorldEntitySpawner.Handler {

    @Nullable private IWorldEntitySpawner customSpawner;

    @Override public void setEntitySpawner(@Nullable IWorldEntitySpawner spawner) {
        this.customSpawner = spawner;
    }

    @Override @Nullable public IWorldEntitySpawner getEntitySpawner() {
        return this.customSpawner;
    }

    @Inject(method = "findChunksForSpawning", cancellable = true, at = @At("HEAD"))
    private void onSpawnMobs(WorldServer world, boolean hostileEnable,
            boolean peacefulEnable, boolean spawnOnSetTickRate, CallbackInfoReturnable<Integer> cir) {
        if (this.customSpawner != null) {
            int ret = this.customSpawner.findChunksForSpawning(world, hostileEnable, peacefulEnable, spawnOnSetTickRate);
            cir.setReturnValue(ret);
            cir.cancel();
        }
    }
}
