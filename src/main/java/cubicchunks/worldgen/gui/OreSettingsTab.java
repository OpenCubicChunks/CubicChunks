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
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeCheckbox;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeFloatSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeRangeSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makePositiveExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.vanillaText;

import com.google.common.eventbus.Subscribe;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.UIBlockStateButton;
import cubicchunks.worldgen.gui.component.UIBlockStateSelect;
import cubicchunks.worldgen.gui.component.UIList;
import cubicchunks.worldgen.gui.component.UIOptionScrollbar;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UISplitLayout;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.control.UIScrollBar;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

class OreSettingsTab {

    private final UIContainer<?> container;

    private final List<UIStandardOreOptions> standardOptions = new ArrayList<>();

    private final List<UIPeriodicGaussianOreOptions> periodicGaussianOptions = new ArrayList<>();

    OreSettingsTab(ExtraGui gui, CustomGeneratorSettings settings) {
        int y = -1;
        UIVerticalTableLayout<?> layout = new UIVerticalTableLayout<>(gui, 1);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS);

        for (CustomGeneratorSettings.StandardOreConfig conf : settings.standardOres) {
            layout.add(new UIStandardOreOptions(gui, conf));
        }

                /*
                .add(label(gui, malisisText("lapis_lazuli_ore_group")),
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
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL));
                */
        layout.setRightPadding(HORIZONTAL_PADDING + 6);
        this.container = layout;
    }

    UIContainer<?> getContainer() {
        return container;
    }

    void writeConfig(CustomGeneratorSettings conf) {
        /*
        conf.lapisLazuliSpawnSize = this.lapisLazuliOreSpawnSize.getValue();
        conf.lapisLazuliSpawnTries = this.lapisLazuliOreSpawnTries.getValue();
        conf.lapisLazuliSpawnProbability = this.lapisLazuliOreSpawnProbability.getValue();
        conf.lapisLazuliSpawnSize = this.lapisLazuliOreSpawnSize.getValue();
        conf.lapisLazuliHeightMean = this.lapisLazuliMeanHeight.getValue();
        conf.lapisLazuliHeightStdDeviation = this.lapisLazuliHeightStdDev.getValue();
        conf.lapisLazuliHeightSpacing = this.lapisLazuliSpacingHeight.getValue();
        conf.lapisLazuliSpawnMinHeight = this.lapisLazuliSpawnRange.getMinValue();
        conf.lapisLazuliSpawnMaxHeight = this.lapisLazuliSpawnRange.getMaxValue();
        */

    }

    private class UIStandardOreOptions extends UIVerticalTableLayout {

        private final UIButton delete;
        private final UILabel name = null;
        private final UIBlockStateButton block;
        private final UISlider<Integer> size;
        private final UISlider<Integer> attempts;
        private final UISlider<Float> probability;
        private final UIRangeSlider<Float> heightRange;

        private final UICheckBox selectBiomes;
        private final UIList<Biome, UICheckBox, ?> biomes;

        private final UISplitLayout<?> split;

        private CustomGeneratorSettings.StandardOreConfig config;

        public UIStandardOreOptions(ExtraGui gui, CustomGeneratorSettings.StandardOreConfig config) {
            super(gui, 6);

            // this.setSize(getWidth(), 30);
            this.autoResizeToContent(true);

            split = new UISplitLayout<>(
                    gui, UISplitLayout.Type.SIDE_BY_SIDE,
                    new UIVerticalTableLayout<>(gui, 6),
                    new UIList<>(gui, ForgeRegistries.BIOMES.getValues(),
                            e -> new UICheckBox(gui, e.getBiomeName() + "(" + e.getRegistryName() + ")"))
            );
            split.setSizeWeights(2, 1);
            split.autoResizeToContent(true);
            split.setUserResizable(false);

            UIContainer<?> label = makeLabel(gui, config);

            this.block = new UIBlockStateButton(gui, config.blockstate);
            this.block.onClick(btn ->
                    new UIBlockStateSelect<>(gui).display(state -> block.setBlockState(state))
            );

            this.add(label,
                    new UIVerticalTableLayout.GridLocation(1, 0, 4));
            this.add(block, new UIVerticalTableLayout.GridLocation(0, 0, 1));
            this.add(delete = new UIButton(gui, malisisText("delete")).setSize(10, 20).setAutoSize(false),
                    new UIVerticalTableLayout.GridLocation(5, 0, 1));
            this.add(split, new GridLocation(0, 1, 6));


            this.config = config;
            int y = -1;

            UIVerticalTableLayout<?> layout = (UIVerticalTableLayout<?>) split.getFirst();

            biomes = (UIList<Biome, UICheckBox, ?>) split.getSecond();

            layout.add(size = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, config.spawnSize),
                    new UIVerticalTableLayout.GridLocation(0, ++y, 3));
            layout.add(attempts = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, config.spawnTries),
                    new UIVerticalTableLayout.GridLocation(3, y, 3));
            layout.add(probability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), config.spawnProbability),
                    new UIVerticalTableLayout.GridLocation(0, ++y, 3));
            layout.add(selectBiomes = makeCheckbox(gui, malisisText("select_biomes"), config.biomes != null),
                    new UIVerticalTableLayout.GridLocation(3, y, 3));
            layout.add(heightRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, config.minHeight, config.maxHeight),
                    new UIVerticalTableLayout.GridLocation(0, ++y, 6));

            layout.autoResizeToContent(true);
            selectBiomes.register(new Object() {
                @Subscribe
                public void onClick(UICheckBox.CheckEvent evt) {
                    allowSelectBiomes(evt.isChecked());
                }
            });
            allowSelectBiomes(selectBiomes.isChecked());

            delete.register(new Object() {
                @Subscribe
                public void onClick(UIButton.ClickEvent evt) {
                    standardOptions.remove(UIStandardOreOptions.this);
                    container.remove(UIStandardOreOptions.this);
                }
            });

            biomes.setHeightFunc(() -> ((UIContainer) split.getFirst()).getContentHeight());
            biomes.setRightPadding(6);
            //size = null;
            //attempts = null;
            //probability = null;
            //heightRange = null;
            //selectBiomes = null;
            //biomes = null;
            //delete = null;

            if (config.biomes != null) {
                config.biomes.forEach(b -> {
                    biomes.getAll().get(b).setChecked(true);
                });
            }

            sortCheckedOnTop(biomes);
        }

        private void sortCheckedOnTop(UIList<Biome, UICheckBox, ?> biomes) {

        }

        private UIContainer<?> makeLabel(ExtraGui gui, CustomGeneratorSettings.StandardOreConfig config) {
            String stateStr = config.blockstate.toString();
            String name;
            String props;
            if (!stateStr.contains("[")) {
                name = stateStr;
                props = "[]";
            } else {
                String[] statesplit = stateStr.split("\\[");
                name = statesplit[0];
                props = "[" + statesplit[1];
            }
            UIVerticalTableLayout<?> label = new UIVerticalTableLayout<>(gui, 1).setInsets(0, 0, 0, 0);

            UIComponent<?> l1 = label(gui, name);
            UIComponent<?> l2 = label(gui, props);
            label.add(l1, l2);
            label.setSize(label.getWidth(), l1.getHeight() + l2.getHeight());
            return label;
        }

        private void allowSelectBiomes(boolean checked) {
            biomes.setVisible(checked);
            if (!biomes.isVisible()) {
                // TODO: is this the expected behavior for users? Or should the selected ones persist?
                biomes.getAll().forEach((e, c) -> c.setChecked(true));
            }
        }

        @Override protected void layout() {
            biomes.setSize(biomes.getRawWidth(), ((UIContainer<?>) split.getFirst()).getContentHeight());
            super.layout();
        }
    }

    private class UIPeriodicGaussianOreOptions {

    }
}
