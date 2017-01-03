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
import com.google.common.eventbus.Subscribe;

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.malisis.core.client.gui.event.component.SpaceChangeEvent;
import net.malisis.core.renderer.font.FontOptions;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UISliderNoScroll;
import cubicchunks.worldgen.gui.converter.Converters;

import static java.lang.Math.round;

public class CustomCubicGuiUtils {
	public static UISlider<Float> makeExponentialSlider(MalisisGui gui, String name, float minNeg, float maxNeg, float minPos, float maxPos, float defaultVal) {

		UISlider<Float>[] wrappedSlider = new UISlider[1];
		DoubleUnaryOperator roundRadiusFunc = d -> 1.0/(wrappedSlider[0] == null ? 1000 : wrappedSlider[0].getWidth())*0.5;

		float defMult = defaultVal == 0 ? 1 : defaultVal;

		Converter<Float, Float> conv = Converters.builder()
			.exponential().withZero().withBaseValue(2).withNegativeExponentRange(minNeg, maxNeg).withPositiveExponentRange(minPos, maxPos)
			.rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128).withRoundingRadius(roundRadiusFunc)
			.build();

		UISlider<Float> slider = new UISliderNoScroll<>(gui, 100, conv, name).setValue(defaultVal);
		wrappedSlider[0] = slider;
		return slider;
	}

	public static UISlider<Float> makeInvertedExponentialSlider(MalisisGui gui, String name, float minNeg, float maxNeg, float minPos, float maxPos, float defaultVal) {

		UISlider<Float>[] wrappedSlider = new UISlider[1];
		DoubleUnaryOperator roundRadiusFunc = d -> 1.0/(wrappedSlider[0] == null ? 1000 : wrappedSlider[0].getWidth())*0.5;

		float defMult = defaultVal == 0 ? 1 : defaultVal;

		Converter<Float, Float> conv = Converters.builder()
			.reverse()
			.pow(2)
			.exponential().withZero().withBaseValue(2).withNegativeExponentRange(minNeg, maxNeg).withPositiveExponentRange(minPos, maxPos)
			.inverse()
			.rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128).withRoundingRadius(roundRadiusFunc)
			.build();

		UISlider<Float> slider = new UISliderNoScroll<>(gui, 100, conv, name).setValue(defaultVal);
		wrappedSlider[0] = slider;
		return slider;
	}

	public static UISlider<Integer> makeIntSlider(MalisisGui gui, String name, int min, int max, int defaultValue) {
		// the explicit <Integer> needs to be there because otherwise it won't compile on some systems
		UISlider<Integer> slider = new UISliderNoScroll<Integer>(
			gui,
			100,
			Converter.from(
				x -> round(x*(max - min) + min),
				x -> (x - min)/((float) max - min))
			, name)
			.setValue(defaultValue)
			.setSize(0, 20);
		return slider;
	}

	public static UISlider<Float> makeFloatSlider(MalisisGui gui, String name, float defaultValue) {
		// the explicit <Integer> needs to be there because otherwise it won't compile on some systems
		UISlider<Float> slider = new UISliderNoScroll<Float>(
			gui,
			100,
			Converter.identity(),
			name)
			.setValue(defaultValue)
			.setSize(0, 20);
		return slider;
	}

	public static UICheckBox makeCheckbox(MalisisGui gui, String name, boolean defaultValue) {
		UICheckBox cb = new UICheckBox(gui, name)
			.setChecked(defaultValue)
			.setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build());
		return cb;
	}

	public static UIRangeSlider<Float> makeRangeSlider(ExtraGui gui, String name, float min, float max, float defaultMin, float defaultMax) {
		UIRangeSlider<Float>[] wrappedSlider = new UIRangeSlider[1];
		DoubleUnaryOperator roundRadiusFunc = d -> 1.0/(wrappedSlider[0] == null ? 1000 : wrappedSlider[0].getWidth())*0.5;
		float maxExp = MathHelper.ceil(Math.log(Math.max(1, max))/Math.log(2));

		Converter<Float, Float> conv = Converters.builder()
			.linearScale(min, max)
			.rounding().withBase(2, 1).withBase(10, 1).withMaxExp(maxExp).withRoundingRadius(roundRadiusFunc)
			.withInfinity().negativeAt(min).positiveAt(max)
			.build();

		UIRangeSlider<Float> slider = new UIRangeSlider<Float>(
			gui, 100,
			conv,
			(a, b) -> I18n.format(name, a*100, b*100))
			.setRange(defaultMin, defaultMax);
		wrappedSlider[0] = slider;
		return slider;
	}

	public static UISelect<BiomeOption> makeBiomeList(MalisisGui gui) {
		List<BiomeOption> biomes = new ArrayList<>();
		biomes.add(BiomeOption.ALL);
		for (Biome biome : ForgeRegistries.BIOMES) {
			if (!biome.isMutation()) {
				biomes.add(new BiomeOption(biome));
			}
		}
		UISelect<BiomeOption> select = new UISelect<>(gui, 0, biomes);
		select.register(new Object() {
			@Subscribe
			public void onResize(SpaceChangeEvent.SizeChangeEvent evt) {
				select.setMaxExpandedWidth(evt.getNewWidth());
			}
		});
		select.select(BiomeOption.ALL);
		select.maxDisplayedOptions(8);
		return select;
	}

	public static UIComponent<?> label(MalisisGui gui, String text, int height) {
		return wrappedCentered(
			gui, new UILabel(gui, text)
				.setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build())
		).setSize(0, height);
	}

	public static UIContainer<?> wrappedCentered(MalisisGui gui, UIComponent<?> comp) {
		comp.setAnchor(Anchor.MIDDLE | Anchor.CENTER);
		UIContainer<?> cont = new UIContainer<>(gui);
		cont.add(comp);
		return cont;
	}

	public static String vanillaText(String name) {
		String unloc = "cubicchunks.gui.customcubic." + name;
		return unloc;
	}

	public static String malisisText(String name) {
		String unloc = "{cubicchunks.gui.customcubic." + name + "}";
		return unloc;
	}

	public static String malisisText(String name, String fmt) {
		String unloc = "{cubicchunks.gui.customcubic." + name + "}" + fmt;
		return unloc;
	}
}
