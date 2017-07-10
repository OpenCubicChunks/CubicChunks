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
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.floatFormat;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.label;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeFloatSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeRangeSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.percentageFormat;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings.RavineLavaMode;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UISelectLayoutSpecial;
import cubicchunks.worldgen.gui.component.UITabbedContainer;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.malisis.core.renderer.font.FontOptions;

import java.lang.reflect.Field;
import java.util.function.Consumer;

class StructuresAdvancedTab {

    // caves

    // basic advanced cave config
    private final UISlider<Integer> caveRarityPerChunk;
    private final UISlider<Integer> caveMaxInitialNodes;
    private final UISlider<Integer> caveLargeNodeRarity;
    private final UISlider<Integer> caveLargeNodeMaxBranches;
    private final UISlider<Integer> bigCaveRarity;
    private final UISlider<Float> caveSizeFactor1;
    private final UISlider<Float> caveSizeFactor2;
    private final UIRangeSlider<Float> bigCaveSizeFactorRange;
    private final UISlider<Float> caveSizeAdd;

    // more advanced cave config
    private final UISlider<Integer> caveAlternateFlattenFactorRarity;
    private final UISlider<Float> caveFlattenFactor;
    private final UISlider<Float> caveAltFlattenFactor;
    private final UISlider<Float> caveDirectionChangeFactor;
    private final UISlider<Float> cavePrevHorizAccelerationWeight;
    private final UISlider<Float> cavePrevVertAccelerationWeight;
    private final UISlider<Float> caveMaxHorizAccelChange;
    private final UISlider<Float> caveMaxVertAccelChange;
    private final UISlider<Integer> caveCarveStepRarity;
    private final UISlider<Float> caveFloorDepth;

    // ravines

    private final UISlider<Integer> ravineRarityPerChunk;
    private final UIRangeSlider<Float> ravineYRange;
    private final UISlider<Float> ravineSizeFactor1;
    private final UISlider<Float> ravineSizeFactor2;
    private final UISelect<RavineLavaMode> ravineLavaGenSelect;
    //private final UIComponent<?> ravineGenLavaSection;
    private final UIRangeSlider<Float> ravineYAngleRange;
    private final UISlider<Float> ravineSizeAdd;
    private final UISlider<Float> ravineFlattenFactor;
    private final UISlider<Float> ravineDirectionChangeFactor;
    private final UIRangeSlider<Float> ravineRandomSizeFactorRange;
    private final UISlider<Float> ravinePrevHorizAccelerationWeight;
    private final UISlider<Float> ravinePrevVertAccelerationWeight;
    private final UISlider<Float> ravineMaxHorizAccelChange;
    private final UISlider<Float> ravineMaxVertAccelChange;
    private final UISlider<Integer> ravineCarveStepRarity;
    private final UISlider<Float> ravineStretchYFactor;

    private final UIVerticalTableLayout container;

    StructuresAdvancedTab(ExtraGui gui, CustomGeneratorSettings settings) {
        int y = -1;

        UIVerticalTableLayout layout = new UIVerticalTableLayout(gui, 6);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS)

                // CAVES

