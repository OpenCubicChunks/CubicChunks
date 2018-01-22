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

import com.google.common.eventbus.Subscribe;
import cubicchunks.util.CooldownTimer;
import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.control.IScrollable;
import net.malisis.core.client.gui.component.control.UIScrollBar;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.client.gui.event.component.StateChangeEvent;
import net.malisis.core.renderer.animation.Animation;
import net.malisis.core.renderer.animation.transformation.AlphaTransform;
import net.malisis.core.util.MouseButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.util.concurrent.TimeUnit;

public class UIOptionScrollbar extends UIScrollBar implements IDragTickable {

    private final CooldownTimer timer = new CooldownTimer(1000 / 20, TimeUnit.MILLISECONDS);

    private boolean isScrollHovered = false;

    /** Background color of the scroll. */
    protected int backgroundColor = 0;
    /** Scroll color **/
    protected int scrollColor1 = 0x808080;
    protected int scrollColor2 = 0xC0C0C0;
    /** Whether the scrollbar should fade in/out */
    protected boolean fade = false;
    protected SimpleGuiShape scrollShapeInner;
    private int lastHeight = Integer.MIN_VALUE;
    private int lastWidth = Integer.MIN_VALUE;

    public <T extends UIComponent<T> & IScrollable> UIOptionScrollbar(ExtraGui gui, T parent, Type type) {
        super(gui, parent, type);
        setScrollSize(6, 15);

        gui.registerDragTickable(this);
    }

    public void setFade(boolean fade) {
        this.fade = fade;
    }

    public boolean isFade() {
        return fade;
    }

    @Override
    protected void setPosition() {
        int vp = getScrollable().getRightPadding();
        int hp = getScrollable().getBottomPadding();

        if (type == UIScrollBar.Type.HORIZONTAL) {
            setPosition(-hp + offsetX, vp + offsetY, Anchor.BOTTOM);
        } else {
            setPosition(hp + offsetX, -vp + offsetY, Anchor.RIGHT);
        }
    }

    @Override
    protected void createShape(MalisisGui gui) {
        int w = type == Type.HORIZONTAL ? scrollHeight : scrollThickness;
        int h = type == Type.HORIZONTAL ? scrollThickness : scrollHeight;

        //background shape
        shape = new SimpleGuiShape();
        //scroller shape
        scrollShape = new SimpleGuiShape();
        scrollShape.setSize(w, h);
        scrollShape.storeState();

        scrollShapeInner = new SimpleGuiShape();
        scrollShapeInner.setSize(w - 1, h - 1);
        scrollShapeInner.storeState();
    }

    @Override
    public int getWidth() {
        int w = super.getWidth();
        if (type == Type.HORIZONTAL) {
            w -= getScrollable().getLeftPadding() + getScrollable().getRightPadding();
        }
        return w;
    }

    @Override
    public int getHeight() {
        int h = super.getHeight();
        if (type == Type.VERTICAL) {
            h -= getScrollable().getTopPadding() + getScrollable().getBottomPadding();
        }
        return h;

    }

    public void setColor(int scrollColor1, int scrollColor2) {
        setColor(scrollColor1, scrollColor2, backgroundColor);
    }

    public void setColor(int scrollColor1, int scrollColor2, int backgroundColor) {
        this.scrollColor1 = scrollColor1;
        this.scrollColor2 = scrollColor2;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void draw(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        // temporary hack until malisis core does events properly
        // it has to be in draw() so that it's called even when scrollbar is not visible
        if (lastHeight != parent.getHeight() || lastWidth != parent.getWidth()) {

            int contentSize = ((IScrollable) parent).getContentHeight();
            this.setVisible(contentSize > parent.getHeight());

            float visibleHeight = getHeight();
            float visibleFraction = visibleHeight / contentSize;
            int scrollbarHeight = Math.round(visibleFraction * visibleHeight);
            this.setScrollSize(scrollThickness, scrollbarHeight);
        }
        super.draw(renderer, mouseX, mouseY, partialTick);
    }

    @Override
    public void drawBackground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        renderer.disableTextures();
        rp.colorMultiplier.set(backgroundColor);
        renderer.drawShape(shape, rp);
        renderer.next();
        renderer.enableTextures();
    }

