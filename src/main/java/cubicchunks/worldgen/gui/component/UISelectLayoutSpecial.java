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

import com.google.common.collect.Iterables;
import com.google.common.eventbus.Subscribe;
import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.event.component.SpaceChangeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is a replacement of UISelect that works as any user would expect with my custom layouts.
 * The original one increases it's height when expanded, which is wrong when using layouts.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UISelectLayoutSpecial<T> extends UISelect<T> implements ISpecialLayoutSize {

    public UISelectLayoutSpecial(MalisisGui gui, int width, @Nullable Iterable<T> values) {
        super(gui, width, values);
        this.register(new Object() {
            @Subscribe
            public void onResize(SpaceChangeEvent.SizeChangeEvent evt) {
                UISelectLayoutSpecial.this.setMaxExpandedWidth(evt.getNewWidth());
            }
        });
        if (values != null && values.iterator().hasNext()) {
            setSelectedOption(values.iterator().next());
        }
    }

    public UISelectLayoutSpecial(MalisisGui gui, int width) {
        this(gui, width, null);
    }

    @Override public int getLayoutWidth() {
        return this.getWidth();
    }

    @Override public int getLayoutHeight() {
        boolean expanded = this.expanded;
        this.expanded = false;
        int height = super.getHeight();
        this.expanded = expanded;
        return height;
    }

    @Override
    public T selectPrevious() {
        if (selectedOption == null) {
            return selectFirst();
        }

        Option<T> option = Iterables.getFirst(options, null);
        for (Option<T> opt : this) {
            if (opt.isDisabled()) {
                continue;
            }
            if (opt.equals(selectedOption)) {
                return select(option);
            }
            option = opt;
        }
        //should not happen
        return null;
    }
}
