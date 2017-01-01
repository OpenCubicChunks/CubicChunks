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

import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.renderer.animation.transformation.ITransformable;
import net.malisis.core.renderer.font.FontOptions;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

public class UIMultilineLabel extends UIContainer<UIMultilineLabel> {
	private String text = "";
	private final List<UILabel> labels = new ArrayList<>();
	private int labelAnchor;
	private FontOptions fontOptions = FontOptions.builder().color(0x444444).build();

	public UIMultilineLabel(MalisisGui gui, String text) {
		super(gui);
		this.setText(text);
	}

	public UIMultilineLabel(MalisisGui gui) {
		this(gui, "");
	}

	public UIMultilineLabel setText(String text) {
		this.text = I18n.format(text).replaceAll("\r\n", "\n");
		updateSizeAndLabels();
		return this;
	}

	public UIMultilineLabel setTextAnchor(int anchor) {
		for (UILabel label : labels) {
			label.setAnchor(anchor);
		}
		this.labelAnchor = anchor;
		return this;
	}

	private void updateSizeAndLabels() {
		removeAll();

		final String[] lines = getLines();
		adjustLabelAmount(lines.length);
		updateLabelsText(lines);
		updateContainerSize();

		labels.forEach(this::add);
	}

	private String[] getLines() {
		final String[] lines;
		if (!this.text.contains("\n")) {
			lines = new String[]{text};
		} else {
			lines = this.text.split("\n");
		}
		return lines;
	}

	private void updateLabelsText(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			labels.get(i).setText(lines[i].trim());
		}
	}

	private void adjustLabelAmount(int newAmount) {
		if (labels.size() == newAmount) {
			return;
		}
		List<UILabel> temp = new ArrayList<>();

		int numFromOld = Math.min(labels.size(), newAmount);
		for (int i = 0; i < numFromOld; i++) {
			temp.add(labels.get(i));
		}
		labels.clear();
		for (int i = numFromOld; i < newAmount; i++) {
			temp.add(new UILabel(getGui()).setAnchor(labelAnchor).setFontOptions(fontOptions));
		}
		labels.addAll(temp);
	}

	private void updateContainerSize() {
		int totalHeight = 0;
		int maxWidth = 0;
		for (UILabel label : labels) {
			label.setPosition(0, totalHeight);
			totalHeight += label.getHeight();
			if (label.getWidth() > maxWidth) {
				maxWidth = label.getWidth();
			}
		}
		this.setSize(maxWidth, totalHeight);
	}

	public UIMultilineLabel setFontOptions(FontOptions options) {
		fontOptions = options;
		labels.forEach(l -> l.setFontOptions(options));
		return this;
	}
}