    @Override
    public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        int ox = 0, oy = 0;
        int l = getLength() - scrollHeight;
        if (isHorizontal()) {
            ox = (int) (getOffset() * l);
        } else {
            oy = (int) (getOffset() * l);
        }

        renderer.disableTextures();

        scrollShape.resetState();
        scrollShape.setPosition(ox, oy);
        rp.colorMultiplier.set(scrollColor1);
        renderer.drawShape(scrollShape, rp);

        scrollShapeInner.resetState();
        scrollShapeInner.setPosition(ox, oy);
        rp.colorMultiplier.set(scrollColor2);
        renderer.drawShape(scrollShapeInner, rp);

        renderer.next();
        renderer.enableTextures();
    }

    @Subscribe
    public void onMouseOver(StateChangeEvent.HoveredStateChange<?> event) {
        if (!fade) {
            return;
        }

        if (isFocused() && !event.getState()) {
            return;
        }

        int from = event.getState() ? 0 : 255;
        int to = event.getState() ? 255 : 0;

        Animation<Alpha> anim = new Animation<>(this, new AlphaTransform(from, to).forTicks(5));

        event.getComponent().getGui().animate(anim);
    }

    @Override
    public boolean onMouseMove(int lastX, int lastY, int x, int y) {
        this.updateHoverState(x, y);
        return true;
    }

    @Override
    public boolean onButtonPress(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return true;
        }

        if (isOnScroll()) {
            return true;
        }
        scrollByStepClick(x, y);
        return true;
    }

    private void updateHoverState(int x, int y) {
        if (Mouse.isButtonDown(MouseButton.LEFT.getCode())) {
            return;
        }
        this.isScrollHovered = isScrollHoveredAt(x, y);
    }

    private boolean isScrollHoveredAt(int x, int y) {
        int pos = relativeScrollPos(x, y);
        int posStart = (int) ((getLength() - scrollHeight) * getOffset());
        int posEnd = posStart + scrollHeight;
        return pos >= posStart && pos < posEnd;
    }

    private boolean isOnScroll() {
        return this.isScrollHovered;
    }

    private void scrollByStepClick(int x, int y) {
        if (isScrollHoveredAt(x, y)) {
            return;
        }
        int pos = relativeScrollPos(x, y);
        int posCenter = (int) (getLength() * getOffset());
        float mult = 0.5f * (pos < posCenter ? -1 : 1);
        scrollBy(((IScrollable) parent).getScrollStep() * mult);
    }

    private int relativeScrollPos(int x, int y) {
        return isHorizontal() ? relativeX(x) : relativeY(y);
    }

    @Override
    public boolean onDrag(int lastX, int lastY, int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return super.onDrag(lastX, lastY, x, y, button);
        }
        if (isFocused()) {
            if (!isOnScroll()) {
                timer.tryDo(() -> scrollByStepClick(x, y));
                return true;
            }
            onScrollBy(x - lastX, y - lastY);
        }
        return true;
    }

    @Override public void onDragTick(int mouseX, int mouseY, float partialTick) {
        if (isFocused() && !isOnScroll()) {
            timer.tryDo(() -> scrollByStepClick(mouseX, mouseY));
        }
    }

    protected void onScrollBy(int x, int y) {
        int l = getLength() - scrollHeight;
        int pos = isHorizontal() ? x : y;
        scrollBy((float) pos / l);
    }

    @Override
    public boolean onScrollWheel(int x, int y, int delta) {
        if ((isHorizontal() != GuiScreen.isShiftKeyDown()) && !isHovered()) {
            return super.onScrollWheel(x, y, delta);
        }
        float step = getScrollable().getScrollStep();
        if (step <= 0) {
            return false;
        }
        scrollBy(-delta * step);
        //true = stop
        float o = getOffset();
        if (!Float.isFinite(o)) {
            return false;
        }
        return !(delta > 0 && o == 0 || delta < 0 && o == 1);
    }
}
