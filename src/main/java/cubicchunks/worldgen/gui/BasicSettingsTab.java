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
import static cubicchunks.worldgen.gui.CustomCubicGui.WIDTH_2_COL;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeBiomeList;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeCheckbox;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;

import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.malisis.core.renderer.font.FontOptions;
import net.minecraft.world.biome.Biome;

class BasicSettingsTab {

    private final UIVerticalTableLayout container;

    private final UICheckBox caves;
    private final UICheckBox strongholds;
    private final UICheckBox villages;
    private final UICheckBox mineshafts;
    private final UICheckBox temples;
    private final UICheckBox ravines;
    private final UICheckBox oceanMonuments;
    private final UICheckBox woodlandMansions;
    private final UICheckBox dungeons;
    private final UICheckBox waterLakes;
    private final UICheckBox lavaLakes;
    private final UICheckBox lavaOceans;

    private final UISelect<BiomeOption> biome;

    private final UISlider<Integer> dungeonCount;

    private final UISlider<Integer> waterLakeRarity;
    private final UISlider<Integer> lavaLakeRarity;

    private final UISlider<Integer> biomeSize;
    private final UISlider<Integer> riverSize;

    private final UISlider<Float> waterLevel;

    BasicSettingsTab(ExtraGui gui, CustomGeneratorSettings settings) {

        FontOptions warnOptions = FontOptions.builder().color(0xAA0000).build();
        UIVerticalTableLayout layout = new UIVerticalTableLayout(gui, 6);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS)

                .add(this.caves = makeCheckbox(gui, malisisText("caves"), settings.caves),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 0, WIDTH_2_COL))
                .add(this.strongholds = makeCheckbox(gui, malisisText("strongholds"), settings.strongholds)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 0, WIDTH_2_COL))

                .add(this.villages = makeCheckbox(gui, malisisText("villages"), settings.villages)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 1, WIDTH_2_COL))
                .add(this.mineshafts = makeCheckbox(gui, malisisText("mineshafts"), settings.mineshafts)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 1, WIDTH_2_COL))

                .add(this.temples = makeCheckbox(gui, malisisText("temples"), settings.temples)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 2, WIDTH_2_COL))
                .add(this.ravines = makeCheckbox(gui, malisisText("ravines"), settings.ravines),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 2, WIDTH_2_COL))

                .add(this.oceanMonuments = makeCheckbox(gui, malisisText("ocean_monuments"), settings.oceanMonuments)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 3, WIDTH_2_COL))
                .add(this.woodlandMansions = makeCheckbox(gui, malisisText("woodland_mansions"), settings.woodlandMansions)
                                .setTooltip(malisisText("tooltip.not_yet_implemented")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 3, WIDTH_2_COL))


                .add(this.dungeons = makeCheckbox(gui, malisisText("dungeons"), settings.dungeons),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 4, WIDTH_2_COL))
                .add(this.waterLakes = makeCheckbox(gui, malisisText("water_lakes"), settings.waterLakes),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 4, WIDTH_2_COL))

                .add(this.lavaLakes = makeCheckbox(gui, malisisText("lava_lakes"), settings.lavaLakes),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 5, WIDTH_2_COL))
                .add(this.lavaOceans = makeCheckbox(gui, malisisText("lava_oceans"), false)
                                .setTooltip(malisisText("tooltip.replacement_wip")).setFontOptions(warnOptions),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 5, WIDTH_2_COL))


                .add(this.biome = makeBiomeList(gui), new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 6, WIDTH_2_COL))
                .add(this.dungeonCount = makeIntSlider(gui, malisisText("dungeon_count", ": %d"), 1, 100, settings.dungeonCount),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 6, WIDTH_2_COL))

                .add(this.waterLakeRarity = makeIntSlider(gui, malisisText("water_lake_rarity", ": %d"), 1, 100, settings.waterLakeRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 7, WIDTH_2_COL))
                .add(this.lavaLakeRarity = makeIntSlider(gui, malisisText("lava_lake_rarity", ": %d"), 1, 100, settings.lavaLakeRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 7, WIDTH_2_COL))

                .add(this.biomeSize = makeIntSlider(gui, malisisText("biome_size", ": %d"), 1, 8, settings.biomeSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 8, WIDTH_2_COL))
                .add(this.riverSize = makeIntSlider(gui, malisisText("river_size", ": %d"), 1, 5, settings.riverSize),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, 8, WIDTH_2_COL))


                .add(this.waterLevel = makeExponentialSlider(
                        gui, malisisText("water_level", ": %.2f"),
                        1, 12, 1, 12, settings.waterLevel),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, 9, WIDTH_2_COL))


                .init();

        this.container = layout;
    }

    UIVerticalTableLayout getContainer() {
        return container;
    }

    void writeConfig(CustomGeneratorSettings conf) {
        conf.caves = caves.isChecked();
        conf.strongholds = strongholds.isChecked();
        conf.villages = villages.isChecked();
        conf.mineshafts = mineshafts.isChecked();
        conf.temples = temples.isChecked();
        conf.ravines = ravines.isChecked();
        conf.oceanMonuments = oceanMonuments.isChecked();
        conf.woodlandMansions = woodlandMansions.isChecked();
        conf.dungeons = dungeons.isChecked();
        conf.waterLakes = waterLakes.isChecked();
        conf.lavaLakes = lavaLakes.isChecked();

        conf.biome = biome.getSelectedValue().getBiome() == null ? -1 : Biome.getIdForBiome(biome.getSelectedValue().getBiome());

        conf.dungeonCount = dungeonCount.getValue();

        conf.waterLakeRarity = waterLakeRarity.getValue();
        conf.lavaLakeRarity = lavaLakeRarity.getValue();

        conf.biomeSize = biomeSize.getValue();
        conf.riverSize = riverSize.getValue();

        conf.waterLevel = Math.round(waterLevel.getValue());
    }
}
