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
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.vanillaText;

import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.interaction.UISlider;

class StructuresAdvancedTab {

    // basic advanced cave config
    private final UISlider<Integer> rarityPerChunk;
    private final UISlider<Integer> maxInitialNodes;
    private final UISlider<Integer> largeNodeRarity;
    private final UISlider<Integer> largeNodeMaxBranches;
    private final UISlider<Integer> bigCaveRarity;
    private final UISlider<Float> caveSizeFactor1;
    private final UISlider<Float> caveSizeFactor2;
    private final UIRangeSlider<Float> bigCaveSizeFactorRange;
    private final UISlider<Float> caveSizeAdd;

    // more advanced cave config
    private final UISlider<Integer> alternateFlattenFactorRarity;
    private final UISlider<Float> flattenFactor;
    private final UISlider<Float> altFlattenFactor;
    private final UISlider<Float> directionChangeFactor;
    private final UISlider<Float> prevHorizAccelerationWeight;
    private final UISlider<Float> prevVertAccelerationWeight;
    private final UISlider<Float> maxHorizAccelChange;
    private final UISlider<Float> maxVertAccelChange;
    private final UISlider<Integer> carveStepRarity;
    private final UISlider<Float> caveFloorDepth;

    private final UIVerticalTableLayout container;

    StructuresAdvancedTab(ExtraGui gui, CustomGeneratorSettings settings) {
        int y = -1;

        UIVerticalTableLayout layout = new UIVerticalTableLayout(gui, 6);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                .setInsets(VERTICAL_INSETS, VERTICAL_INSETS, HORIZONTAL_INSETS, HORIZONTAL_INSETS)
                .add(label(gui, malisisText("cave.settings_group"), 20),
                        new UIVerticalTableLayout.GridLocation(WIDTH_1_COL * 0, ++y, WIDTH_1_COL))

                .add(this.rarityPerChunk = makeIntSlider(gui, malisisText("cave.rarity", " %d"), 1, 1024, settings.rarityPerChunk),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.maxInitialNodes = makeIntSlider(gui, malisisText("cave.max_init_nodes", " %d"), 1, 20, settings.maxInitialNodes),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.largeNodeRarity = makeIntSlider(gui, malisisText("cave.large_node_rarity", " %d"), 1, 50, settings.largeNodeRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.largeNodeMaxBranches =
                                makeIntSlider(gui, malisisText("cave.large_node_max_branches", " %d"), 1, 15, settings.largeNodeMaxBranches),
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

                .add(this.alternateFlattenFactorRarity =
                                makeIntSlider(gui, malisisText("cave.alt_flatten_rarity", " %d"), 1, 100, settings.alternateFlattenFactorRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.directionChangeFactor =
                                makeFloatSlider(gui, malisisText("cave.dir_change_factor", " %.2f"), 0, 4, settings.directionChangeFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.flattenFactor = makeFloatSlider(gui, malisisText("cave.flatten_factor", " %.2f"), 0, 1.01f, settings.flattenFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.altFlattenFactor =
                                makeFloatSlider(gui, malisisText("cave.alt_flatten_factor", " %.2f"), 0, 1.01f, settings.altFlattenFactor),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.prevHorizAccelerationWeight =
                                makeFloatSlider(gui, malisisText("cave.prev_horiz_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .prevHorizAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.prevVertAccelerationWeight =
                                makeFloatSlider(gui, malisisText("cave.prev_vert_accel_weight", " %.2f"), 0, 1.01f, settings
                                        .prevVertAccelerationWeight),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.maxHorizAccelChange =
                                makeFloatSlider(gui, malisisText("cave.max_horiz_accel_change", " %.2f"), 0, 32, settings.maxHorizAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.maxVertAccelChange =
                                makeFloatSlider(gui, malisisText("cave.max_vert_accel_change", " %.2f"), 0, 32, settings.maxVertAccelChange),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .add(this.carveStepRarity = makeIntSlider(gui, malisisText("cave.carve_step_rarity", " %d"), 1, 16, settings.carveStepRarity),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 0, ++y, WIDTH_2_COL))
                .add(this.caveFloorDepth = makeFloatSlider(gui, malisisText("cave.floor_depth", " %.2f"), -1, 1, settings.caveFloorDepth),
                        new UIVerticalTableLayout.GridLocation(WIDTH_2_COL * 1, y, WIDTH_2_COL))

                .init();
        this.container = layout;
    }

    public UIVerticalTableLayout getContainer() {
        return container;
    }
}
