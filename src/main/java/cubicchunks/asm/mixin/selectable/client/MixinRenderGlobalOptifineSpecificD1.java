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
package cubicchunks.asm.mixin.selectable.client;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import javax.annotation.ParametersAreNonnullByDefault;
import cubicchunks.world.ICubicWorld;
import net.minecraft.client.multiplayer.WorldClient;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalOptifineSpecificD1 {

    @Shadow 
    private WorldClient world;
    
    @ModifyArg(method = "setupTerrain", at = @At(target = 
            "Lnet/minecraft/client/renderer/RenderGlobal;getRenderChunkOffset(Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/client/renderer/chunk/RenderChunk;"
            + "Lnet/minecraft/util/EnumFacing;ZI)Lnet/minecraft/client/renderer/chunk/RenderChunk;", value = "INVOKE"))
    public int onGetRenderChunkOffset(int originalMaxXChunkY){
        if(((ICubicWorld)world).isCubicWorld())
            return Integer.MAX_VALUE;
        return originalMaxXChunkY;
    }
    
    @Redirect(method = "setupTerrain", at = @At(target = "Lnet/minecraft/util/math/BlockPos;getY()I", value = "INVOKE", ordinal = 2), require = 1)
    public int getPosOnCompareToMaxChunkY2(BlockPos pos) {
        if(((ICubicWorld)world).isCubicWorld())
            return Integer.MIN_VALUE;
        return pos.getY();
    }
}
