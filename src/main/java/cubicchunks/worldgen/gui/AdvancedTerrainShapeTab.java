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

import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_INSETS;
import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_PADDING;
import static cubicchunks.worldgen.gui.CustomCubicGui.VERTICAL_INSETS;
import static cubicchunks.worldgen.gui.CustomCubicGui.WIDTH_1_COL;
import static cubicchunks.worldgen.gui.CustomCubicGui.WIDTH_2_COL;
import static cubicchunks.worldgen.gui.CustomCubicGui.WIDTH_3_COL;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.label;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeInvertedExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;

import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.interaction.UISlider;

class AdvancedTerrainShapeTab {

    private final UIVerticalTableLayout container;

    private final UISlider<Float> heightVariationFactor;
    private final UISlider<Float> heightVariationSpecialFactor;
    private final UISlider<Float> heightVariationOffset;
    private final UISlider<Float> heightFactor;
    private final UISlider<Float> heightOffset;

    private final UISlider<Float> depthNoisePeriodX;
    private final UISlider<Float> depthNoisePeriodZ;
    private final UISlider<Integer> depthNoiseOctaves;
    private final UISlider<Float> depthNoiseFactor;
    private final UISlider<Float> depthNoiseOffset;

    private final UISlider<Float> selectorNoisePeriodX;
    private final UISlider<Float> selectorNoisePeriodY;
    private final UISlider<Float> selectorNoisePeriodZ;
    private final UISlider<Integer> selectorNoiseOctaves;
    private final UISlider<Float> selectorNoiseFactor;
    private final UISlider<Float> selectorNoiseOffset;

    private final UISlider<Float> lowNoisePeriodX;
    private final UISlider<Float> lowNoisePeriodY;
    private final UISlider<Float> lowNoisePeriodZ;
    private final UISlider<Integer> lowNoiseOctaves;
    private final UISlider<Float> lowNoiseFactor;
    private final UISlider<Float> lowNoiseOffset;

    private final UISlider<Float> highNoisePeriodX;
    private final UISlider<Float> highNoisePeriodY;
    private final UISlider<Float> highNoisePeriodZ;
    private final UISlider<Integer> highNoiseOctaves;
    private final UISlider<Float> highNoiseFactor;
    private final UISlider<Float> highNoiseOffset;

