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
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeFloatSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeRangeSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makePositiveExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.vanillaText;

import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.interaction.UISlider;

class OreSettingsTab {

    private final UIVerticalTableLayout container;

    private final UISlider<Integer> dirtSpawnSize;
    private final UISlider<Integer> dirtSpawnTries;
    private final UISlider<Float> dirtSpawnProbability;
    private final UIRangeSlider<Float> dirtSpawnRange;

    private final UISlider<Integer> gravelSpawnSize;
    private final UISlider<Integer> gravelSpawnTries;
    private final UISlider<Float> gravelSpawnProbability;
    private final UIRangeSlider<Float> gravelSpawnRange;

    private final UISlider<Integer> graniteSpawnSize;
    private final UISlider<Integer> graniteSpawnTries;
    private final UISlider<Float> graniteSpawnProbability;
    private final UIRangeSlider<Float> graniteSpawnRange;

    private final UISlider<Integer> dioriteSpawnSize;
    private final UISlider<Integer> dioriteSpawnTries;
    private final UISlider<Float> dioriteSpawnProbability;
    private final UIRangeSlider<Float> dioriteSpawnRange;

    private final UISlider<Integer> andesiteSpawnSize;
    private final UISlider<Integer> andesiteSpawnTries;
    private final UISlider<Float> andesiteSpawnProbability;
    private final UIRangeSlider<Float> andesiteSpawnRange;

    private final UISlider<Integer> coalOreSpawnSize;
    private final UISlider<Integer> coalOreSpawnTries;
    private final UISlider<Float> coalOreSpawnProbability;
    private final UIRangeSlider<Float> coalOreSpawnRange;

    private final UISlider<Integer> ironOreSpawnSize;
    private final UISlider<Integer> ironOreSpawnTries;
    private final UISlider<Float> ironOreSpawnProbability;
    private final UIRangeSlider<Float> ironOreSpawnRange;

    private final UISlider<Integer> goldOreSpawnSize;
    private final UISlider<Integer> goldOreSpawnTries;
    private final UISlider<Float> goldOreSpawnProbability;
    private final UIRangeSlider<Float> goldOreSpawnRange;

    private final UISlider<Integer> redstoneOreSpawnSize;
    private final UISlider<Integer> redstoneOreSpawnTries;
    private final UISlider<Float> redstoneOreSpawnProbability;
    private final UIRangeSlider<Float> redstoneOreSpawnRange;

    private final UISlider<Integer> diamondOreSpawnSize;
    private final UISlider<Integer> diamondOreSpawnTries;
    private final UISlider<Float> diamondOreSpawnProbability;
    private final UIRangeSlider<Float> diamondOreSpawnRange;

    private final UISlider<Integer> lapisLazuliOreSpawnSize;
    private final UISlider<Integer> lapisLazuliOreSpawnTries;
    private final UISlider<Float> lapisLazuliOreSpawnProbability;
    private final UISlider<Float> lapisLazuliMeanHeight;
    private final UISlider<Float> lapisLazuliHeightStdDev;
    private final UISlider<Float> lapisLazuliSpacingHeight;
    private final UIRangeSlider<Float> lapisLazuliSpawnRange;

    OreSettingsTab(ExtraGui gui, CustomGeneratorSettings settings) {
        int y = -1;
        UIVerticalTableLayout layout = new UIVerticalTableLayout(gui, 6);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS)

