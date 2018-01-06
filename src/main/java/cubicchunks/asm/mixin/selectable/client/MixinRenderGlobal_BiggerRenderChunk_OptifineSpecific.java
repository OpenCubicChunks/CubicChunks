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

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import cubicchunks.client.IRenderChunk;
import cubicchunks.world.ICubicWorldClient;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderGlobal.ContainerLocalRenderInformation;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

/**
 * Fixes renderEntities crashing when rendering cubes that are not at existing array index in chunk.getEntityLists(), <p> Allows to render cubes outside of 0..256 height range.
 */
@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal_BiggerRenderChunk_OptifineSpecific {

    @Shadow private WorldClient world;
    @Shadow public List<ContainerLocalRenderInformation> renderInfosEntities;
    protected Chunk blankChunk;
    private ContainerLocalRenderInformation info;

    @Inject(method = "setWorldAndLoadRenderers", at = @At(value = "RETURN"), cancellable = false)
    public void onSetWorldAndLoadRenderers(@Nullable WorldClient worldClientIn, CallbackInfo ci) {
        if (worldClientIn != null)
            blankChunk = new EmptyChunk(worldClientIn, 0, 0);
    }

    @Shadow
    abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/Deque;poll()Ljava/lang/Object;"), remap = false)
    public Object onGettingRenderInfoFromQueue(Deque<ContainerLocalRenderInformation> queue) {
        info = queue.poll();
        return info;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;", ordinal = 0))
    public Chunk onGettingChunkFromRenderChunk(RenderChunk renderChunk) {
        IRenderChunk renderChunkEntitiesCheck = (IRenderChunk) renderChunk;
        if (renderChunkEntitiesCheck.hasEntities())
            renderInfosEntities.add(info);
        return blankChunk;
    }
}