    AdvancedTerrainShapeTab(ExtraGui gui, CustomGeneratorSettings settings) {

        final float MAX_NOISE_FREQ_POWER = -4;

        String PERIOD_FMT = " %.2f";
        UIVerticalTableLayout layout = new UIVerticalTableLayout(gui, 6);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS)
                // height variation
                .add(label(gui, malisisText("height_variation_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 0, WIDTH_1_COL))
                .add(this.heightVariationFactor = makeExponentialSlider(
                        gui, malisisText("height_variation_factor_slider", ": %.2f"),
                        Float.NaN, Float.NaN, 0, 20, settings.heightVariationFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 1, WIDTH_3_COL))
                .add(this.heightVariationSpecialFactor = makeExponentialSlider(
                        gui, malisisText("height_variation_special_factor_slider", ": %.2f"),
                        Float.NaN, Float.NaN, 0, 20, settings.specialHeightVariationFactorBelowAverageY),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 1, WIDTH_3_COL))
                .add(this.heightVariationOffset = makeExponentialSlider(
                        gui, malisisText("height_variation_offset_slider", ": %.2f"),
                        0, 20, 0, 20, settings.heightVariationOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 1, WIDTH_3_COL))

                // height
                .add(label(gui, malisisText("height_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 2, WIDTH_1_COL))
                .add(this.heightFactor = makeExponentialSlider(
                        gui, malisisText("height_factor", ": %.2f"),
                        1, 20, 1, 20, settings.heightFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 3, WIDTH_2_COL))
                .add(this.heightOffset = makeExponentialSlider(
                        gui, malisisText("height_offset", ": %.2f"),
                        1, 20, 1, 20, settings.heightOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 3, WIDTH_2_COL))

                // depth noise
                .add(label(gui, malisisText("depth_noise_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 6, WIDTH_1_COL))
                .add(this.depthNoisePeriodX = makeInvertedExponentialSlider(
                        gui, malisisText("depth_noise_period_x", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.depthNoiseFrequencyX),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 7, WIDTH_2_COL))
                .add(this.depthNoisePeriodZ = makeInvertedExponentialSlider(
                        gui, malisisText("depth_noise_period_z", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.depthNoiseFrequencyZ),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 7, WIDTH_2_COL))

                .add(this.depthNoiseOctaves = makeIntSlider(
                        gui, malisisText("depth_noise_octaves", ": %d"),
                        1, 16, settings.depthNoiseOctaves),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 8, WIDTH_3_COL))
                .add(this.depthNoiseFactor = makeExponentialSlider(
                        gui, malisisText("depth_noise_factor", ": %.4f"),
                        Float.NaN, Float.NaN, 1, 12, settings.depthNoiseFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 8, WIDTH_3_COL))
                .add(this.depthNoiseOffset = makeExponentialSlider(
                        gui, malisisText("depth_noise_offset", ": %.2f"),
                        1, 12, 1, 12, settings.depthNoiseOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 8, WIDTH_3_COL))

                // selector noise
                .add(label(gui, malisisText("selector_noise_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 9, WIDTH_1_COL))
                .add(this.selectorNoisePeriodX = makeInvertedExponentialSlider(
                        gui, malisisText("selector_noise_period_x", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.selectorNoiseFrequencyX),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 10, WIDTH_3_COL))
                .add(this.selectorNoisePeriodY = makeInvertedExponentialSlider(
                        gui, malisisText("selector_noise_period_y", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.selectorNoiseFrequencyY),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 10, WIDTH_3_COL))
                .add(this.selectorNoisePeriodZ = makeInvertedExponentialSlider(
                        gui, malisisText("selector_noise_period_z", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.selectorNoiseFrequencyZ),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 10, WIDTH_3_COL))

                .add(this.selectorNoiseOctaves = makeIntSlider(
                        gui, malisisText("selector_noise_octaves", ": %d"),
                        1, 16, settings.selectorNoiseOctaves),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 11, WIDTH_3_COL))
                .add(this.selectorNoiseFactor = makeExponentialSlider(
                        gui, malisisText("selector_noise_factor", ": %.4f"),
                        Float.NaN, Float.NaN, 0, 10, settings.selectorNoiseFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 11, WIDTH_3_COL))
                .add(this.selectorNoiseOffset = makeExponentialSlider(
                        gui, malisisText("selector_noise_offset", ": %.2f"),
                        -5, 5, -5, 5, settings.selectorNoiseOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 11, WIDTH_3_COL))


                // low noise
                .add(label(gui, malisisText("low_noise_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 12, WIDTH_1_COL))
                .add(this.lowNoisePeriodX = makeInvertedExponentialSlider(
                        gui, malisisText("low_noise_period_x", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.lowNoiseFrequencyX),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 13, WIDTH_3_COL))
                .add(this.lowNoisePeriodY = makeInvertedExponentialSlider(
                        gui, malisisText("low_noise_period_y", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.lowNoiseFrequencyY),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 13, WIDTH_3_COL))
                .add(this.lowNoisePeriodZ = makeInvertedExponentialSlider(
                        gui, malisisText("low_noise_period_z", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.lowNoiseFrequencyZ),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 13, WIDTH_3_COL))

                .add(this.lowNoiseOctaves = makeIntSlider(
                        gui, malisisText("low_noise_octaves", ": %d"),
                        1, 16, settings.lowNoiseOctaves),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 14, WIDTH_3_COL))
                .add(this.lowNoiseFactor = makeExponentialSlider(
                        gui, malisisText("low_noise_factor", ": %.4f"),
                        Float.NaN, Float.NaN, -10, 10, settings.lowNoiseFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 14, WIDTH_3_COL))
                .add(this.lowNoiseOffset = makeExponentialSlider(
                        gui, malisisText("low_noise_offset", ": %.2f"),
                        -5, 5, -5, 5, settings.lowNoiseOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 14, WIDTH_3_COL))

                // high noise
                .add(label(gui, malisisText("high_noise_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, 15, WIDTH_1_COL))
                .add(this.highNoisePeriodX = makeInvertedExponentialSlider(
                        gui, malisisText("high_noise_period_x", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.highNoiseFrequencyX),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 16, WIDTH_3_COL))
                .add(this.highNoisePeriodY = makeInvertedExponentialSlider(
                        gui, malisisText("high_noise_period_y", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.highNoiseFrequencyY),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 16, WIDTH_3_COL))
                .add(this.highNoisePeriodZ = makeInvertedExponentialSlider(
                        gui, malisisText("high_noise_period_z", PERIOD_FMT),
                        Float.NaN, Float.NaN, -8, MAX_NOISE_FREQ_POWER, 1.0f / settings.highNoiseFrequencyZ),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 16, WIDTH_3_COL))

                .add(this.highNoiseOctaves = makeIntSlider(
                        gui, malisisText("high_noise_octaves", ": %d"),
                        1, 16, settings.highNoiseOctaves),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, 17, WIDTH_3_COL))
                .add(this.highNoiseFactor = makeExponentialSlider(
                        gui, malisisText("high_noise_factor", ": %.4f"),
                        Float.NaN, Float.NaN, -10, 10, settings.highNoiseFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, 17, WIDTH_3_COL))
                .add(this.highNoiseOffset = makeExponentialSlider(
                        gui, malisisText("high_noise_offset", ": %.2f"),
                        -5, 5, -5, 5, settings.highNoiseOffset),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, 17, WIDTH_3_COL))

                .init();
        this.container = layout;
    }

    UIVerticalTableLayout getContainer() {
        return container;
    }

    void writeConfig(CustomGeneratorSettings conf) {
        conf.heightVariationFactor = this.heightVariationFactor.getValue();
        conf.specialHeightVariationFactorBelowAverageY = this.heightVariationSpecialFactor.getValue();
        conf.heightVariationOffset = this.heightVariationOffset.getValue();
        conf.heightFactor = this.heightFactor.getValue();
        conf.heightOffset = this.heightOffset.getValue();

        conf.depthNoiseFrequencyX = 1.0f / this.depthNoisePeriodX.getValue();
        conf.depthNoiseFrequencyZ = 1.0f / this.depthNoisePeriodZ.getValue();
        conf.depthNoiseOctaves = this.depthNoiseOctaves.getValue();
        conf.depthNoiseFactor = this.depthNoiseFactor.getValue();
        conf.depthNoiseOffset = this.depthNoiseOffset.getValue();

        conf.selectorNoiseFrequencyX = 1.0f / this.selectorNoisePeriodX.getValue();
        conf.selectorNoiseFrequencyY = 1.0f / this.selectorNoisePeriodY.getValue();
        conf.selectorNoiseFrequencyZ = 1.0f / this.selectorNoisePeriodZ.getValue();
        conf.selectorNoiseOctaves = this.selectorNoiseOctaves.getValue();
        conf.selectorNoiseFactor = this.selectorNoiseFactor.getValue();
        conf.selectorNoiseOffset = this.selectorNoiseOffset.getValue();

        conf.lowNoiseFrequencyX = 1.0f / this.lowNoisePeriodX.getValue();
        conf.lowNoiseFrequencyY = 1.0f / this.lowNoisePeriodY.getValue();
        conf.lowNoiseFrequencyZ = 1.0f / this.lowNoisePeriodZ.getValue();
        conf.lowNoiseOctaves = this.lowNoiseOctaves.getValue();
        conf.lowNoiseFactor = this.lowNoiseFactor.getValue();
        conf.lowNoiseOffset = this.lowNoiseOffset.getValue();

        conf.highNoiseFrequencyX = 1.0f / this.highNoisePeriodX.getValue();
        conf.highNoiseFrequencyY = 1.0f / this.highNoisePeriodY.getValue();
        conf.highNoiseFrequencyZ = 1.0f / this.highNoisePeriodZ.getValue();
        conf.highNoiseOctaves = this.highNoiseOctaves.getValue();
        conf.highNoiseFactor = this.highNoiseFactor.getValue();
        conf.highNoiseOffset = this.highNoiseOffset.getValue();
    }
}
