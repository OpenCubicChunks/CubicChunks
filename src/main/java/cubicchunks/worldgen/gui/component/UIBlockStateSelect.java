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

import cubicchunks.worldgen.gui.DummyWorld;
import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.ClipArea;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.IClipable;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.container.UIPanel;
import net.malisis.core.client.gui.component.control.IScrollable;
import net.malisis.core.client.gui.component.control.UIScrollBar;
import net.malisis.core.client.gui.component.control.UISlimScrollbar;
import net.malisis.core.client.gui.component.decoration.UITooltip;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.element.GuiShape;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.client.gui.element.XYResizableGuiShape;
import net.malisis.core.renderer.icon.provider.GuiIconProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIBlockStateSelect<T extends UIBlockStateSelect<T>> extends UIContainer<T> {

    private static final int PADDING_VERT = 20;
    private static final int PADDING_HORIZ = 20;

    private Consumer<IBlockState> onSelect;

    private static final List<IBlockState> allStates;

    static {
        List<IBlockState> states = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (state != block.getStateFromMeta(block.getMetaFromState(state))) {
                    continue;
                }
                if (state.getBlock().hasTileEntity(state)
                        && TileEntityRendererDispatcher.instance.getRenderer(state.getBlock().createTileEntity(null, state)) != null) {
                    continue; // Don't allow TESR
                }
                states.add(state);
            }
        }

        allStates = states;
    }

    public UIBlockStateSelect(ExtraGui gui) {
        super(gui);

        UIScrollBar scrollbar = new UIOptionScrollbar((ExtraGui) getGui(), (T) this, UIScrollBar.Type.VERTICAL);
        scrollbar.setVisible(true);
        scrollbar.setPosition(4, 0);
        this.clipContent = true;

        this.setPadding(PADDING_HORIZ, PADDING_VERT);
    }

    @Override public ClipArea getClipArea() {
        return new ClipArea(this, getLeftPadding(), getTopPadding(), getWidth() - getRightPadding(), getHeight() - getBottomPadding(), false);
    }

    @Override public boolean onMouseMove(int lastX, int lastY, int x, int y) {
        int idx = getSelectedIdx(x, y);
        if (idx < 0 || idx >= allStates.size()) {
            tooltip = null;
        } else {
            if (tooltip == null) {
                tooltip = new UITooltip(getGui());
            }
            tooltip.setText(UIBlockStateButton.generateTooltip(allStates.get(idx)));
        }
        return true;
    }

    @Override public boolean onClick(int x, int y) {
        int idx = getSelectedIdx(x, y);
        if (idx < 0 || idx >= allStates.size()) {
            return false;
        }
        onSelect.accept(allStates.get(idx));
        getGui().removeFromScreen(this);
        return true;
    }

    public void display(Consumer<IBlockState> onSelect) {
        this.onSelect = onSelect;

        this.setZIndex(1);
        getGui().addToScreen(this);
    }

    @Override public void drawBackground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        rp.useTexture.set(false);

        renderer.disableTextures();

        // first black, and then white on top of it
        // this makes the white overlay actually look nice
        rp.alpha.set(200);
        rp.colorMultiplier.set(0);

        shape.resetState();
        shape.setSize(getWidth(), getHeight());
        renderer.drawShape(shape, rp);

        rp.alpha.set(100);
        rp.colorMultiplier.set(0xFFFFFF);

        shape.resetState();
        shape.setSize((int) getAvailableWidth(), (int) getAvailableHeight());
        shape.setPosition(getLeftPadding(), getTopPadding());
        renderer.drawShape(shape, rp);

        renderer.next();
        renderer.enableTextures();
    }

    @Override public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {

        // half of that on the left, half on the right
        int addPadding =
                (int) Math.round((getAvailableWidth() - getLineStates() * UIBlockStateButton.SIZE) * 0.5);

        double offsetY = getOffsetY();
        double pixelsOffset = offsetY * (getContentHeight() - getAvailableHeight());

        int lineStart = (int) (pixelsOffset / UIBlockStateButton.SIZE);
        int lineEnd = MathHelper.ceil((pixelsOffset + getAvailableHeight()) / UIBlockStateButton.SIZE);

        int itemStart = lineStart * getLineStates();
        int itemEnd = lineEnd * getLineStates();

        GlStateManager.bindTexture(0);
        int idx = getSelectedIdx(mouseX, mouseY);
        if (idx >= 0 && idx < allStates.size()) {
            int line = idx / getLineStates();
            int num = idx % getLineStates();

            shape.resetState();
            shape.setSize(UIBlockStateButton.SIZE, UIBlockStateButton.SIZE);
            shape.setPosition(num * UIBlockStateButton.SIZE + getLeftPadding() + addPadding, (int) (line * UIBlockStateButton.SIZE - pixelsOffset +
                    getTopPadding()));
            rp.setAlpha(200);
            rp.setColor(0);
            renderer.drawShape(shape, rp);
        }

        renderer.next();

        GlStateManager.enableDepth();

        Tessellator.getInstance().draw();
        ITextureObject blockTexture = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for (int i = itemStart; i < itemEnd; i++) {
            if (i >= allStates.size() || i < 0) {
                continue;
            }
            int line = i / getLineStates();
            int num = i % getLineStates();

            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            Minecraft.getMinecraft().entityRenderer.enableLightmap();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blockTexture.getGlTextureId());

            drawState(allStates.get(i), num * UIBlockStateButton.SIZE + PADDING_HORIZ + addPadding,
                    (int) (line * UIBlockStateButton.SIZE - pixelsOffset + PADDING_VERT));
        }


        renderer.next();

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void drawState(IBlockState state, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) this.screenX() + x + 2, (float) this.screenY() + 18f + y, 100.0F);
        GlStateManager.scale(14.0F, 14.0F, -14.0F);
        GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);

        BufferBuilder buf = Tessellator.getInstance().getBuffer();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(state, BlockPos.ORIGIN,
                DummyWorld.getInstanceWithBlockState(state), buf);

        Tessellator.getInstance().draw();

        GlStateManager.popMatrix();
    }

    private double getAvailableWidth() {
        return getWidth() - PADDING_HORIZ * 2;
    }

    private int getSelectedIdx(int mouseX, int mouseY) {

        int addPadding =
                (int) Math.round((getAvailableWidth() - getLineStates() * UIBlockStateButton.SIZE) * 0.5);

        mouseX -= getLeftPadding() + addPadding;
        mouseY -= getTopPadding();

        if (mouseX < 0 || mouseX >= getLineStates() * UIBlockStateButton.SIZE) {
            return -1;
        }
        int column = mouseX / UIBlockStateButton.SIZE;

        double pixelsOffset = getOffsetY() * (getContentHeight() - getAvailableHeight());
        int totalY = (int) (mouseY + pixelsOffset);
        int row = totalY / UIBlockStateButton.SIZE;

        return row * getLineStates() + column;
    }

    private double getAvailableHeight() {
        return getHeight() - PADDING_VERT * 2;
    }

    private int getLineStates() {
        return (int) (getAvailableWidth() / UIBlockStateButton.SIZE);
    }

    private int getLineCount() {
        return MathHelper.ceil(allStates.size() / (double) getLineStates());
    }

    @Override
    public int getContentWidth() {
        return getWidth();
    }

    @Override
    public int getContentHeight() {
        return getLineCount() * UIBlockStateButton.SIZE;
    }
}
