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

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.control.UIScrollBar;
import net.malisis.core.client.gui.component.control.UISlimScrollbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UIGridContainer extends UIContainer<UIGridContainer> {
	private final Set<ComponentEntry> entries = new HashSet<>();
	private int insetUp;
	private int insetDown;
	private int insetLeft;
	private int insetRight;
	private boolean isInit = false;
	private int columns;
	private int lastWidth = Integer.MIN_VALUE;
	private Map<Integer, ComponentEntry[]> rows;
	private int totalRows;
	private UIOptionScrollbar scrollbar;

	/**
	 * Default constructor, creates the components list.
	 *
	 * @param gui the gui
	 */
	public UIGridContainer(MalisisGui gui) {
		super(gui);
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param title the title
	 */
	public UIGridContainer(MalisisGui gui, String title) {
		super(gui, title);
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param width the width
	 * @param height the height
	 */
	public UIGridContainer(MalisisGui gui, int width, int height) {
		super(gui, width, height);
	}

	/**
	 * Instantiates a new {@link UIContainer}.
	 *
	 * @param gui the gui
	 * @param title the title
	 * @param width the width
	 * @param height the height
	 */
	public UIGridContainer(MalisisGui gui, String title, int width, int height) {
		super(gui, title, width, height);
	}

	public UIGridContainer setInsets(int up, int down, int left, int right) {
		if (isInit) {
			throw new IllegalStateException("Already initialized");
		}
		this.insetUp = up;
		this.insetDown = down;
		this.insetLeft = left;
		this.insetRight = right;
		return this;
	}

	public UIGridContainer setColumns(int columns) {
		if (isInit) {
			throw new IllegalStateException("Already initialized");
		}
		this.columns = columns;
		return this;
	}

	public UIGridContainer add(UIComponent<?> component, int gridX, int gridY) {
		return add(component, gridX, gridY, 1, 1);
	}

	public UIGridContainer add(UIComponent<?> component, int gridX, int gridY, int cellsX, int cellsY) {
		if (isInit) {
			throw new IllegalStateException("Already initialized");
		}
		entries.add(new ComponentEntry(component, gridX, gridY, cellsX, cellsY));
		return this;
	}

	public UIGridContainer init() {
		this.isInit = true;
		this.rows = new HashMap<>();
		int maxRow = 0;
		for (ComponentEntry e : entries) {
			add(e.comp);
			int row = e.gridY;
			if (row > maxRow) {
				maxRow = row;
			}
			if (!rows.containsKey(row)) {
				rows.put(row, new ComponentEntry[columns]);
			}
			int column = e.gridX;
			ComponentEntry[] rowArr = rows.get(row);
			if (rowArr[column] != null) {
				throw new IllegalStateException("Found 2 components at the same position: row=" + row + ", column=" + column);
			}
			rowArr[column] = e;
		}
		this.totalRows = maxRow + 1;

		this.scrollbar = new UIOptionScrollbar(getGui(), this, UIScrollBar.Type.VERTICAL);
		this.scrollbar.setPosition(6, 0, Anchor.RIGHT);
		this.scrollbar.setVisible(true);
		return this;
	}

	private void layout() {
		if (!isInit) {
			throw new IllegalStateException("Can't reset layout when not initialized");
		}
		if (getParent() == null) {
			return;
		}
		int width = getWidth() - getHorizontalPadding()*2;

		final double noInsetSizeX = width/(float) columns;

		int[] noInsetRowHeights = new int[totalRows];

		for (Map.Entry<Integer, ComponentEntry[]> rowEntry : rows.entrySet()) {
			int rowNumber = rowEntry.getKey();
			ComponentEntry[] rowArr = rowEntry.getValue();

			int maxHeight = 0;
			for (ComponentEntry e : rowArr) {
				if (e != null) {
					maxHeight = Math.max(maxHeight, e.comp.getHeight());
				}
			}
			noInsetRowHeights[rowNumber] = maxHeight + insetUp + insetDown;
		}

		int currentY = 0;
		for (int rowNumber = 0; rowNumber < totalRows; rowNumber++) {
			ComponentEntry[] entries = rows.get(rowNumber);
			if (entries == null) {
				continue;
			}
			for (ComponentEntry entry : entries) {
				if (entry == null) {
					continue;
				}
				UIComponent<?> comp = entry.comp;

				double noInsetPosX = noInsetSizeX*entry.gridX;
				double noInsetPosY = currentY;

				double posX = noInsetPosX + insetLeft;
				double posY = noInsetPosY + insetUp;

				double sizeX = noInsetSizeX*entry.cellsX - insetLeft - insetRight;
				int sizeY = comp.getRawHeight();

				comp.setPosition((int) posX, (int) posY);
				comp.setSize((int) sizeX, sizeY);
			}
			currentY += noInsetRowHeights[rowNumber];
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
	public void calculateContentSize() {
		super.calculateContentSize();
		this.contentHeight += insetDown + insetDown;
	}

	@Override
	public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		if (!isInit) {
			throw new IllegalStateException("Can't draw when not initialized");
		}
		if (getWidth() != lastWidth) {
			lastWidth = getWidth();
			layout();
		}
		super.drawForeground(renderer, mouseX, mouseY, partialTick);
	}

	private final class ComponentEntry {
		final UIComponent<?> comp;
		final int gridX;
		final int gridY;
		final int cellsX;
		final int cellsY;

		ComponentEntry(UIComponent<?> comp, int gridX, int gridY, int cellsX, int cellsY) {
			this.comp = comp;
			this.gridX = gridX;
			this.gridY = gridY;
			this.cellsX = cellsX;
			this.cellsY = cellsY;
		}
	}
}
