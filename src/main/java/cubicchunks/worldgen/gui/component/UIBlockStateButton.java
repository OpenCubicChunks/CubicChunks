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
package cubicchunks.worldgen.gui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.lwjgl.opengl.GL11;

import cubicchunks.worldgen.gui.DummyWorld;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class UIBlockStateButton<T extends UIBlockStateButton<T>> extends UIComponent<T> {

    public static final int SIZE = 24;
    private IBlockState iBlockState;
    private final List<Consumer<? super UIBlockStateButton<?>>> onClick;

    public UIBlockStateButton(MalisisGui gui, IBlockState iBlockState1) {
        super(gui);
        iBlockState = iBlockState1;
        onClick = new ArrayList<>();
        setTooltip(generateTooltip(iBlockState));
        setSize(SIZE, SIZE);
    }

    public T onClick(Consumer<? super UIBlockStateButton<?>> cons) {
        this.onClick.add(cons);
        return self();
    }

    public static String generateTooltip(IBlockState blockState) {
        StringBuffer sb = new StringBuffer(128);
        sb.append(blockState.getBlock().getLocalizedName());
        for (Entry<IProperty<?>, Comparable<?>> entry : blockState.getProperties().entrySet()) {
            sb.append(" \n ");
            sb.append(entry.getKey().getName());
            sb.append(" = ");
            sb.append(entry.getValue().toString());
        }
        return sb.toString();
    }

    public IBlockState getState() {
        return iBlockState;
    }

    public void setBlockState(IBlockState iBlockState1) {
        iBlockState = iBlockState1;
        setTooltip(generateTooltip(iBlockState));
    }

    @Override
    public boolean onClick(int x, int y) {
        MalisisGui.playSound(SoundEvents.UI_BUTTON_CLICK);
        onClick.forEach(cons -> cons.accept(self()));
        return true;
    }

    @Override
    public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        if (iBlockState != null) {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableRescaleNormal();

            BufferBuilder vertexbuffer = Tessellator.getInstance().getBuffer();
            Tessellator.getInstance().draw();
            ITextureObject blockTexture = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blockTexture.getGlTextureId());
            VertexFormat format = DefaultVertexFormats.BLOCK;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) this.screenX(), (float) this.screenY() + 16f, 100.0F);
            GlStateManager.scale(12.0F, 12.0F, -12.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            vertexbuffer.begin(GL11.GL_QUADS, format);
            Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(iBlockState, BlockPos.ORIGIN,
                    DummyWorld.getInstanceWithBlockState(iBlockState), vertexbuffer);
            Tessellator.getInstance().draw();
            if (iBlockState.getBlock().hasTileEntity(iBlockState)) {
                TileEntity te = iBlockState.getBlock().createTileEntity(null, iBlockState);
                if (te != null) {
                    TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer =
                            TileEntityRendererDispatcher.instance.<TileEntity>getRenderer(te);
                    if (tileentityspecialrenderer != null) {
                        TileEntityItemStackRenderer.instance.renderByItem(new ItemStack(iBlockState.getBlock()));
                    }
                }
            }
            GlStateManager.popMatrix();
            renderer.next();

            GlStateManager.disableRescaleNormal();
        }
        GlStateManager.enableLighting();
    }

    @Override
    public void drawBackground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
    }
}
