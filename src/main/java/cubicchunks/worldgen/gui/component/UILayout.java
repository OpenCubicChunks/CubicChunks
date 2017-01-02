package cubicchunks.worldgen.gui.component;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.control.UIScrollBar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cubicchunks.worldgen.gui.ExtraGui;

public abstract class UILayout<T extends UILayout<T, LOC>, LOC> extends UIContainer<T> {
	private int lastWidth = Integer.MIN_VALUE, lastHeight = Integer.MIN_VALUE;
	private UIOptionScrollbar scrollbar;

	private boolean isInit = false;
	private BiMap<UIComponent<?>, LOC> entries = HashBiMap.create();

	public UILayout(ExtraGui gui) {
		super(gui);
	}

	protected abstract LOC findNextLocation();

	/**
	 * Recalculate positions and sizes of components
	 */
	protected abstract void layout();

	/**
	 * Done before the first layout, when init() is called.
	 * <p>
	 * Allows to add initialization logic for example creating data structures that help with layout.
	 * <p>
	 * After this method is called no components can be added or removed.
	 */
	protected abstract void initLayout();

	protected abstract void onAdd(UIComponent<?> comp, LOC at);

	protected abstract void onRemove(UIComponent<?> comp, LOC at);

	protected boolean isInit() {
		return this.isInit;
	}

	protected void checkNotInitialized() {
		if (isInit()) {
			throw new IllegalStateException("Already initialized");
		}
	}

	protected void checkInitialized() {
		if (!isInit()) {
			throw new IllegalStateException("Not initialized");
		}
	}

	protected Map<UIComponent<?>, LOC> componentToLocationMap() {
		return entries;
	}

	protected Map<LOC, UIComponent<?>> locationToComponentMap() {
		return entries.inverse();
	}

	protected LOC locationOf(UIComponent<?> comp) {
		return entries.get(comp);
	}

	public T init() {
		this.isInit = true;
		this.entries.forEach((c, loc) -> super.add(c));
		this.entries = Maps.unmodifiableBiMap(entries);
		this.initLayout();
		this.scrollbar = new UIOptionScrollbar((ExtraGui) getGui(), (T) this, UIScrollBar.Type.VERTICAL);
		this.scrollbar.setPosition(6, 0, Anchor.RIGHT);
		this.scrollbar.setVisible(true);
		this.layout();
		return (T) this;
	}

	public T add(UIComponent<?> component, LOC at) {
		this.checkNotInitialized();
		this.entries.put(component, at);
		this.onAdd(component, at);
		return (T) this;
	}

	@Override
	public void add(UIComponent<?>... components) {
		this.checkNotInitialized();
		for (UIComponent c : components) {
			add(c, findNextLocation());
		}
	}

	@Override
	public void remove(UIComponent<?> component) {
		this.checkNotInitialized();
		LOC loc = this.entries.remove(component);
		this.onRemove(component, loc);
	}

	@Override
	public void removeAll() {
		this.checkNotInitialized();
		Iterator<BiMap.Entry<UIComponent<?>, LOC>> it = this.entries.entrySet().iterator();
		while (it.hasNext()) {
			BiMap.Entry<UIComponent<?>, LOC> e = it.next();
			it.remove();
			this.onRemove(e.getKey(), e.getValue());
		}
	}

	@Override
	public float getScrollStep() {
		float contentSize = getContentHeight() - getHeight();
		float scrollStep = super.getScrollStep()*1000;
		float scrollFraction = scrollStep/contentSize;
		return scrollFraction;
	}

	@Override
	public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		this.checkInitialized();
		if (getWidth() != lastWidth || getHeight() != lastHeight) {
			lastWidth = getWidth();
			lastHeight = getHeight();
			layout();
		}
		super.drawForeground(renderer, mouseX, mouseY, partialTick);
	}
}
