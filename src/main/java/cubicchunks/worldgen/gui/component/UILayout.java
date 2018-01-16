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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import cubicchunks.util.MathUtil;
import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.ClipArea;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.component.IClipable;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.control.UIScrollBar;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

public abstract class UILayout<T extends UILayout<T>> extends UIContainer<T> {
    private UIOptionScrollbar scrollbar;

    private boolean needsLayoutUpdate = false;
    private int lastSizeX, lastSizeY;
    private boolean resizeToContent;

    private IntSupplier widthFunc;
    private IntSupplier heightFunc;

    public UILayout(ExtraGui gui) {
        super(gui);
        this.scrollbar = new UIOptionScrollbar((ExtraGui) getGui(), (T) this, UIScrollBar.Type.VERTICAL);
        this.scrollbar.setVisible(true);
        this.scrollbar.setPosition(6, 0);
    }

    @Override public T setRightPadding(int padding) {
        return super.setRightPadding(padding); // scrollbar
    }

    /**
     * Recalculate positions and sizes of components
     */
    protected abstract void layout();

    protected abstract boolean isLayoutChanged();

    protected boolean canAutoSizeX() {
        return true;
    }

    protected boolean canAutoSizeY() {
        return true;
    }

    public void setWidthFunc(@Nullable IntSupplier func) {
        this.widthFunc = func;
    }

    public void setHeightFunc(@Nullable IntSupplier func) {
        this.heightFunc = func;
    }

    @Override public int getRawWidth() {
        if (widthFunc != null) {
            return widthFunc.getAsInt();
        }
        return super.getRawWidth();
    }

    @Override
    public int getWidth() {
        if (resizeToContent && canAutoSizeX()) {
            return getContentWidth();
        }
        if (getRawWidth() > 0) {
            return getRawWidth();
        }

        if (parent == null) {
            return 0;
        }

        //if width < 0 consider it relative to parent container
        int w = parent.getWidth() + getRawWidth();
        if (parent instanceof UIContainer) {
            final UIContainer<?> parentContainer = (UIContainer<?>) parent;
            w -= parentContainer.getLeftPadding() + parentContainer.getRightPadding();
        }

        return w;
    }

    public int getAvailableWidth() {
        return getWidth() - getLeftPadding() - getRightPadding();
    }

    @Override public int getRawHeight() {
        if (heightFunc != null) {
            return heightFunc.getAsInt();
        }
        return super.getRawHeight();
    }

    @Override
    public int getHeight() {
        if (resizeToContent && canAutoSizeY()) {
            return getContentHeight();
        }
        if (getRawHeight() > 0) {
            return getRawHeight();
        }

        if (parent == null) {
            return 0;
        }

        //if height < 0 consider it relative to parent container
        int h = parent.getHeight() + getRawHeight();
        if (parent instanceof UIContainer) {
            final UIContainer<?> parentContainer = (UIContainer<?>) parent;
            h -= parentContainer.getTopPadding() + parentContainer.getBottomPadding();
        }

        return h;
    }

    public T autoFitToContent(boolean resizeToContent) {
        this.resizeToContent = resizeToContent;
        return self();
    }

    public void setNeedsLayoutUpdate() {
        this.needsLayoutUpdate = true;
    }

    @Override
    public void add(UIComponent<?>... components) {
        this.needsLayoutUpdate = true;
        super.add(components);
    }

    @Override
    public void remove(UIComponent<?> component) {
        this.needsLayoutUpdate = true;
        super.remove(component);
    }

    @Override
    public void removeAll() {
        for (UIComponent<?> c : new HashSet<>(components)) {
            remove(c);
        }
    }

    @Override
    public float getScrollStep() {
        float contentSize = getContentHeight() - getHeight();
        float scrollStep = super.getScrollStep() * 1000;
        float scrollFraction = scrollStep / contentSize;
        if (Float.isFinite(scrollFraction) && scrollFraction > 0) {
            return scrollFraction;
        }
        return 0;
    }

    @Override
    public float getOffsetY() {
        if (getContentHeight() <= getHeight()) {
            return 0;
        }
        return (float) yOffset / (getContentHeight() - getHeight());
    }

    @Override
    public float getOffsetX() {
        if (getContentWidth() <= getWidth()) {
            return 0;
        }
        return (float) xOffset / (getContentWidth() - getWidth());
    }


    @Override
    public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        ClipArea area = this.getClipArea();
        for (UIComponent<?> c : components) {
            int x = c.screenX();
            int y = c.screenY();
            int X = x + c.getWidth();
            int Y = y + c.getHeight();
            if (MathUtil.rangesIntersect(x, X, area.x, area.X) && MathUtil.rangesIntersect(y, Y, area.y, area.Y)) {
                c.draw(renderer, mouseX, mouseY, partialTick);
            }
        }
    }

    public void checkLayout() {
        if (needsLayoutUpdate || getWidth() != lastSizeX || getHeight() != lastSizeY || isLayoutChanged()) {
            lastSizeX = getWidth();
            lastSizeY = getHeight();
            needsLayoutUpdate = false;
            layout();
        }
    }
}
