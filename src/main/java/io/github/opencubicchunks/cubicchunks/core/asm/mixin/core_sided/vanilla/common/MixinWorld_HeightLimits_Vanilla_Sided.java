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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core_sided.vanilla.common;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld_HeightLimits;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Split from {@link MixinWorld_HeightLimits} for compatibility.
 */
@Mixin(World.class)
public abstract class MixinWorld_HeightLimits_Vanilla_Sided implements ICubicWorld {

    @Shadow protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    @Redirect(method = "spawnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z"))
    private boolean spawnEntity_isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent) {
        assert this == (Object) world;
        if (isCubicWorld()) {
            return this.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), ent.posY, cubeToMinBlock(chunkZ)), allowEmpty);
        } else {
            return this.isChunkLoaded(chunkX, chunkZ, allowEmpty);
        }
    }
}