                .add(label(gui, malisisText("dirt_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.dirtSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.dirtSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.dirtSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.dirtSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.dirtSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.dirtSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.dirtSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.dirtSpawnMinHeight, settings
                                        .dirtSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("gravel_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.gravelSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.gravelSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.gravelSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.gravelSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.gravelSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.gravelSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.gravelSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.gravelSpawnMinHeight, settings
                                        .gravelSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("granite_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.graniteSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.graniteSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.graniteSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.graniteSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.graniteSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.graniteSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.graniteSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.graniteSpawnMinHeight, settings
                                        .graniteSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("diorite_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.dioriteSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.dioriteSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.dioriteSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.dioriteSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.dioriteSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.dioriteSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.dioriteSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.dioriteSpawnMinHeight, settings
                                        .dioriteSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("andesite_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.andesiteSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.andesiteSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.andesiteSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.andesiteSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.andesiteSpawnProbability =
                                makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.andesiteSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.andesiteSpawnRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.andesiteSpawnMinHeight,
                        settings.andesiteSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("coal_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.coalOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.coalOreSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.coalOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.coalOreSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.coalOreSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.coalOreSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.coalOreSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.coalOreSpawnMinHeight, settings
                                        .coalOreSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("iron_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.ironOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.ironOreSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.ironOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.ironOreSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.ironOreSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.ironOreSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.ironOreSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.ironOreSpawnMinHeight, settings
                                        .ironOreSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("gold_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.goldOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.goldOreSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.goldOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.goldOreSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.goldOreSpawnProbability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.goldOreSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.goldOreSpawnRange =
                                makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.goldOreSpawnMinHeight, settings
                                        .goldOreSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("redstone_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.redstoneOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.redstoneOreSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.redstoneOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.redstoneOreSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.redstoneOreSpawnProbability =
                                makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.redstoneOreSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.redstoneOreSpawnRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.redstoneOreSpawnMinHeight,
                        settings.redstoneOreSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("diamond_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.diamondOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.diamondOreSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.diamondOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.diamondOreSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.diamondOreSpawnProbability =
                                makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), settings.diamondOreSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.diamondOreSpawnRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, settings.diamondOreSpawnMinHeight,
                        settings.diamondOreSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(label(gui, malisisText("lapis_lazuli_ore_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.lapisLazuliOreSpawnSize = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, settings.lapisLazuliSpawnSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 0, ++y, WIDTH_3_COL))
                .add(this.lapisLazuliOreSpawnTries = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, settings.lapisLazuliSpawnTries),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 1, y, WIDTH_3_COL))
                .add(this.lapisLazuliOreSpawnProbability =
                                makeFloatSlider(gui, malisisText("spawn_maxprobability", " %.3f"), settings.lapisLazuliSpawnProbability),
                        new UIVerticalTableLayout.GridLocation(WIDTH_3_COL * 2, y, WIDTH_3_COL))
                .add(this.lapisLazuliSpacingHeight = makePositiveExponentialSlider(gui, malisisText("spacing_height", " %.3f"), -1f, 6.0f,
                        settings.lapisLazuliHeightSpacing),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.lapisLazuliSpawnRange = makeRangeSlider(gui, vanillaText("spawn_range"), -4.0f, 4.0f, settings.lapisLazuliSpawnMinHeight,
                        settings.lapisLazuliSpawnMaxHeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .add(this.lapisLazuliMeanHeight = makeFloatSlider(gui, malisisText("mean_height", " %.3f"), -4.0f, 4.0f,
                        settings.lapisLazuliHeightMean),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.lapisLazuliHeightStdDev = makeFloatSlider(gui, malisisText("height_std_dev", " %.3f"), 0f, 1f, settings.lapisLazuliHeightStdDeviation),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))
                .init();

        this.container = layout;
    }

    UIVerticalTableLayout getContainer() {
        return container;
    }

    void writeConfig(CustomGeneratorSettings conf) {
        conf.dirtSpawnSize = this.dirtSpawnSize.getValue();
        conf.dirtSpawnTries = this.dirtSpawnTries.getValue();
        conf.dirtSpawnProbability = this.dirtSpawnProbability.getValue();
        conf.dirtSpawnSize = this.dirtSpawnSize.getValue();
        conf.dirtSpawnMinHeight = this.dirtSpawnRange.getMinValue();
        conf.dirtSpawnMaxHeight = this.dirtSpawnRange.getMaxValue();

        conf.gravelSpawnSize = this.gravelSpawnSize.getValue();
        conf.gravelSpawnTries = this.gravelSpawnTries.getValue();
        conf.gravelSpawnProbability = this.gravelSpawnProbability.getValue();
        conf.gravelSpawnSize = this.gravelSpawnSize.getValue();
        conf.gravelSpawnMinHeight = this.gravelSpawnRange.getMinValue();
        conf.gravelSpawnMaxHeight = this.gravelSpawnRange.getMaxValue();

        conf.graniteSpawnSize = this.graniteSpawnSize.getValue();
        conf.graniteSpawnTries = this.graniteSpawnTries.getValue();
        conf.graniteSpawnProbability = this.graniteSpawnProbability.getValue();
        conf.graniteSpawnSize = this.graniteSpawnSize.getValue();
        conf.graniteSpawnMinHeight = this.graniteSpawnRange.getMinValue();
        conf.graniteSpawnMaxHeight = this.graniteSpawnRange.getMaxValue();

        conf.dioriteSpawnSize = this.dioriteSpawnSize.getValue();
        conf.dioriteSpawnTries = this.dioriteSpawnTries.getValue();
        conf.dioriteSpawnProbability = this.dioriteSpawnProbability.getValue();
        conf.dioriteSpawnSize = this.dioriteSpawnSize.getValue();
        conf.dioriteSpawnMinHeight = this.dioriteSpawnRange.getMinValue();
        conf.dioriteSpawnMaxHeight = this.dioriteSpawnRange.getMaxValue();

        conf.andesiteSpawnSize = this.andesiteSpawnSize.getValue();
        conf.andesiteSpawnTries = this.andesiteSpawnTries.getValue();
        conf.andesiteSpawnProbability = this.andesiteSpawnProbability.getValue();
        conf.andesiteSpawnSize = this.andesiteSpawnSize.getValue();
        conf.andesiteSpawnMinHeight = this.andesiteSpawnRange.getMinValue();
        conf.andesiteSpawnMaxHeight = this.andesiteSpawnRange.getMaxValue();

        conf.coalOreSpawnSize = this.coalOreSpawnSize.getValue();
        conf.coalOreSpawnTries = this.coalOreSpawnTries.getValue();
        conf.coalOreSpawnProbability = this.coalOreSpawnProbability.getValue();
        conf.coalOreSpawnSize = this.coalOreSpawnSize.getValue();
        conf.coalOreSpawnMinHeight = this.coalOreSpawnRange.getMinValue();
        conf.coalOreSpawnMaxHeight = this.coalOreSpawnRange.getMaxValue();

        conf.ironOreSpawnSize = this.ironOreSpawnSize.getValue();
        conf.ironOreSpawnTries = this.ironOreSpawnTries.getValue();
        conf.ironOreSpawnProbability = this.ironOreSpawnProbability.getValue();
        conf.ironOreSpawnSize = this.ironOreSpawnSize.getValue();
        conf.ironOreSpawnMinHeight = this.ironOreSpawnRange.getMinValue();
        conf.ironOreSpawnMaxHeight = this.ironOreSpawnRange.getMaxValue();

        conf.goldOreSpawnSize = this.goldOreSpawnSize.getValue();
        conf.goldOreSpawnTries = this.goldOreSpawnTries.getValue();
        conf.goldOreSpawnProbability = this.goldOreSpawnProbability.getValue();
        conf.goldOreSpawnSize = this.goldOreSpawnSize.getValue();
        conf.goldOreSpawnMinHeight = this.goldOreSpawnRange.getMinValue();
        conf.goldOreSpawnMaxHeight = this.goldOreSpawnRange.getMaxValue();

        conf.redstoneOreSpawnSize = this.redstoneOreSpawnSize.getValue();
        conf.redstoneOreSpawnTries = this.redstoneOreSpawnTries.getValue();
        conf.redstoneOreSpawnProbability = this.redstoneOreSpawnProbability.getValue();
        conf.redstoneOreSpawnSize = this.redstoneOreSpawnSize.getValue();
        conf.redstoneOreSpawnMinHeight = this.redstoneOreSpawnRange.getMinValue();
        conf.redstoneOreSpawnMaxHeight = this.redstoneOreSpawnRange.getMaxValue();

        conf.diamondOreSpawnSize = this.diamondOreSpawnSize.getValue();
        conf.diamondOreSpawnTries = this.diamondOreSpawnTries.getValue();
        conf.diamondOreSpawnProbability = this.diamondOreSpawnProbability.getValue();
        conf.diamondOreSpawnSize = this.diamondOreSpawnSize.getValue();
        conf.diamondOreSpawnMinHeight = this.diamondOreSpawnRange.getMinValue();
        conf.diamondOreSpawnMaxHeight = this.diamondOreSpawnRange.getMaxValue();

        conf.lapisLazuliSpawnSize = this.lapisLazuliOreSpawnSize.getValue();
        conf.lapisLazuliSpawnTries = this.lapisLazuliOreSpawnTries.getValue();
        conf.lapisLazuliSpawnProbability = this.lapisLazuliOreSpawnProbability.getValue();
        conf.lapisLazuliSpawnSize = this.lapisLazuliOreSpawnSize.getValue();
        conf.lapisLazuliHeightMean = this.lapisLazuliMeanHeight.getValue();
        conf.lapisLazuliHeightStdDeviation = this.lapisLazuliHeightStdDev.getValue();
        conf.lapisLazuliHeightSpacing = this.lapisLazuliSpacingHeight.getValue();
        conf.lapisLazuliSpawnMinHeight = this.lapisLazuliSpawnRange.getMinValue();
        conf.lapisLazuliSpawnMaxHeight = this.lapisLazuliSpawnRange.getMaxValue();

    }
}
