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

import net.malisis.core.client.gui.component.UIComponent;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.worldgen.gui.ExtraGui;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UIVerticalTableLayout extends UILayout<UIVerticalTableLayout, UIVerticalTableLayout.GridLocation> {
	private final int columns;

	private int nextX, nextY;
	private int insetUp;
	private int insetDown;
	private int insetLeft;
	private int insetRight;
	private Map<Integer, UIComponent<?>[]> rows = new HashMap<>();
	private int totalRows;

	/**
	 * Default constructor, creates the components list.
	 *
	 * @param gui the gui
	 */
	public UIVerticalTableLayout(ExtraGui gui, int columns) {
		super(gui);
		this.columns = columns;
	}

	public UIVerticalTableLayout setInsets(int up, int down, int left, int right) {
		this.checkNotInitialized();
		this.insetUp = up;
		this.insetDown = down;
		this.insetLeft = left;
		this.insetRight = right;
		return this;
	}

	@Override protected GridLocation findNextLocation() {
		while (!isFree(nextX, nextY)) {
			nextX++;
			if (nextX >= columns) {
				nextX = 0;
				nextY++;
			}
		}
		return new GridLocation(nextX, nextY, 1);
	}

	private boolean isFree(int x, int y) {
		return !rows.containsKey(y) || this.rows.get(y)[x] == null;
	}

	@Override
	public void initLayout() {
		this.rows = new HashMap<>();
		int maxRow = 0;
		for (Map.Entry<UIComponent<?>, GridLocation> e : componentToLocationMap().entrySet()) {
			int row = e.getValue().gridY;
			if (row > maxRow) {
				maxRow = row;
			}
			if (!rows.containsKey(row)) {
				rows.put(row, new UIComponent[columns]);
			}
			int column = e.getValue().gridX;
			UIComponent<?>[] rowArr = rows.get(row);
			if (rowArr[column] != null) {
				throw new IllegalStateException("Found 2 components at the same position: row=" + row + ", column=" + column);
			}
			rowArr[column] = e.getKey();
		}
		this.totalRows = maxRow + 1;
	}

	@Override protected void onAdd(UIComponent<?> comp, GridLocation at) {
		int row = at.gridY;
		int column = at.gridX;
		int columns = at.cellsX;
		if (!rows.containsKey(row)) {
			rows.put(row, new UIComponent[this.columns]);
		}
		UIComponent<?>[] rowArr = rows.get(row);
		for (int i = column; i < column + columns; i++) {
			if (rowArr[i] != null) {
				throw new IllegalStateException("Found 2 components at the same position: row=" + row + ", column=" + column);
			}
			rowArr[i] = comp;
		}
		totalRows = Math.max(row + 1, totalRows);
	}

	@Override protected void onRemove(UIComponent<?> comp, GridLocation at) {

	}

	@Override
	protected void layout() {
		this.checkInitialized();
		if (getParent() == null) {
			return;
		}
		int width = getWidth() - getHorizontalPadding()*2;

		final double noInsetSizeX = width/(float) columns;

		int[] noInsetRowHeights = new int[totalRows];

		for (Map.Entry<Integer, UIComponent<?>[]> rowEntry : rows.entrySet()) {
			int rowNumber = rowEntry.getKey();
			UIComponent<?>[] rowArr = rowEntry.getValue();

			int maxHeight = 0;
			for (UIComponent e : rowArr) {
				if (e != null) {
					maxHeight = Math.max(maxHeight, e.getHeight());
				}
			}
			noInsetRowHeights[rowNumber] = maxHeight + insetUp + insetDown;
		}

		int currentY = 0;
		for (int rowNumber = 0; rowNumber < totalRows; rowNumber++) {
			UIComponent<?>[] entries = rows.get(rowNumber);
			if (entries == null) {
				continue;
			}
			int currentRowHeight = noInsetRowHeights[rowNumber];
			int ySpaceForComponent = currentRowHeight - insetDown - insetUp;
			for (UIComponent<?> entry : entries) {
				if (entry == null) {
					continue;
				}
				UIComponent<?> comp = entry;
				GridLocation loc = locationOf(comp);

				double noInsetPosX = noInsetSizeX*loc.gridX;
				double noInsetPosY = currentY;

				double posX = noInsetPosX + insetLeft;
				double posY = noInsetPosY + insetUp + (ySpaceForComponent - comp.getHeight())/2;

				double sizeX = noInsetSizeX*loc.cellsX - insetLeft - insetRight;
				int sizeY = comp.getRawHeight();

				comp.setPosition((int) posX, (int) posY);
				comp.setSize((int) sizeX, sizeY);
			}
			currentY += currentRowHeight;
		}
	}

	@Override
	public void calculateContentSize() {
		super.calculateContentSize();
		this.contentHeight += insetDown + insetDown;
	}

	public final static class GridLocation {
		private final int gridX;
		private final int gridY;
		private final int cellsX;

		public GridLocation(int gridX, int gridY, int cellsX) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.cellsX = cellsX;
		}
	}
}
