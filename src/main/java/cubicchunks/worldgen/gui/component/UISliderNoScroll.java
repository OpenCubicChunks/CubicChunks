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

import com.google.common.base.Converter;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.control.IControlComponent;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.minecraft.util.math.MathHelper;

public class UISliderNoScroll<T> extends UISlider<T> {

    public UISliderNoScroll(MalisisGui gui, int width, Converter<Float, T> converter, String text) {
        super(gui, width, converter, text);
    }

    @Override
    public boolean onScrollWheel(int x, int y, int delta) {
        if (parent != null && !(this instanceof IControlComponent)) {
            return parent.onScrollWheel(x, y, delta);
        }
        return false;
    }

    @Override
    public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
        int currWidth = this.width;
        this.width = getWidth();
        super.drawForeground(renderer, mouseX, mouseY, partialTick);
        this.width = currWidth;
    }

    @Override
    public void slideTo(int x) {
        int l = getWidth() - SLIDER_WIDTH;
        int pos = relativeX(x);
        pos = MathHelper.clamp(pos - SLIDER_WIDTH / 2, 0, l);
        slideTo((float) pos / l);
    }

}
