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

import static java.lang.Math.round;

import com.google.common.base.Converter;
import com.google.common.eventbus.Subscribe;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UISliderImproved;
import cubicchunks.worldgen.gui.converter.Converters;
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
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;

import javax.annotation.Nonnull;

public class CustomCubicGuiUtils {

    public static UISlider<Float> makeFloatSlider(MalisisGui gui, String name, float min, float max, float defaultVal) {

        UISlider<Float>[] wrappedSlider = new UISlider[1];
        BiPredicate<Double, Double> isInRoundRadius = getIsInRoundRadiusPredicate(wrappedSlider);

        float defMult = defaultVal == 0 ? 1 : defaultVal;

        Converter<Float, Float> conv = Converters.builder()
                .linearScale(min, max).rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128)
                .withRoundingRadiusPredicate(isInRoundRadius)
                .build();

        UISlider<Float> slider = new UISliderImproved<>(gui, 100, conv, name).setValue(defaultVal);
        wrappedSlider[0] = slider;
        return slider;
    }

    public static UISlider<Float> makePositiveExponentialSlider(MalisisGui gui, String name, float minPos, float maxPos,
                                                        float defaultVal) {

        UISlider<Float>[] wrappedSlider = new UISlider[1];
        BiPredicate<Double, Double> isInRoundRadius = getIsInRoundRadiusPredicate(wrappedSlider);

        float defMult = defaultVal == 0 ? 1 : defaultVal;

        Converter<Float, Float> conv = Converters.builder()
                .exponential().withBaseValue(2).withPositiveExponentRange(minPos, maxPos)
                .rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128)
                .withRoundingRadiusPredicate(isInRoundRadius)
                .withInfinity().positiveAt((float)Math.pow(2, maxPos)).negativeAt(Float.NaN)
                .build();

        UISlider<Float> slider = new UISliderImproved<>(gui, 100, conv, name).setValue(defaultVal);
        wrappedSlider[0] = slider;
        return slider;
    }

    public static UISlider<Float> makeExponentialSlider(MalisisGui gui, String name, float minNeg, float maxNeg, float minPos, float maxPos,
            float defaultVal) {

        UISlider<Float>[] wrappedSlider = new UISlider[1];
        BiPredicate<Double, Double> isInRoundRadius = getIsInRoundRadiusPredicate(wrappedSlider);

        float defMult = defaultVal == 0 ? 1 : defaultVal;

        Converter<Float, Float> conv = Converters.builder()
                .exponential().withZero().withBaseValue(2).withNegativeExponentRange(minNeg, maxNeg).withPositiveExponentRange(minPos, maxPos)
                .rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128)
                .withRoundingRadiusPredicate(isInRoundRadius)
                .build();

        UISlider<Float> slider = new UISliderImproved<>(gui, 100, conv, name).setValue(defaultVal);
        wrappedSlider[0] = slider;
        return slider;
    }

    @Nonnull private static BiPredicate<Double, Double> getIsInRoundRadiusPredicate(UISlider<Float>[] floatUISlider) {
        return getIsInRoundRadiusPredicate(() -> floatUISlider[0] == null ? 1000 : floatUISlider[0].getWidth());
    }

    @Nonnull private static BiPredicate<Double, Double> getIsInRoundRadiusPredicate(UIRangeSlider<Float>[] floatUISlider) {
        return getIsInRoundRadiusPredicate(() -> floatUISlider[0] == null ? 1000 : floatUISlider[0].getWidth());
    }

    @Nonnull private static BiPredicate<Double, Double> getIsInRoundRadiusPredicate(DoubleSupplier width) {
        return (previousSlide, foundSlide) -> {
            double w = width.getAsDouble();
            double rangeCenter = Math.round(previousSlide * w) / w;
            double minRange = rangeCenter - 0.5 / w;
            double maxRange = rangeCenter + 0.5 / w;

            return foundSlide >= minRange && foundSlide <= maxRange;
        };
    }

    public static UISlider<Float> makeInvertedExponentialSlider(MalisisGui gui, String name, float minNeg, float maxNeg, float minPos, float maxPos,
            float defaultVal) {

        UISlider<Float>[] wrappedSlider = new UISlider[1];
        BiPredicate<Double, Double> isInRoundRadius = getIsInRoundRadiusPredicate(wrappedSlider);

        float defMult = defaultVal == 0 ? 1 : defaultVal;

        Converter<Float, Float> conv = Converters.builder()
                .reverse()
                .pow(2)
                .exponential().withZero().withBaseValue(2).withNegativeExponentRange(minNeg, maxNeg).withPositiveExponentRange(minPos, maxPos)
                .inverse()
                .rounding().withBase(2, 1).withBase(10, 1).withBase(2, defMult).withBase(10, defMult).withMaxExp(128)
                .withRoundingRadiusPredicate(isInRoundRadius)
                .build();

        UISlider<Float> slider = new UISliderImproved<>(gui, 100, conv, name).setValue(defaultVal);
        wrappedSlider[0] = slider;
        return slider;
    }

    public static UISlider<Integer> makeIntSlider(MalisisGui gui, String name, int min, int max, int defaultValue) {
        // the explicit <Integer> needs to be there because otherwise it won't compile on some systems
        UISlider<Integer> slider = new UISliderImproved<Integer>(
                gui,
                100,
                Converter.from(
                        x -> round(x * (max - min) + min),
                        x -> (x - min) / ((float) max - min))
                , name)
                .setValue(defaultValue)
                .setSize(0, 20);
        return slider;
    }

    public static UISlider<Float> makeFloatSlider(MalisisGui gui, String name, float defaultValue) {
        // the explicit <Float> needs to be there because otherwise it won't compile on some systems
        UISlider<Float> slider = new UISliderImproved<Float>(
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
        BiPredicate<Double, Double> isInRoundRadius = getIsInRoundRadiusPredicate(wrappedSlider);
        float maxExp = MathHelper.ceil(Math.log(Math.max(1, max)) / Math.log(2));

        Converter<Float, Float> conv = Converters.builder()
                .linearScale(min, max)
                .rounding().withBase(2, 1).withBase(10, 1).withMaxExp(maxExp).withRoundingRadiusPredicate(isInRoundRadius)
                .withInfinity().negativeAt(min).positiveAt(max)
                .build();

        UIRangeSlider<Float> slider = new UIRangeSlider<Float>(
                gui, 100,
                conv,
                (a, b) -> I18n.format(name, a * 100, b * 100))
                .setRange(defaultMin, defaultMax);
        wrappedSlider[0] = slider;
        return slider;
    }

    public static <T> UISelect<T> makeUISelect(MalisisGui gui, Iterable<T> values) {
        UISelect<T> select = new UISelect<T>(gui, 0, values);
        return select;
    }

    public static UISelect<BiomeOption> makeBiomeList(MalisisGui gui) {
        List<BiomeOption> biomes = new ArrayList<>();
        biomes.add(BiomeOption.ALL);
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (!biome.isMutation()) {
                biomes.add(new BiomeOption(biome));
            }
        }
        UISelect<BiomeOption> select = makeUISelect(gui, biomes);

        select.select(BiomeOption.ALL);
        select.maxDisplayedOptions(8);
        return select;
    }

    public static UIComponent<?> label(MalisisGui gui, String text) {
        return wrappedCentered(
                gui, new UILabel(gui, text)
                        .setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build())
        ).setSize(0, 15);
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
