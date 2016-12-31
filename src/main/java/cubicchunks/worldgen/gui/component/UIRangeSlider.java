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

import com.google.common.base.Converter;

import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.IGuiText;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.client.gui.element.XYResizableGuiShape;
import net.malisis.core.client.gui.event.ComponentEvent;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.renderer.font.MalisisFont;
import net.malisis.core.renderer.icon.GuiIcon;
import net.malisis.core.renderer.icon.provider.GuiIconProvider;
import net.malisis.core.util.MouseButton;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import cubicchunks.util.CooldownTimer;
import cubicchunks.util.MathUtil;

@SideOnly(Side.CLIENT)
public class UIRangeSlider<T> extends UIComponent<UIRangeSlider<T>> implements IGuiText<UIRangeSlider<T>> {
	private static final int FOCUSED_COLOR = 0xFFFFC0;
	private static final int HOVERED_COLOR = 0xFFFF90;

	private static final int SLIDER_WIDTH = 8;
	private static final int SLIDER_HEIGHT = 20;

	private final BiFunction<T, T, String> toString;

	private final SliderPair<T> sliderPair;

	private final GuiIconProvider sliderIcon;
	private final GuiIconProvider sliderIconMirrored;
	private final SimpleGuiShape sliderShape;
	private final SimpleGuiShape rectangle;

	/** The {@link MalisisFont} to use for this {@link UISlider}. */
	protected MalisisFont font = MalisisFont.minecraftFont;
	/** The {@link FontOptions} to use for this {@link UISlider}. */
	protected FontOptions fontOptions = FontOptions.builder().color(0xFFFFFF).shadow().build();
	/** The {@link FontOptions} to use for this {@link UISlider} when hovered. */
	protected FontOptions hoveredFontOptions = FontOptions.builder().color(0xFFFFA0).shadow().build();

	private float scrollStep = 0.01f;

	private boolean beforeClickHovered = false;

	public UIRangeSlider(MalisisGui gui, int width, Converter<Float, T> converter, BiFunction<T, T, String> toString) {
		super(gui);
		this.toString = toString;

		this.sliderPair = new SliderPair<>(this, converter);

		this.setSize(width, 20);

		this.iconProvider = new GuiIconProvider(gui.getGuiTexture().getXYResizableIcon(0, 0, 200, 20, 5));
		this.sliderIcon = new GuiIconProvider(gui.getGuiTexture().getIcon(227, 46, 8, 20));
		this.sliderIconMirrored = new GuiIconProvider((GuiIcon) gui.getGuiTexture().getIcon(227, 46, 8, 20).flip(true, false));
		this.shape = new XYResizableGuiShape();

		this.sliderShape = new SimpleGuiShape();
		this.rectangle = new SimpleGuiShape();
	}

	@Override public void draw(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		super.draw(renderer, mouseX, mouseY, partialTick);

		if ((this.isFocused() && beforeClickHovered) && Mouse.isButtonDown(MouseButton.LEFT.getCode())) {
			sliderPair.moveHovered(mouseX, mouseX);// it shouldn't be mouseX, mouseY
		} else {
			beforeClickHovered = isHovered();
		}
	}

