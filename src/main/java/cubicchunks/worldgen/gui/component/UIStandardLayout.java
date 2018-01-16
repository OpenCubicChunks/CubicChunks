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
