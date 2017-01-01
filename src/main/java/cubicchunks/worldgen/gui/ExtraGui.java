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

import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.util.MouseButton;

import org.lwjgl.input.Mouse;

import java.util.Map;
import java.util.WeakHashMap;

import cubicchunks.worldgen.gui.component.IDragTickable;

public abstract class ExtraGui extends MalisisGui {

	private Map<IDragTickable, DragTickableWrapper> set = new WeakHashMap<>();

	@Override
	public void update(int mouseX, int mouseY, float partialTick) {
		set.values().forEach(tc -> tc.tick(mouseX, mouseY, partialTick));
	}

	public <T extends UIComponent<X> & IDragTickable, X extends UIComponent<X>> void registerDragTickable(T t) {
		set.put(t, new DragTickableWrapper(t));
	}

	public static final class DragTickableWrapper {
		private final IDragTickable component;
		private boolean beforeClickHovered = false;

		public DragTickableWrapper(IDragTickable component) {
			this.component = component;
		}

		void tick(int mouseX, int mouseY, float partialTick) {
			if (((UIComponent<?>) component).isFocused() && beforeClickHovered && Mouse.isButtonDown(MouseButton.LEFT.getCode()))
				component.onDragTick(mouseX, mouseY, partialTick);
			else
				beforeClickHovered = ((UIComponent<?>) component).isHovered();
		}
	}
}
