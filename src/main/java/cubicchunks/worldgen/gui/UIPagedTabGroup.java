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
package cubicchunks.worldgen.gui;

import com.google.common.eventbus.Subscribe;

import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UIPagedTabGroup extends UIContainer<UIPagedTabGroup> {

	private final UIButton previous;
	private final UIButton next;
	private final List<UIComponent<?>> componentsOrdered = new ArrayList<>();
	private int currentTab;

	/**
	 * Default constructor, creates the components list.
	 *
	 * @param gui the gui
	 */
	public UIPagedTabGroup(MalisisGui gui, UIButton previous, UIButton next) {
		super(gui);
		this.previous = previous;
		this.next = next;
		init();
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param title the title
	 */
	public UIPagedTabGroup(MalisisGui gui, String title, UIButton previous, UIButton next) {
		super(gui, title);
		this.next = next;
		this.previous = previous;
		init();
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param width the width
	 * @param height the height
	 */
	public UIPagedTabGroup(MalisisGui gui, int width, int height, UIButton previous, UIButton next) {
		super(gui, width, height);
		this.next = next;
		this.previous = previous;
		init();
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param title the title
	 * @param width the width
	 * @param height the height
	 */
	public UIPagedTabGroup(MalisisGui gui, String title, int width, int height, UIButton previous, UIButton next) {
		super(gui, title, width, height);
		this.next = next;
		this.previous = previous;
		init();
	}

	private void init() {
		next.register(new Object() {
			@Subscribe
			public void onClick(UIButton.ClickEvent evt) {
				updateTab(currentTab + 1, true);
			}
		});
		previous.register(new Object() {
			@Subscribe
			public void onClick(UIButton.ClickEvent evt) {
				updateTab(currentTab - 1, true);
			}
		});
		super.add(previous, next);
	}

	private void updateTab(int i, boolean removeOld) {
		if (removeOld) {
			currentTab = MathHelper.clamp(currentTab, 0, componentsOrdered.size() - 1);
			UIPagedTabGroup.super.remove(componentsOrdered.get(currentTab));
		}
		currentTab = i;
		if (componentsOrdered.size() != 0) {
			currentTab = MathHelper.clamp(currentTab, 0, componentsOrdered.size() - 1);
			UIPagedTabGroup.super.add(componentsOrdered.get(currentTab));
		}
		previous.setDisabled(currentTab <= 0);
		next.setDisabled(currentTab >= componentsOrdered.size() - 1);
	}

	@Override
	public void add(UIComponent<?>... comp) {
		for (UIComponent<?> c : comp) {
			componentsOrdered.add(c);
		}
		updateTab(currentTab, true); //to refresh possibly disabled buttons
	}

	@Override
	public void removeAll() {
		componentsOrdered.forEach(c -> super.remove(c));
		updateTab(0, false);
		componentsOrdered.clear();
	}

	@Override
	public void remove(UIComponent<?> component) {
		super.remove(component);
		componentsOrdered.remove(component);
		updateTab(currentTab, false); // update buttons and clamp value
	}
}
