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
import cubicchunks.worldgen.gui.ExtraGui;
import jline.internal.Preconditions;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.event.component.StateChangeEvent;
import net.malisis.core.util.MouseButton;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;

/**
 * Layout for 2 components. Up and down, or left and right.
 *
 * The components will together fill the whole layout. The split line is optionally movable by user.
 */
public class UISplitLayout<T extends UISplitLayout<T>> extends UIStandardLayout<T, UISplitLayout.Pos> {

    private Type splitType;

    private SizeMode sizeMode;
    private float sizeData = 1; // specifies either ratio of component weights, or size of the "main" component

    private boolean userResizable;
    private int separatorSize = 1;
    private int minSizeFirst;
    private int minSizeSecond;

    private boolean isMovingSplit;
    private int separatorClickOffset = 0;

    private final Object onVisibilityChange = new Object() {
        @Subscribe
        public void onSetVisible(StateChangeEvent.VisibleStateChange evt) {
            setNeedsLayoutUpdate();
        }
    };
    /**
     * Creates a split layout with 2 components provided
     */
    public UISplitLayout(ExtraGui gui, Type splitType, @Nullable UIComponent<?> first, @Nullable UIComponent<?> second) {
        super(gui);
        if (first != null) {
            this.add(first, Pos.FIRST);
        }
        if (second != null) {
            this.add(second, Pos.SECOND);
        }
        this.splitType = splitType;
        this.sizeMode = SizeMode.WEIGHT;

    }

    // Note: setSizeMode doesn't exist on purpose because there is no reasonable way to use it without also setting new weights of sizes

    /**
     * Sets weights of the components, and switches to specifying sizes component sizes by weight.
     * A component will take fraction of space specified by {@code componentWeight/sumOfAllWeights}.
     * If there is only one component added, the space that would be used by second component remains unused.
     * Any previously set size or weight is discarded.
     *
     * @param weight1 weight of the first component
     * @param weight2 weight of the second component
     * @return {@code this}
     */
    public T sizeWeights(float weight1, float weight2) {
        this.sizeData = weight1 / weight2;
        this.sizeMode = SizeMode.WEIGHT;
        this.setNeedsLayoutUpdate();
        return self();
    }

    /**
     * Sets size of the selected component slot, and switches to specifying sizes by size of that component.
     * Any previously set size or weight is discarded.
     *
     * @param componentPos the component slot to specify size of
     * @param size size in pixels
     * @return {@code this}
     */
    public T setSizeOf(Pos componentPos, int size) {
        Preconditions.checkNotNull(componentPos);
        this.sizeData = size;
        this.sizeMode = SizeMode.byPos(componentPos);
        this.setNeedsLayoutUpdate();
        return self();
    }

    public T setSeparatorSize(int size) {
        this.separatorSize = size;
        this.setNeedsLayoutUpdate();
        return self();
    }

    /**
     * If set to true, user will be able to change the split line. By default it's false
     *
     * @return {@code this}
     */
    public T userResizable(boolean resizable) {
        this.userResizable = resizable;
        return self();
    }

    /**
     * Checks whether the split line can be moved my user.
     *
     * @return true if the split line can be moved by user
     */
    public boolean isUserResizable() {
        return userResizable;
    }

    /**
     * Specifies minimum size of a component
     *
     * @return {@code this}
     */
    public T setMinimumUserComponentSize(Pos pos, int minSize) {
        Preconditions.checkNotNull(pos);
        if (pos == Pos.FIRST) {
            minSizeFirst = minSize;
        } else {
            assert pos == Pos.SECOND;
            minSizeSecond = minSize;
        }
        return self();
    }

    public T setSplitType(Type type) {
        Preconditions.checkNotNull(type);
        this.splitType = type;
        this.setNeedsLayoutUpdate();
        return self();
    }

    @Override protected boolean isLayoutChanged() {
        // nothing really can change here, the layout is in full control of component sizes
        // all changes are handled by base layout
        return false;
    }

    @Override protected void onAdd(UIComponent<?> comp, Pos at) {
        comp.register(onVisibilityChange);
    }