	@Override public void drawBackground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {
		renderer.drawShape(shape, rp);
		renderer.next();

		int minSliderX = sliderPair.minSliderPos();
		int minSliderEndX = minSliderX + sliderPair.minSliderWidth();
		int maxSliderX = sliderPair.maxSliderPos();
		int maxSliderEndX = maxSliderX + sliderPair.maxSliderWidth();

		int startMin = minSliderEndX;
		int endMin = Math.round(MathUtil.lerp(1.0f/3.0f, minSliderEndX, maxSliderX));
		int center = Math.round(MathUtil.lerp(0.5f, minSliderEndX, maxSliderX));
		int startMax = Math.round(MathUtil.lerp(2.0f/3.0f, minSliderEndX, maxSliderX));
		int endMax = maxSliderX;


		renderer.disableTextures();
		rp.useTexture.set(false);
		{
			// draw dark background
			rp.colorMultiplier.set(0);
			rp.alpha.set(0x80);

			rectangle.resetState();
			rectangle.setSize(minSliderX, getHeight());
			rectangle.setPosition(0, 0);
			renderer.drawShape(rectangle, rp);

			rectangle.resetState();
			rectangle.setSize(getWidth() - maxSliderEndX, getHeight());
			rectangle.setPosition(maxSliderEndX, 0);
			renderer.drawShape(rectangle, rp);
		}
		renderer.next();
		GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
		if (this.isHovered()) {
			rp.usePerVertexColor.set(true);
			if (sliderPair.isMaxFocused() && sliderPair.isMinFocused()) {
				int color = HOVERED_COLOR;

				drawRectangle(renderer, center - endMin, getHeight(), endMin, 0, 0xFFFFFF, color);
				drawRectangle(renderer, center - endMin, getHeight(), center, 0, color, 0xFFFFFF);
			} else if (sliderPair.isMinFocused()) {
				int color = sliderPair.isMinHovered() ? HOVERED_COLOR : FOCUSED_COLOR;

				drawRectangle(renderer, endMin - startMin, getHeight(), startMin, 0, color, 0xFFFFFF);
			} else if (sliderPair.isMaxFocused()) {
				int color = sliderPair.isMaxHovered() ? HOVERED_COLOR : FOCUSED_COLOR;

				drawRectangle(renderer, endMax - startMax, getHeight(), startMax, 0, 0xFFFFFF, color);
			}

			rp.colorMultiplier.set(0xFFFFFF);
			rp.alpha.set(0xFF);
			rp.usePerVertexColor.set(false);
		}
		renderer.next();
		renderer.enableTextures();
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void drawRectangle(GuiRenderer renderer, int width, int height, int posX, int posY, int colorLeft, int colorRight) {
		rectangle.resetState();
		rectangle.setSize(width, height);
		rectangle.setPosition(posX, posY);
		rectangle.getVertexes("Left").forEach(v -> v.setColor(colorLeft));
		rectangle.getVertexes("Right").forEach(v -> v.setColor(colorRight));

		renderer.drawShape(rectangle, rp);
	}

	@Override public void drawForeground(GuiRenderer renderer, int mouseX, int mouseY, float partialTick) {

		int minSliderX = sliderPair.minSliderPos();
		int minSliderEndX = minSliderX + sliderPair.minSliderWidth();
		int maxSliderX = sliderPair.maxSliderPos();
		int maxSliderEndX = maxSliderX + sliderPair.maxSliderWidth();

		int minSliderSizeX = minSliderEndX - minSliderX;
		int maxSliderSizeX = maxSliderEndX - maxSliderX;

		{
			sliderShape.resetState();
			sliderShape.setSize(minSliderSizeX, getHeight());
			sliderShape.setPosition(minSliderX, 0);

			if (this.isHovered() && (sliderPair.isMinFocused())) {
				rp.colorMultiplier.set(sliderPair.isMinHovered() ? HOVERED_COLOR : FOCUSED_COLOR);
			} else {
				rp.colorMultiplier.set(0xFFFFFF);
			}

			rp.iconProvider.set(sliderIconMirrored);
			renderer.drawShape(sliderShape, rp);
		}
		{
			sliderShape.resetState();
			sliderShape.setSize(maxSliderSizeX, getHeight());
			sliderShape.setPosition(maxSliderX, 0);
			if (this.isHovered() && (sliderPair.isMaxFocused())) {
				rp.colorMultiplier.set(sliderPair.isMaxHovered() ? HOVERED_COLOR : FOCUSED_COLOR);
			} else {
				rp.colorMultiplier.set(0xFFFFFF);
			}

			rp.iconProvider.set(sliderIcon);
			renderer.drawShape(sliderShape, rp);
		}
		renderer.next();

		String str = toString.apply(sliderPair.getMinValue(), sliderPair.getMaxValue());
		if (!StringUtils.isEmpty(str)) {
			int x = (int) ((getWidth() - font.getStringWidth(str, fontOptions))/2);
			int y = (int) Math.ceil((getHeight() - font.getStringHeight(fontOptions))/2);

			renderer.drawText(font, str, x, y, 0, isHovered() ? hoveredFontOptions : fontOptions);
		}

	}

	@Override public MalisisFont getFont() {
		return this.font;
	}

	@Override public UIRangeSlider<T> setFont(MalisisFont font) {
		this.font = font;
		return this;
	}

	@Override public FontOptions getFontOptions() {
		return this.fontOptions;
	}

	@Override public UIRangeSlider<T> setFontOptions(FontOptions fontOptions) {
		this.fontOptions = fontOptions;
		return this;
	}

	@Override
	public boolean onDrag(int lastX, int lastY, int x, int y, MouseButton button) {
		if (button != MouseButton.LEFT) {
			return true;
		}
		this.sliderPair.moveHovered(lastX, x);
		this.sliderPair.updateHoverState(x);
		return true;
	}

	@Override
	public boolean onMouseMove(int lastX, int lastY, int x, int y) {
		this.sliderPair.updateHoverState(x);
		return true;
	}

	public UIRangeSlider<T> setRange(T min, T max) {
		sliderPair.setRange(min, max);
		return this;
	}

	/**
	 * Fired when a {@link UIComponent} gets it's value changed.
	 *
	 * @param <S> the type of the value being changed.
	 */
	public static class RangeChange<S> extends ComponentEvent<UIRangeSlider<S>> {
		protected S oldValueMin;
		protected S newValueMin;

		protected S oldValueMax;
		protected S newValueMax;

		/**
		 * @param component the component
		 * @param oldValueMin the old min value
		 * @param newValueMin the new min value
		 * @param oldValueMax the old max value
		 * @param newValueMax the new max value
		 */
		public RangeChange(UIRangeSlider<S> component, S oldValueMin, S newValueMin, S oldValueMax, S newValueMax) {
			super(component);
			this.oldValueMin = oldValueMin;
			this.newValueMin = newValueMin;
			this.oldValueMax = oldValueMax;
			this.newValueMax = newValueMax;
		}

		public S getOldValueMin() {
			return oldValueMin;
		}

		public S getNewValueMin() {
			return newValueMin;
		}

		public S getOldValueMax() {
			return oldValueMax;
		}

		public S getNewValueMax() {
			return newValueMax;
		}
	}

	private enum Part {
		MIN_SLIDER, BOTH, NONE, MAX_SLIDER
	}

	private static class SliderPair<T> {
		private final CooldownTimer moveStepCooldown = new CooldownTimer(1000/30, TimeUnit.MILLISECONDS);
		private final UIRangeSlider<T> uiSlider;
		private final Converter<Float, T> converter;

		private float offsetMin;
		private T valueMin;
		private float offsetMax;
		private T valueMax;

		private Part focusedPart = Part.NONE;
		private Part currentlyHoveredPart = Part.NONE;

		public SliderPair(UIRangeSlider<T> uiSlider, Converter<Float, T> converter) {
			this.uiSlider = uiSlider;
			this.converter = converter;

			this.offsetMin = 0.0f;
			this.valueMin = converter.convert(offsetMin);
			this.offsetMax = 1.0f;
			this.valueMax = converter.convert(offsetMax);
		}

		// input handling
		public void updateHoverState(int x) {
			// special logic is LMB is pressed
			if (Mouse.isButtonDown(MouseButton.LEFT.getCode())) {
				// if nothing is focused or both parts are focused - either dragging nothing or both at once, don't hover
				if (focusedPart == Part.BOTH || focusedPart == Part.NONE) {
					return;
				}

				// if something is already hovered...
				if (this.currentlyHoveredPart != Part.BOTH && this.currentlyHoveredPart != Part.NONE) {
					Part focused = getFocusedPartForMousePos(x);
					Part hovered = getHoveredPartForMousePos(x);
					// and something is focused and it's not the same thing as the hovered thing for new cursor position...
					if (focused != currentlyHoveredPart && focused != Part.BOTH && focused != Part.NONE) {
						// make hovered part the newly focused part - this allows for one part to seemingly "go through" the other one
						// note that for this trick to work dragging must be handled before hover state update
						this.currentlyHoveredPart = focused;
						this.focusedPart = focused;
					} else if (hovered != Part.BOTH && hovered != Part.NONE) {
						// something was already hovered, and [for new position it's either the same thing as before or both/none would be focused now]
						// just update both focused and hovered part to newly hovered part, whatever it is
						this.currentlyHoveredPart = hovered;
						this.focusedPart = hovered;
					}
				}
				return;
			}
			// not handling dragging, use normal logic
			this.currentlyHoveredPart = getHoveredPartForMousePos(x);
			this.focusedPart = getFocusedPartForMousePos(x);
		}

		// return true if anything got changed
		public boolean stepTowardsPosition(int x) {
			float prevMin = offsetMin;
			float prevMax = offsetMax;
			float mousePos = mouseXToSlidePosRaw(x);
			float slideOffset = uiSlider.scrollStep;
			if (focusedPart == Part.MIN_SLIDER) {
				if (mousePos < offsetMin) {
					if (offsetMin - slideOffset < mousePos) {
						slideMinTo(mousePos);
					} else {
						slideMinBy(-slideOffset);
					}
				} else {
					if (offsetMin + slideOffset > mousePos) {
						slideMinTo(mousePos);
					} else {
						slideMinBy(slideOffset);
					}
				}
			} else if (focusedPart == Part.MAX_SLIDER) {
				if (mousePos < offsetMax) {
					if (offsetMax - slideOffset < mousePos) {
						slideMaxTo(mousePos);
					} else {
						slideMaxBy(-slideOffset);
					}
				} else {
					if (offsetMax + slideOffset > mousePos) {
						slideMaxTo(mousePos);
					} else {
						slideMaxBy(slideOffset);
					}
				}
			}
			return prevMin != offsetMin || prevMax != offsetMax;
		}

		public void moveHovered(int fromX, int toX) {
			// it doesn't look like it works, but it's good enough

			// find out by how much the position has changed based on mouse position difference
			// this is not exact but it will be corrected later
			float lastSliderPos = mouseXToSlidePosRaw(fromX);
			float newSliderPos = mouseXToSlidePosRaw(toX);
			float diff = newSliderPos - lastSliderPos;
			// if both are focused - slide both of them
			if (focusedPart == Part.BOTH) {
				if (fromX != toX) {
					this.slideBothBy(diff);
				}
			} else if (focusedPart != Part.NONE) {
				// if something other than BOTH is focused and NONE is hovered
				if (currentlyHoveredPart == Part.NONE) {
					// step the currently focused part towards mouse position with cooldown
					moveStepCooldown.tryDo(() -> stepTowardsPosition(toX));
				} else if (fromX != toX) {
					// if a single part is focused and something is hovered
					if (currentlyHoveredPart == Part.MIN_SLIDER) {
						slideMinBy(diff);
					} else if (currentlyHoveredPart == Part.MAX_SLIDER) {
						slideMaxBy(diff);
					} else {
						assert currentlyHoveredPart == Part.BOTH;
						// currently never happens
						slideBothBy(diff);
					}
					// this will try to correct inaccuracy of slide diff
					// if hovered part for the new mouse position doesn't match the hovered part before we started
					// then the mouse cursor went off that part
					// so as long as it affects anything, try to move currently hovered part towards mouse position
					Part foundHovered = getHoveredPartForMousePos(toX);
					if (foundHovered != currentlyHoveredPart) {
						while (stepTowardsPosition(toX)) ;
					}
				}
			}
		}

		// hover and focus state access

		public boolean isMinFocused() {
			return focusedPart == Part.MIN_SLIDER || focusedPart == Part.BOTH;
		}

		public boolean isMaxFocused() {
			return focusedPart == Part.MAX_SLIDER || focusedPart == Part.BOTH;
		}

		public boolean isMinHovered() {
			return currentlyHoveredPart == Part.MIN_SLIDER;
		}

		public boolean isMaxHovered() {
			return currentlyHoveredPart == Part.MAX_SLIDER;
		}

		// accessors

		public Part getHoveredPart() {
			return this.currentlyHoveredPart;
		}

		// values accessors

		public void slideMinTo(float offset) {
			offset = MathHelper.clamp(offset, 0, 1);
			if (offset > offsetMax) {
				offset = offsetMax;
			}
			this.valueMin = converter.convert(offset);
			this.offsetMin = offset;
		}

		public void slideMaxTo(float offset) {
			offset = MathHelper.clamp(offset, 0, 1);
			if (offset < offsetMin) {
				offset = offsetMin;
			}
			this.valueMax = converter.convert(offset);
			this.offsetMax = offset;
		}

		public void slideMinBy(float offset) {
			slideMinTo(this.offsetMin + offset);
		}

		public void slideMaxBy(float offset) {
			slideMaxTo(this.offsetMax + offset);
		}

		private void slideBothBy(float diff) {
			if (offsetMin + diff < 0) {
				diff = -offsetMin;
			} else if (offsetMax + diff > 1) {
				diff = 1 - offsetMax;
			}
			slideBothTo(offsetMin + diff, offsetMax + diff);
		}

		private void slideBothTo(float min, float max) {
			min = MathHelper.clamp(min, 0, 1);
			max = MathHelper.clamp(max, 0, 1);
			this.offsetMin = min;
			this.offsetMax = max;
			this.valueMin = converter.convert(min);
			this.valueMax = converter.convert(max);
		}

		public void setRange(T min, T max) {
			if (this.valueMin.equals(min) && this.valueMax.equals(max)) {
				return;
			}
			if (!uiSlider.fireEvent(new RangeChange<>(uiSlider, this.valueMin, min, this.valueMax, max))) {
				return;
			}

			this.valueMin = min;
			this.valueMax = max;
			this.offsetMin = MathHelper.clamp(converter.reverse().convert(min), 0, 1);
			this.offsetMax = MathHelper.clamp(converter.reverse().convert(max), 0, 1);
		}

		public T getMinValue() {
			return valueMin;
		}

		public T getMaxValue() {
			return valueMax;
		}

		public float getMinOffset() {
			return offsetMin;
		}

		public float getMaxOffset() {
			return offsetMax;
		}

		// slider positions:

		public int minSliderPos() {
			int minSliderX = slidePosToRelativeXMinRaw(offsetMin);
			return minSliderX;
		}

		public int maxSliderPos() {
			final int width = Math.round(SLIDER_WIDTH*uiSlider.getHeight()/(float) SLIDER_HEIGHT);

			int minSliderX = slidePosToRelativeXMinRaw(offsetMin);
			int minSliderEndX = minSliderX + width;

			int maxSliderX = slidePosToRelativeXMinRaw(offsetMax);

			if (maxSliderX < minSliderEndX) {
				float avg = (maxSliderX + minSliderEndX)*0.5f;
				maxSliderX = MathHelper.floor(avg);
			}

			return maxSliderX;
		}

		public int minSliderWidth() {
			final int width = Math.round(SLIDER_WIDTH*uiSlider.getHeight()/(float) SLIDER_HEIGHT);

			int minSliderX = slidePosToRelativeXMinRaw(offsetMin);
			int minSliderEndX = minSliderX + width;

			int maxSliderX = slidePosToRelativeXMinRaw(offsetMax);

			int minSliderSizeX = width;
			if (maxSliderX < minSliderEndX) {
				float avg = (maxSliderX + minSliderEndX)*0.5f;
				minSliderEndX = MathHelper.ceil(avg);

				minSliderSizeX = minSliderEndX - minSliderX;
			}

			return minSliderSizeX;
		}

		public int maxSliderWidth() {
			final int width = Math.round(SLIDER_WIDTH*uiSlider.getHeight()/(float) SLIDER_HEIGHT);

			int minSliderX = slidePosToRelativeXMinRaw(offsetMin);
			int minSliderEndX = minSliderX + width;

			int maxSliderX = slidePosToRelativeXMinRaw(offsetMax);
			int maxSliderEndX = slidePosToRelativeXMinRaw(offsetMax) + width;

			int maxSliderSizeX = width;
			if (maxSliderX < minSliderEndX) {
				float avg = (maxSliderX + minSliderEndX)*0.5f;
				maxSliderX = MathHelper.floor(avg);

				maxSliderSizeX = maxSliderEndX - maxSliderX;
			}

			return maxSliderSizeX;
		}

		// internal

		private Part getHoveredPartForMousePos(int x) {
			if (isMouseOnMaxSlider(x)) {
				return Part.MAX_SLIDER;
			} else if (isMouseOnMinSlider(x)) {
				return Part.MIN_SLIDER;
			}
			return Part.NONE;
		}

		private Part getFocusedPartForMousePos(int x) {
			Part hovered = getHoveredPartForMousePos(x);
			// if something it hovered - the same thing is also focused
			if (hovered != Part.NONE) {
				return hovered;
			}
			int relX = uiSlider.relativeX(x);

			int minSliderEnd = minSliderPos() + minSliderWidth() - 1;
			int maxSliderStart = maxSliderPos();

			float minBorder = MathUtil.lerp(1.0f/3.0f, minSliderEnd, maxSliderStart);
			float maxBorder = MathUtil.lerp(2.0f/3.0f, minSliderEnd, maxSliderStart);

			if (relX <= minBorder) {
				return Part.MIN_SLIDER;
			} else if (relX >= maxBorder) {
				return Part.MAX_SLIDER;
			}
			// currently no possibility where nothing is focused
			return Part.BOTH;
		}

		private float mouseXToSlidePosRaw(int x) {
			return (uiSlider.relativeX(x) - SLIDER_WIDTH/2.0f)/(uiSlider.getWidth() - SLIDER_WIDTH);
		}

		private int slidePosToRelativeXMinRaw(float offset) {
			return Math.round(offset*(uiSlider.getWidth() - SLIDER_WIDTH));
		}

		private boolean isMouseOnMinSlider(int x) {
			int relX = uiSlider.relativeX(x);
			int minX = minSliderPos();
			int maxX = minX + minSliderWidth() - 1;
			return relX >= minX && relX <= maxX;
		}

		private boolean isMouseOnMaxSlider(int x) {
			int relX = uiSlider.relativeX(x);
			int minX = maxSliderPos();
			int maxX = minX + maxSliderWidth() - 1;
			return relX >= minX && relX <= maxX;
		}
	}
}
