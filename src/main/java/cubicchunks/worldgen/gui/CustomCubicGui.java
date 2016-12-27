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

import com.google.common.base.Converter;

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.minecraft.client.gui.GuiCreateWorld;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import mcp.MethodsReturnNonnullByDefault;

import static java.lang.Math.round;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomCubicGui extends MalisisGui {

	private final GuiCreateWorld parent;
	private UIContainer<?> tabs;

	public CustomCubicGui(GuiCreateWorld parent) {
		super();
		this.parent = parent;
	}

	/**
	 * Called before display() if this {@link MalisisGui} is not constructed yet.<br>
	 * Called when Ctrl+R is pressed to rebuild the GUI.
	 */
	@Override
	public void construct() {
		tabs = makeTabContainer();
		tabs.add(inPanel(createAdvancedTerrainShapeTab()));
		addToScreen(tabs);
	}

	private UIContainer<?> inPanel(UIComponent<?> comp) {

		UIColoredPanel panel = new UIColoredPanel(this);
		panel.setSize(UIComponent.INHERITED, UIComponent.INHERITED - 60);
		panel.setPosition(0, 30);
		panel.add(comp);
		return panel;
	}

	private UIContainer<?> makeTabContainer() {

		UIButton prev = new UIButton(this, text("previous_page"))
			.setPosition(32, 8, Anchor.LEFT).setSize(50, 20);
		UIButton next = new UIButton(this, text("next_page"))
			.setPosition(-32, 8, Anchor.RIGHT).setSize(50, 20);

		return new UIPagedTabGroup(this, prev, next);
	}
	private UIContainer<?> createAdvancedTerrainShapeTab() {

		CustomGeneratorSettings settings = CustomGeneratorSettings.defaults();

		final int height = 1;
		final int width1col = 6;
		final int width2col = 3;
		final int width3col = 2;

		String FREQ_FMT = ": %.7f";
		UIGridContainer layout = new UIGridContainer(this);
		layout.setPadding(25, 0);
		layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
			.setColumns(6)
			.setInsets(2, 2, 4, 4)
			// height variation
			.add(
				new UILabel(this, text("height_variation_group")),
				width1col*0, height*0, width1col, height)
			.add(
				makeExponentialSlider(
					text("height_variation_factor_slider", ": %.2f"),
					Float.NaN, Float.NaN, 0, 10, settings.heightVariationFactor),
				width3col*0, height*1, width3col, height)
			.add(
				makeExponentialSlider(
					text("height_variation_special_factor_slider", ": %.2f"),
					Float.NaN, Float.NaN, 0, 10, settings.specialHeightVariationFactorBelowAverageY),
				width3col*1, height*1, width3col, height)
			.add(
				makeExponentialSlider(
					text("height_variation_offset_slider", ": %.2f"),
					0, 10, 0, 10, settings.heightVariationOffset),
				width3col*2, height*1, width3col, height)

			// height
			.add(
				new UILabel(this, text("height_group")),
				width1col*0, height*2, width1col, height)
			.add(
				makeExponentialSlider(
					text("height_factor", ": %.2f"),
					1, 12, 1, 12, settings.heightFactor),
				width2col*0, height*3, width2col, height)
			.add(
				makeExponentialSlider(
					text("height_offset", ": %.2f"),
					1, 12, 1, 12, settings.heightOffset),
				width2col*1, height*3, width2col, height)

			// water
			.add(
				new UILabel(this, text("water_group")),
				width1col*0, height*4, width1col, height)
			.add(
				makeExponentialSlider(
					text("water_level", ": %.2f"),
					1, 12, 1, 12, settings.waterLevel),
				width1col*0, height*5, width1col, height)

			// depth noise
			.add(
				new UILabel(this, text("depth_noise_group")),
				width1col*0, height*6, width1col, height)
			.add(
				makeExponentialSlider(
					text("depth_noise_frequency_x", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.depthNoiseFrequencyX),
				width2col*0, height*7, width2col, height)
			.add(
				makeExponentialSlider(
					text("depth_noise_frequency_z", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.depthNoiseFrequencyZ),
				width2col*1, height*7, width2col, height)

			.add(
				makeIntSlider(
					text("depth_noise_octaves", ": %d"),
					1, 16, settings.depthNoiseOctaves),
				width3col*0, height*8, width3col, height)
			.add(
				makeExponentialSlider(
					text("depth_noise_factor", ": %.4f"),
					Float.NaN, Float.NaN, 1, 12, settings.depthNoiseFactor),
				width3col*1, height*8, width3col, height)
			.add(
				makeExponentialSlider(
					text("depth_noise_offset", ": %.2f"),
					1, 12, 1, 12, settings.depthNoiseOffset),
				width3col*2, height*8, width3col, height)

			// selector noise
			.add(
				new UILabel(this, text("selector_noise_group")),
				width1col*0, height*9, width1col, height)
			.add(
				makeExponentialSlider(
					text("selector_noise_frequency_x", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.selectorNoiseFrequencyX),
				width3col*0, height*10, width3col, height)
			.add(
				makeExponentialSlider(
					text("selector_noise_frequency_y", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.selectorNoiseFrequencyY),
				width3col*1, height*10, width3col, height)
			.add(
				makeExponentialSlider(
					text("selector_noise_frequency_z", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.selectorNoiseFrequencyZ),
				width3col*2, height*10, width3col, height)

			.add(
				makeIntSlider(
					text("selector_noise_octaves", ": %d"),
					1, 16, settings.selectorNoiseOctaves),
				width3col*0, height*11, width3col, height)
			.add(
				makeExponentialSlider(
					text("selector_noise_factor", ": %.4f"),
					Float.NaN, Float.NaN, 0, 10, settings.selectorNoiseFactor),
				width3col*1, height*11, width3col, height)
			.add(
				makeExponentialSlider(
					text("selector_noise_offset", ": %.2f"),
					-5, 5, -5, 5, settings.selectorNoiseOffset),
				width3col*2, height*11, width3col, height)


			// low noise
			.add(
				new UILabel(this, text("low_noise_group")),
				width1col*0, height*12, width1col, height)
			.add(
				makeExponentialSlider(
					text("low_noise_frequency_x", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.lowNoiseFrequencyX),
				width3col*0, height*13, width3col, height)
			.add(
				makeExponentialSlider(
					text("low_noise_frequency_y", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.lowNoiseFrequencyY),
				width3col*1, height*13, width3col, height)
			.add(
				makeExponentialSlider(
					text("low_noise_frequency_z", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.lowNoiseFrequencyZ),
				width3col*2, height*13, width3col, height)

			.add(
				makeIntSlider(
					text("low_noise_octaves", ": %d"),
					1, 16, settings.lowNoiseOctaves),
				width3col*0, height*14, width3col, height)
			.add(
				makeExponentialSlider(
					text("low_noise_factor", ": %.4f"),
					Float.NaN, Float.NaN, 0, 10, settings.lowNoiseFactor),
				width3col*1, height*14, width3col, height)
			.add(
				makeExponentialSlider(
					text("low_noise_offset", ": %.2f"),
					-5, 5, -5, 5, settings.lowNoiseOffset),
				width3col*2, height*14, width3col, height)

			// high noise
			.add(
				new UILabel(this, text("high_noise_group")),
				width1col*0, height*15, width1col, height)
			.add(
				makeExponentialSlider(
					text("high_noise_frequency_x", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.highNoiseFrequencyX),
				width3col*0, height*16, width3col, height)
			.add(
				makeExponentialSlider(
					text("high_noise_frequency_y", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.highNoiseFrequencyY),
				width3col*1, height*16, width3col, height)
			.add(
				makeExponentialSlider(
					text("high_noise_frequency_z", FREQ_FMT),
					Float.NaN, Float.NaN, -8, 1, settings.highNoiseFrequencyZ),
				width3col*2, height*16, width3col, height)

			.add(
				makeIntSlider(
					text("high_noise_octaves", ": %d"),
					1, 16, settings.highNoiseOctaves),
				width3col*0, height*17, width3col, height)
			.add(
				makeExponentialSlider(
					text("high_noise_factor", ": %.4f"),
					Float.NaN, Float.NaN, 0, 10, settings.highNoiseFactor),
				width3col*1, height*17, width3col, height)
			.add(
				makeExponentialSlider(
					text("high_noise_offset", ": %.2f"),
					-5, 5, -5, 5, settings.highNoiseOffset),
				width3col*2, height*17, width3col, height)

			.init();
		return layout;
	}

	private UIComponent<?> makeExponentialSlider(String name, float minNeg, float maxNeg, float minPos, float maxPos, float defaultVal) {
		// a little hack to be able to access slider from within lambda used to construct slider
		UISlider<Float>[] wrappedSlider = new UISlider[1];
		ExponentialConverter.Builder builder = ExponentialConverter
			.builder()
			.setSnapRadiusFunction(d -> 1.0/(wrappedSlider[0] == null ? 1000 : wrappedSlider[0].getWidth())*0.5)
			.setBaseValue(2)
			.addBaseValueWithMultiplier(10, 1)
			.setHasZero(true)
			.setNegativeExponentRange(minNeg, maxNeg)
			.setPositiveExponentRange(minPos, maxPos);
		if (defaultVal != 0) {
			builder.addBaseValueWithMultiplier(2, defaultVal);
		}
		UISlider<Float> slider = new UISliderNoScroll<>(this, this.width - 32,
			builder.build(), name)
			.setValue(defaultVal);
		wrappedSlider[0] = slider;
		return slider;
	}

	private UIComponent<?> makeIntSlider(String name, int min, int max, int defaultValue) {
		// the explicit <Integer> needs to be there because otherwise it won't compile on some systems
		UISlider<Integer> slider = new UISliderNoScroll<Integer>(
			this,
			this.width,
			Converter.from(
				x -> round(x*(max - min) + min),
				x -> (x - min)/((float) max - min))
			, name)
			.setValue(defaultValue)
			.setSize(0, 20);
		return slider;
	}

	private static String text(String name) {
		String unloc = "{cubicchunks.gui.customcubic." + name + "}";
		return unloc;
	}

	private static String text(String name, String fmt) {
		String unloc = "{cubicchunks.gui.customcubic." + name + "}" + fmt;
		return unloc;
	}
}
