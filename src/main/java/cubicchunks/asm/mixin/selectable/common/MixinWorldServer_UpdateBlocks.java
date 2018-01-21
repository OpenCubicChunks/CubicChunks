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
package cubicchunks.asm.mixin.selectable.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.world.CubicWorldServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;

@Mixin(value = WorldServer.class, priority = 1001)
public abstract class MixinWorldServer_UpdateBlocks implements CubicWorldServer {

    /**
     * This redirection (if selected by {@link cubicchunks.asm.CubicChunksMixinConfig})
     * will return value {@code 0} instead of {@code getGameRules().getInt("randomTickSpeed")} for cubic type worlds.
     * Redirected function is located inside WorldServer.updateBlocks() function at a line 404.
     * Returned zero will cause {@code if (i > 0)} check at a line 474 to fail and random block ticks skipped.
     * For cubic worlds random block ticks handled inside {@link cubicchunks.world.cube.Cube} class.
     **/
    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getInt(Ljava/lang/String;)I"), require = 1)
    public int redirectGetRandomTickSpeed(GameRules gameRules, String ruleName) {
        return this.isCubicWorld() ? 0 : gameRules.getInt(ruleName);
    }
}
