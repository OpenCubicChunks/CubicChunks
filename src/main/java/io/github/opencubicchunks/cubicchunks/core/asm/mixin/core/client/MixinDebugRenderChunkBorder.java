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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.debug.DebugRendererChunkBorder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(DebugRendererChunkBorder.class)
public class MixinDebugRenderChunkBorder {

    private boolean isCubicWorld() {
        return ((ICubicWorld) Minecraft.getMinecraft().world).isCubicWorld();
    }

    /**
     * @param partialTicks partial ticks
     * @param  finishTimeNano max time to finish frame to fit into fps limit
     * @param ci callback info
     *
     * @author Babbaj
     * @reason Change chunk border renderer to work at any Y value.
     */
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void renderChunkBorder(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!isCubicWorld())
            return;

        ci.cancel();

        EntityPlayer player = Minecraft.getMinecraft().player;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        double yOffset = (Math.round(playerY / 16)) * 16 - 128; // Offset the grid's y coord to based on the player's y coord
        double minY = (0.0D - playerY) + yOffset; // add the offset
        double maxY = (256.0D - playerY) + yOffset;
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        double chunkX = (double) (player.chunkCoordX << 4) - playerX;
        double chunkZ = (double) (player.chunkCoordZ << 4) - playerZ;
        GlStateManager.glLineWidth(1.0F);
        bufferbuilder.begin(3, DefaultVertexFormats.POSITION_COLOR);

        // Red vertical lines
        for (int i = -16; i <= 32; i += 16) {
            for (int j = -16; j <= 32; j += 16) {
                bufferbuilder.pos(chunkX + (double) i, minY, chunkZ + (double) j).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
                bufferbuilder.pos(chunkX + (double) i, minY, chunkZ + (double) j).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                bufferbuilder.pos(chunkX + (double) i, maxY, chunkZ + (double) j).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                bufferbuilder.pos(chunkX + (double) i, maxY, chunkZ + (double) j).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
            }
        }

        // East-West yellow vertical lines
        for (int k = 2; k < 16; k += 2) {
            bufferbuilder.pos(chunkX + (double) k, minY, chunkZ).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, minY, chunkZ).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, maxY, chunkZ).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, maxY, chunkZ).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, minY, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, minY, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, maxY, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + (double) k, maxY, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
        }

        // South-North yellow vertical lines
        for (int l = 2; l < 16; l += 2) {
            bufferbuilder.pos(chunkX, minY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX, minY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, maxY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, maxY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, minY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, minY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, maxY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, maxY, chunkZ + (double) l).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
        }

        // Yellow horizontal lines
        //start from the offset
        for (int i1 = (int) yOffset; i1 <= 256 + (int) yOffset; i1 += 2) {
            double d7 = (double) i1 - playerY;
            bufferbuilder.pos(chunkX, d7, chunkZ).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX, d7, chunkZ).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d7, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, d7, chunkZ + 16.0D).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, d7, chunkZ).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d7, chunkZ).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d7, chunkZ).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
        }

        tessellator.draw();
        GlStateManager.glLineWidth(2.0F);
        bufferbuilder.begin(3, DefaultVertexFormats.POSITION_COLOR);

        // Blue vertical lines
        for (int j1 = 0; j1 <= 16; j1 += 16) {
            for (int l1 = 0; l1 <= 16; l1 += 16) {
                bufferbuilder.pos(chunkX + (double) j1, minY, chunkZ + (double) l1).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(chunkX + (double) j1, minY, chunkZ + (double) l1).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
                bufferbuilder.pos(chunkX + (double) j1, maxY, chunkZ + (double) l1).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
                bufferbuilder.pos(chunkX + (double) j1, maxY, chunkZ + (double) l1).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
            }
        }

        // Blue horizontal lines
        //start from the offset
        for (int k1 = (int) yOffset; k1 <= 256 + (int) yOffset; k1 += 16) {
            double d8 = (double) k1 - playerY;
            bufferbuilder.pos(chunkX, d8, chunkZ).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
            bufferbuilder.pos(chunkX, d8, chunkZ).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d8, chunkZ + 16.0D).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, d8, chunkZ + 16.0D).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX + 16.0D, d8, chunkZ).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d8, chunkZ).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferbuilder.pos(chunkX, d8, chunkZ).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
        }

        tessellator.draw();
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
    }


}
