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

import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.component.UIComponent;

public class UIBorderLayout extends UIStandardLayout<UIBorderLayout, UIBorderLayout.Border> {

    private int currentBorder = 0;

    public UIBorderLayout(ExtraGui gui) {
        super(gui);
    }

    @Override protected Border findNextLocation() {
        Border ret = Border.values()[currentBorder];
        currentBorder++;
        if (currentBorder >= Border.values().length) {
            currentBorder = Border.values().length - 1;
        }
        return ret;
    }

    @Override protected void layout() {
        this.locationToComponentMap().forEach((loc, comp) -> {
            comp.setAnchor(loc.getAnchor());
            comp.setPosition(0, 0);
        });
    }

    @Override protected boolean isLayoutChanged() {
        return false;
    }

    @Override protected void onAdd(UIComponent<?> comp, Border at) {

    }

    @Override protected void onRemove(UIComponent<?> comp, Border at) {

    }

    public enum Border {
        //@formatter:off
        TOP_LEFT(-1, -1),   TOP(0, -1),     TOP_RIGHT(1, -1),
        LEFT(-1, 0),        CENTER(0, 0),   RIGHT(1, 0),
        BOTTOM_LEFT(-1, 1), BOTTOM(0, 1),   BOTTOM_RIGHT(1, 1);
        //@formatter:on

        private final int x;
        private final int y;

        Border(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getAnchor() {
            int anchor = 0;
            if (x == -1) {
                anchor |= Anchor.LEFT;
            } else if (x == 0) {
                anchor |= Anchor.CENTER;
            } else {
                assert x == 1;
                anchor |= Anchor.RIGHT;
            }

            if (y == -1) {
                anchor |= Anchor.TOP;
            } else if (y == 0) {
                anchor |= Anchor.MIDDLE;
            } else {
                assert y == 1;
                anchor |= Anchor.BOTTOM;
            }

            return anchor;
        }
    }
}
