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
import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.component.UIComponent;

import java.util.Iterator;
import java.util.Map;

public abstract class UIStandardLayout<T extends UIStandardLayout<T, LOC>, LOC> extends UILayout<T> {

    private BiMap<UIComponent<?>, LOC> entries = HashBiMap.create();

    public UIStandardLayout(ExtraGui gui) {
        super(gui);
    }

    protected abstract LOC findNextLocation();

    protected abstract void onAdd(UIComponent<?> comp, LOC at);

    protected abstract void onRemove(UIComponent<?> comp, LOC at);

    protected Map<UIComponent<?>, LOC> componentToLocationMap() {
        return entries;
    }

    protected Map<LOC, UIComponent<?>> locationToComponentMap() {
        return entries.inverse();
    }

    protected LOC locationOf(UIComponent<?> comp) {
        return entries.get(comp);
    }

    public T add(UIComponent<?> component, LOC at) {
        this.entries.put(component, at);
        this.onAdd(component, at);
        super.add(component);
        return (T) this;
    }


    @Override
    public void add(UIComponent<?>... components) {
        for (UIComponent c : components) {
            add(c, findNextLocation());
        }
    }

    @Override
    public void remove(UIComponent<?> component) {
        LOC loc = this.entries.remove(component);
        super.remove(component);
        this.onRemove(component, loc);
    }

    @Override
    public void removeAll() {
        Iterator<BiMap.Entry<UIComponent<?>, LOC>> it = this.entries.entrySet().iterator();
        while (it.hasNext()) {
            BiMap.Entry<UIComponent<?>, LOC> e = it.next();
            it.remove();
            super.remove(e.getKey());
            this.onRemove(e.getKey(), e.getValue());
        }
    }

}