    @Override protected void onRemove(UIComponent<?> comp, Pos at) {
        comp.unregister(onVisibilityChange);
    }

    @Override protected Pos findNextLocation() {
        if (this.locationToComponentMap().containsKey(Pos.FIRST)) {
            if (this.locationToComponentMap().containsKey(Pos.SECOND)) {
                throw new UnsupportedOperationException("No more locations");
            }
            return Pos.SECOND;
        }
        return Pos.FIRST;
    }

    @Override protected boolean canAutoSizeX() {
        return this.splitType == Type.STACKED;
    }

    @Override protected boolean canAutoSizeY() {
        return this.splitType == Type.SIDE_BY_SIDE;
    }

    @Override protected void layout() {
        boolean shouldResize = getFirst() != null && getFirst().isVisible() && getSecond() != null && getSecond().isVisible();

        int sizeFirst = getSizeFirst();
        if (shouldResize && sizeFirst < minSizeFirst) {
            setSizeOf(Pos.FIRST, minSizeFirst);
            sizeFirst = minSizeFirst;
        }
        int sizeSecond = getSizeSecond();
        if (shouldResize && sizeSecond < minSizeSecond) {
            setSizeOf(Pos.SECOND, minSizeSecond);
            sizeSecond = minSizeSecond;
            sizeFirst = getSizeFirst();
        }
        assert !shouldResize || sizeFirst + sizeSecond == getTotalAvailableSize();

        int offsetSecond = sizeFirst + getSeparatorSize();

        setPosSize(getFirst(), 0, sizeFirst);
        setPosSize(getSecond(), offsetSecond, sizeSecond);
    }


    @Override public boolean onDrag(int lastX, int lastY, int x, int y, MouseButton button) {
        if (!isUserResizable()) {
            return false;
        }
        int lastCoord = splitType.getSizeCoord(lastX, lastY) - getStartCoordWithPadding();
        int currCoord = splitType.getSizeCoord(x, y) - getStartCoordWithPadding();
        if (isMovingSplit || isSeparator(lastCoord)) {
            if (!isMovingSplit) {
                separatorClickOffset = currCoord - getSizeFirst();
            }
            isMovingSplit = true;
            resizeTo(currCoord - separatorClickOffset);
        }
        return true;
    }

    @Override public boolean onButtonRelease(int x, int y, MouseButton button) {
        if (button == MouseButton.LEFT) {
            this.isMovingSplit = false;
        }
        return true;
    }

