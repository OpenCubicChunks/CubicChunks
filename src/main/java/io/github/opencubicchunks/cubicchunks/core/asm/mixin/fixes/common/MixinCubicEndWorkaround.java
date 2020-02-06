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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseHoldingPattern;
import net.minecraft.entity.boss.dragon.phase.PhaseLanding;
import net.minecraft.entity.boss.dragon.phase.PhaseLandingApproach;
import net.minecraft.entity.boss.dragon.phase.PhaseTakeoff;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.end.DragonFightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// attempt at a workaround for some parts of end not working correctly in cubic end (for hybrid world)
@Mixin(value = {
        EntityDragon.class,
        PhaseHoldingPattern.class,
        PhaseLanding.class,
        PhaseLandingApproach.class,
        PhaseTakeoff.class,
        DragonFightManager.class}
)
public class MixinCubicEndWorkaround {
    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;getTopSolidOrLiquidBlock(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"),
            require = 0)
    private BlockPos getTopSolidOrLiquidBlockRedirect(WorldServer world, BlockPos pos) {
        return ((ICubicWorldInternal) world).getTopSolidOrLiquidBlockVanilla(pos);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getTopSolidOrLiquidBlock(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"),
            require = 0)
    private BlockPos getTopSolidOrLiquidBlockRedirect(World world, BlockPos pos) {
        return ((ICubicWorldInternal) world).getTopSolidOrLiquidBlockVanilla(pos);
    }


}
