package cubicchunks.worldgen.gui.component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import cubicchunks.worldgen.gui.ExtraGui;
import net.malisis.core.client.gui.component.UIComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UIList<E, C extends UIComponent<C>, T extends UIList<E, C, T>> extends UIVerticalTableLayout<T> {

    private final ArrayList<E> data;
    private final Function<E, C> componentFactory;
    private final Map<E, C> componentMap = new HashMap<>();

    /**
     * Default constructor, creates the components list.
     *

     * @param gui the gui
     */
    public UIList(ExtraGui gui, Collection<E> data, Function<E, C> componentFactory) {
        super(gui, 1);
        this.data = new ArrayList<>();
        this.componentFactory = componentFactory;

        data.forEach(e -> add(e));
    }

    public void remove(E element) {
        remove(componentMap.remove(element));
    }

    public void add(E element) {
        C component = componentFactory.apply(element);
        add(component);
        componentMap.put(element, component);
    }

    public void moveTo(E elem, int idx) {
        C comp = componentMap.get(elem);

        GridLocation gl = componentToLocationMap().get(comp);

        for (int i = totalRows - 1; i >= gl.gridY; i--) {
            UIComponent<?> currComp = rows.get(i)[0];

            // remove it and add it one row
            remove(currComp);
            add(currComp, new GridLocation(0, i + 1, 1));
        }
    }

    /**
     * Returns unmodifiable view of map from element to component
     */
    public Map<E, C> getAll() {
        return Collections.unmodifiableMap(componentMap);
    }
}