    @Override public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        super.drawForeground(renderer, mouseX, mouseY, partialTick);
        renderer.currentComponent = this;
        if (!isMovingSplit && (!isSeparator(splitType.getSizeCoord(mouseX, mouseY) - getStartCoordWithPadding())) || !this
                .isInsideBounds(mouseX, mouseY)) {
            return;
        }
        int alpha = (isMovingSplit || Mouse.isButtonDown(0)) ? 60 : 40;
        int offset = getSizeFirst() + getStartPadding();
        int size = getSeparatorSize();
        if (splitType == Type.STACKED) {
            renderer.drawRectangle(0, offset, 0, getWidth(), size, 0xFFFFFF, alpha);
        } else {
            renderer.drawRectangle(offset, 0, 0, size, getHeight(), 0xFFFFFF, alpha);
        }
    }

    private void resizeTo(int sizeCoord) {

        int first = sizeCoord;
        if (first < minSizeFirst) {
            first = minSizeFirst;
        }
        int second = getTotalAvailableSize() - first;
        if (second < minSizeSecond) {
            second = minSizeSecond;
            first = getTotalAvailableSize() - second;
            if (first < minSizeFirst) {
                return; // if minFirst and minSecond don't fit there - do nothing and ignore any attempts at user resizing
            }
        }
        switch (sizeMode) {
            case WEIGHT: {
                this.sizeData = first / (float) second;
                break;
            }
            case SIZE_FIRST: {
                this.sizeData = first;
                break;
            }
            case SIZE_SECOND: {
                this.sizeData = getTotalAvailableSize() - first;
                break;
            }
            default:
                throw new Error();
        }
        setNeedsLayoutUpdate();
    }

    private int getSizeFirst() {
        if (!isUserResizable()) {
            if (getFirst() == null || !getFirst().isVisible()) {
                return 0;
            }
            if (getSecond() == null || !getSecond().isVisible()) {
                return getTotalAvailableSize();
            }
        }
        return getRawSizeFirst();
    }

    private int getRawSizeFirst() {
        if (sizeMode == SizeMode.WEIGHT) {
            // x = (totalWidth*ratio)/(ratio+1)
            return Math.round(getTotalAvailableSize() * sizeData / (sizeData + 1));
        } else if (sizeMode == SizeMode.SIZE_FIRST) {
            return Math.round(sizeData);
        } else {
            assert sizeMode == SizeMode.SIZE_SECOND;
            return getTotalAvailableSize() - getRawSizeSecond();
        }
    }

    private int getSizeSecond() {
        if (!isUserResizable()) {
            if (getSecond() == null || !getSecond().isVisible()) {
                return 0;
            }
            if (getFirst() == null || !getFirst().isVisible()) {
                return getTotalAvailableSize();
            }
        }
        return getRawSizeSecond();
    }

    private int getRawSizeSecond() {
        if (sizeMode == SizeMode.WEIGHT || sizeMode == SizeMode.SIZE_FIRST) {
            return getTotalAvailableSize() - getRawSizeFirst();
        } else {
            assert sizeMode == SizeMode.SIZE_SECOND;
            return Math.round(sizeData);
        }
    }

    private void setPosSize(@Nullable UIComponent<?> component, int offset, int size) {
        if (component == null) {
            return;
        }
        if (splitType == Type.STACKED) {
            component.setSize(getWidth() - getLeftPadding() - getRightPadding(), size);
            component.setPosition(0, offset);
        } else {
            assert splitType == Type.SIDE_BY_SIDE;
            component.setSize(size, getHeight() - getTopPadding() - getBottomPadding());
            component.setPosition(offset, 0);
        }
    }

    public int getSeparatorSize() {
        if (!isUserResizable()) {
            return 0;
        }
        return separatorSize;
    }

    private boolean isSeparator(int pos) {
        if (!isUserResizable()) {
            return false;
        }
        int sizeFirst = getSizeFirst();
        return pos >= sizeFirst && pos < sizeFirst + getSeparatorSize();
    }

    @Nullable
    public UIComponent<?> getFirst() {
        return locationToComponentMap().get(Pos.FIRST);
    }

    @Nullable
    public UIComponent<?> getSecond() {
        return locationToComponentMap().get(Pos.SECOND);
    }

    private int getTotalAvailableSize() {
        return (splitType == Type.STACKED ? getHeight() : getWidth()) - getSeparatorSize() - getStartPadding() - getEndPadding();
    }

    private int getStartPadding() {
        return splitType == Type.STACKED ? getTopPadding() : getLeftPadding();
    }

    private int getEndPadding() {
        return splitType == Type.STACKED ? getBottomPadding() : getRightPadding();
    }

    private int getStartCoordWithPadding() {
        return getStartPadding() + getStartCoordNoPadding();
    }

    private int getStartCoordNoPadding() {
        return (splitType == Type.STACKED ? screenY() : screenX());
    }

    public enum Type {
        /**
         * The first component is up, the second component is down
         */
        STACKED,
        /**
         * The first component is left and the second component is right
         */
        SIDE_BY_SIDE;

        public int getSizeCoord(int x, int y) {
            return this == STACKED ? y : x;
        }
    }

    public enum Pos {
        FIRST, SECOND;
    }

    public enum SizeMode {
        WEIGHT, SIZE_FIRST, SIZE_SECOND;

        public static SizeMode byPos(Pos pos) {
            switch (pos) {
                case FIRST:
                    return SIZE_FIRST;
                case SECOND:
                    return SIZE_SECOND;
                default:
                    throw new Error();
            }
        }
    }
}