                .add(label(gui, malisisText("cave.settings_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.caveRarityPerChunk = makeIntSlider(gui, malisisText("cave.rarity", " %d"), 8, 8192, settings.caveRarityPerChunk),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveMaxInitialNodes = makeIntSlider(gui, malisisText("cave.max_init_nodes", " %d"), 1, 20, settings.caveMaxInitialNodes),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.caveLargeNodeRarity = makeIntSlider(gui, malisisText("cave.large_node_rarity", " %d"), 1, 50, settings.caveLargeNodeRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveLargeNodeMaxBranches =
                                makeIntSlider(gui, malisisText("cave.large_node_max_branches", " %d"), 1, 15, settings.caveLargeNodeMaxBranches),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.bigCaveRarity = makeIntSlider(gui, malisisText("cave.big_branch_rarity", " %d"), 1, 50, settings.bigCaveRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveSizeAdd = makeFloatSlider(gui, malisisText("cave.branch_radius_add", " %.2f"), 1, 10, settings.caveSizeAdd),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.caveSizeFactor1 = makeFloatSlider(gui, malisisText("cave.branch_radius_factor_1", " %.2f"), 1, 10, settings.caveSizeFactor1),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveSizeFactor2 = makeFloatSlider(gui, malisisText("cave.branch_radius_factor_2", " %.2f"), 1, 10, settings.caveSizeFactor2),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.bigCaveSizeFactorRange = makeRangeSlider(gui, floatFormat("cave.big_branch_radius_factor_range", "%.3f"), 0, 16,
                        settings.minBigCaveSizeFactor, settings.maxBigCaveSizeFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.caveAlternateFlattenFactorRarity =
                                makeIntSlider(gui, malisisText("cave.alt_flatten_rarity", " %d"), 1, 100, settings.caveAltFlattenFactorRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveDirectionChangeFactor =
                                makeFloatSlider(gui, malisisText("cave.dir_change_factor", " %.2f"), 0, 4, settings.caveDirectionChangeFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.caveFlattenFactor = makeFloatSlider(gui, malisisText("cave.flatten_factor", " %.2f"), 0, 1.01f, settings.caveFlattenFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveAltFlattenFactor =
                                makeFloatSlider(gui, malisisText("cave.alt_flatten_factor", " %.2f"), 0, 1.01f, settings.caveAltFlattenFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.cavePrevHorizAccelerationWeight =
                                makeFloatSlider(gui, malisisText("cave.prev_horiz_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .cavePrevHorizAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.cavePrevVertAccelerationWeight =
                                makeFloatSlider(gui, malisisText("cave.prev_vert_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .cavePrevVertAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.caveMaxHorizAccelChange =
                                makeFloatSlider(gui, malisisText("cave.max_horiz_accel_change", " %.2f"), 0, 32, settings.caveMaxHorizAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveMaxVertAccelChange =
                                makeFloatSlider(gui, malisisText("cave.max_vert_accel_change", " %.2f"), 0, 32, settings.caveMaxVertAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.caveCarveStepRarity = makeIntSlider(gui, malisisText("cave.carve_step_rarity", " %d"), 1, 16, settings.caveCarveStepRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveFloorDepth = makeFloatSlider(gui, malisisText("cave.floor_depth", " %.2f"), -1, 1, settings.caveFloorDepth),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))


                // RAVINES

                .add(label(gui, malisisText("ravine.settings_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.ravineRarityPerChunk = makeIntSlider(gui, malisisText("ravine.rarity", " %d"), 8, 8192, settings.ravineRarityPerChunk),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.ravineYRange = makeRangeSlider(gui, percentageFormat("ravine.y_range", "%.2f"), 200, 200,
                        settings.ravineMinY, settings.ravineMaxY),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.ravineSizeFactor1 = makeFloatSlider(gui, malisisText("ravine.size_factor_1", " %.2f"), 0, 32, settings.ravineSizeFactor1),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineSizeFactor2 = makeFloatSlider(gui, malisisText("ravine.size_factor_2", " %.2f"), 0, 32, settings.ravineSizeFactor2),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.ravineYAngleRange = makeRangeSlider(gui, percentageFormat("ravine.y_angle_range", "%.2f"), 0, (float) Math.PI,
                        settings.ravineMinY, settings.ravineMaxY, (float) Math.PI),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineFlattenFactor =
                                makeFloatSlider(gui, malisisText("ravine.flatten_factor", " %.2f"), 0, 1.01f, settings.ravineSizeFactor2),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.ravineSizeAdd = makeFloatSlider(gui, malisisText("ravine.size_add", " %.2f"), 0, 64, settings.ravineSizeAdd),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineDirectionChangeFactor =
                                makeFloatSlider(gui, malisisText("ravine.direction_change_factor", " %.2f"), 0, 8, settings
                                        .ravineDirectionChangeFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.ravineRandomSizeFactorRange = makeRangeSlider(gui, percentageFormat("ravine.random_size_factor_range", "%.2f"),
                        0, 64, settings.ravineMinRandomSizeFactor, settings.ravineMaxRandomSizeFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.ravinePrevHorizAccelerationWeight =
                                makeFloatSlider(gui, malisisText("ravine.prev_horiz_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .ravinePrevHorizAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravinePrevVertAccelerationWeight =
                                makeFloatSlider(gui, malisisText("ravine.prev_vert_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .ravinePrevVertAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.ravineMaxHorizAccelChange =
                                makeFloatSlider(gui, malisisText("ravine.max_horiz_accel_change", " %.2f"), 0, 32, settings
                                        .ravineMaxHorizAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineMaxVertAccelChange =
                                makeFloatSlider(gui, malisisText("ravine.max_vert_accel_change", " %.2f"), 0, 32, settings.ravineMaxVertAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.ravineStretchYFactor =
                                makeFloatSlider(gui, malisisText("ravine.stretch_y_factor", " %.2f"), -1, 1, settings.ravineStretchYFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineCarveStepRarity =
                                makeIntSlider(gui, malisisText("ravine.carve_step_rarity", " %d"), 1, 16, settings.ravineCarveStepRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                // lava section

                .add(label(gui, malisisText("ravine.lava_section.group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(new UILabel(gui, malisisText("ravine.lava_section.select_mode"))
                                .setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build()),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.ravineLavaGenSelect = makeRavineGenLavaSection(gui),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(makeRavineGenLavaSectionTabs(gui),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))
                .init();
        this.container = layout;
    }

    public UIVerticalTableLayout getContainer() {
        return container;
    }

    private UISelect<RavineLavaMode> makeRavineGenLavaSection(ExtraGui gui) {
        return new UISelectLayoutSpecial<>(gui, 100, Lists.newArrayList(RavineLavaMode.values()));
    }

    private UIComponent<?> makeRavineGenLavaSectionTabs(ExtraGui gui) {

        UITabbedContainer tabs = new UITabbedContainer(gui);
        this.ravineLavaGenSelect.register(new Object() {
            private final Consumer<UISelect<?>> expandedSetFalse;

            {
                try {
                    Field expandedFld = UISelect.class.getDeclaredField("expanded");
                    expandedFld.setAccessible(true);
                    expandedSetFalse = select -> {
                        try {
                            expandedFld.set(select, false);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

            @Subscribe
            public void onSelect(UISelect.SelectEvent<RavineLavaMode> evt) {
                /*
                 * This is an awful hack to workaround a problem with MalisisCore:
                 *
                 * Firing this event causes re-layout of the containers, but this is still done
                 * while the UISelect is expanded, which means that it's going to be higher than when not expanded.
                 * The layout will take the additional height into account and in the end it will look visually wrong.
                 *
                 * Workaround: reflectively set expanded field to false here
                 */
                expandedSetFalse.accept(ravineLavaGenSelect);
                tabs.setTab(evt.getNewValue().ordinal());
            }
        });
        UIComponent<?> noLavaLabel = label(gui, malisisText("ravine.lava_section.no_config"), 20);

        UISlider<Float> lavaY = makeFloatSlider(gui, malisisText("ravine.lava_section.lava_y"), -200, 200, 0);
        lavaY.setSize(UIComponent.INHERITED, 20);

        UIRangeSlider<Float> fullyFilledToNoFillRange = makeRangeSlider(gui, percentageFormat("ravine.lava_section.lava_y_fill_range", "%.2f"),
                -16, 16, Float.NEGATIVE_INFINITY, -2, 0).setSize(UIComponent.INHERITED, 20);

        UIVerticalTableLayout arctanTable = new UIVerticalTableLayout(gui, 1);
        arctanTable.add(makeRangeSlider(gui, percentageFormat("ravine.lava_section.arctan_fill_range", "%.2f"), 0, 1, 0, 1)
                .setSize(UIComponent.INHERITED, 20), new UIVerticalTableLayout.GridLocation(0, 0, 1));
        arctanTable.add(makeFloatSlider(gui, malisisText("ravine.lava_section.arctan_mean_fill_y", " %.2f"), -400, 400, -100)
                .setSize(UIComponent.INHERITED, 20), new UIVerticalTableLayout.GridLocation(0, 1, 1));
        arctanTable.add(makeFloatSlider(gui, malisisText("ravine.lava_section.arctan_mean_fill_y_offset", "%.2f"), -400, 400, 0)
                .setSize(UIComponent.INHERITED, 20), new UIVerticalTableLayout.GridLocation(0, 2, 1));
        arctanTable.init();
        arctanTable.setSize(UIComponent.INHERITED, 20).setAutoResize(false, true);

        tabs.addTab(noLavaLabel, malisisText("ravine.lava_section.page.no_lava"));
        tabs.addTab(lavaY, malisisText("ravine.lava_section.page.lava_y"));
        tabs.addTab(fullyFilledToNoFillRange, malisisText("ravine.lava_section.page.lava_y_fill_range"));
        tabs.addTab(arctanTable, malisisText("ravine.lava_section.page.lava_arctan_fill"));
        tabs.setAutoResize(false, true);

        return tabs;
    }
}
