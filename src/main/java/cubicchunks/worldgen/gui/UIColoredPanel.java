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

import net.malisis.core.client.gui.ClipArea;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.element.GuiShape;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.renderer.animation.transformation.ITransformable;

public class UIColoredPanel extends UIContainer<UIColoredPanel> implements ITransformable.Color, ITransformable.Alpha {
	private final GuiShape lineShape;
	private int color;
	private int bgAlpha;

	public UIColoredPanel(MalisisGui gui) {
		super(gui);
		this.shape = new SimpleGuiShape();
		this.lineShape = new SimpleGuiShape();
		setBackgroundAlpha(128);
		setColor(0);
	}

	@Override
	public ClipArea getClipArea() {
		return new ClipArea(this);
	}

	@Override
	public void drawBackground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		rp.useTexture.set(false);

		renderer.disableTextures();

		rp.alpha.set(bgAlpha);
		rp.colorMultiplier.set(color);
		renderer.drawShape(shape, rp);

		renderer.next();
		renderer.enableTextures();
	}

	@Override
	public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		super.drawForeground(renderer, mouseX, mouseY, partialTick);

		renderer.currentComponent = this;
		renderer.disableTextures();
		{
			rp.useTexture.set(false);
			rp.usePerVertexAlpha.set(true);
			rp.colorMultiplier.set(color);

			lineShape.resetState();
			lineShape.setSize(getWidth(), 4);
			lineShape.getVertexes("Bottom").forEach(v -> v.setAlpha(0x00));

			renderer.drawShape(lineShape, rp);
		}
		{
			rp.useTexture.set(false);
			rp.usePerVertexAlpha.set(true);
			rp.colorMultiplier.set(color);

			lineShape.resetState();
			lineShape.setSize(getWidth(), 4);
			lineShape.setPosition(0, getHeight() - 4);
			lineShape.getVertexes("Top").forEach(v -> v.setAlpha(0x00));

			renderer.drawShape(lineShape, rp);
		}
		renderer.next();
		renderer.enableTextures();
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	public void setBackgroundAlpha(int alpha) {
		this.bgAlpha = alpha;
	}
}
