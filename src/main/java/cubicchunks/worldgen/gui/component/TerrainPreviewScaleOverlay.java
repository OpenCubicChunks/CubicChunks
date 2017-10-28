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

import cubicchunks.util.MathUtil;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.element.GuiShape;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.renderer.font.MalisisFont;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.util.vector.Matrix4f;

public class TerrainPreviewScaleOverlay extends UIContainer<TerrainPreviewScaleOverlay> {

    private final ShaderMultiImageView<?> wrappedComponent;
    private float zoom;
    private float offsetX;
    private float offsetY;
    private float biomeScale;
    private float biomeOffset;

    public TerrainPreviewScaleOverlay(MalisisGui gui, ShaderMultiImageView<?> wrappedComponent) {
        super(gui);
        this.wrappedComponent = wrappedComponent;
        add(wrappedComponent);
    }

    public void setTransform(float zoom, float offsetX, float offsetY) {
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void setBiomeTransform(float biomeScale, float biomeOffset) {
        this.biomeScale = biomeScale;
        this.biomeOffset = biomeOffset;
    }

    @Override public void drawBackground(GuiRenderer guiRenderer, int i, int i1, float v) {
        wrappedComponent.setSize(getWidth(), getHeight());
        super.drawBackground(guiRenderer, i, i1, v);
    }

    @Override public void drawForeground(GuiRenderer guiRenderer, int i, int i1, float v) {
        super.drawForeground(guiRenderer, i, i1, v);

        drawXScale();
        drawYScale();
        drawBiomeNames();
    }

    private void drawBiomeNames() {

    }

    private void drawXScale() {
        float blockLeft = posToX(0);
        float blockRight = posToX(getWidth());

        float maxVal = Math.max(Math.abs(blockLeft), Math.abs(blockRight));
        int digits = MathHelper.ceil(Math.log(Math.max(maxVal, 1)) / Math.log(10)) + ((blockLeft < 0 || blockRight < 0) ? 1 : 0);

        int increment = getIncrement(blockLeft, blockRight, getWidth() / (7 * digits));
        if (increment < 0 || maxVal + increment > Integer.MAX_VALUE) {
            return;// TODO: handle integer overflow for increment
        }
        int start = MathHelper.roundUp(MathHelper.floor(blockLeft), increment);

        FontOptions fo = new FontOptions.FontOptionsBuilder().color(0xFFFFFF).shadow(true).build();
        for (int x = start; x <= blockRight; x += increment) {
            int pos = (int) xToPos(x);
            int strWidth = (int) (MalisisFont.minecraftFont.getStringWidth(String.valueOf(Math.abs(x)), fo) / 2 +
                    (x < 0 ? MalisisFont.minecraftFont.getStringWidth("-", fo) : 0));
            renderer.drawText(MalisisFont.minecraftFont, String.valueOf(x), pos - strWidth + 1, getHeight() - 10, 0, fo);
        }

        renderer.next();
        GlStateManager.disableTexture2D();
        SimpleGuiShape shape = new SimpleGuiShape();
        shape.setSize(1, 2);
        for (int x = start; x <= blockRight; x += increment) {
            int pos = (int) xToPos(x);
            int strWidth = (int) (MalisisFont.minecraftFont.getStringWidth(String.valueOf(x), fo) / 2);
            shape.storeState();
            shape.setPosition(pos, getHeight() - 1);
            renderer.drawShape(shape, rp);

            shape.resetState();
        }
        renderer.next();
        GlStateManager.enableTexture2D();

    }

    private void drawYScale() {
        float blockBottom = posToY(getHeight());// bottom -> getHeight()
        float blockTop = posToY(0);

        int increment = getIncrement(blockBottom, blockTop, getHeight() / 11);
        if (increment < 0 || Math.max(Math.abs(blockBottom) + increment, Math.abs(blockTop) + increment) > Integer.MAX_VALUE) {
            return;// TODO: handle integer overflow for increment
        }
        int start = MathHelper.roundUp(MathHelper.floor(blockBottom), increment);

        FontOptions fo = new FontOptions.FontOptionsBuilder().color(0xFFFFFF).shadow(true).build();
        for (int y = start; y <= blockTop; y += increment) {
            int pos = (int) yToPos(y);
            if (pos < 15 || pos > getHeight() - 15) {
                continue;
            }
            int strHeight = (int) (MalisisFont.minecraftFont.getStringHeight() / 2);
            // use the "-" character as graph mark
            renderer.drawText(MalisisFont.minecraftFont, "- " + y, 0, pos - strHeight, 0, fo);
        }

    }

    private float posToY(float pos) {
        return offsetY + zoom * (-pos + getHeight() / 2);
    }

    private float yToPos(float y) {
        return -(y - offsetY) / zoom + getHeight() / 2;
    }

    private float posToX(float pos) {
        return offsetX + zoom * (pos - getWidth() / 2);
    }

    private float xToPos(float y) {
        return (y - offsetX) / zoom + getWidth() / 2;
    }

    private int getIncrement(float start, float end, int maxAmount) {
        float totalSize = Math.abs(end - start);

        int curr = 1;
        while (curr < totalSize / maxAmount) {
            long n = curr;
            if (MathUtil.isPowerOfN(curr, 10)) {
                n *= 2;
            } else if (MathUtil.isPowerOfN(curr / 2, 10)) {
                n /= 2;
                n *= 5;
            } else {
                assert MathUtil.isPowerOfN(curr / 5, 10); // 5*powerOf10
                n *= 2;
            }
            if (n != (int) n) {
                return -1; // integer overflow, show just the beginning and the end
            }
            curr = (int) n;
        }

        return curr;
    }
}
