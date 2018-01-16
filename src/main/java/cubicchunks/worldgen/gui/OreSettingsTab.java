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

import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_PADDING;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.label;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeCheckbox;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeFloatSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeIntSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makePositiveExponentialSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeRangeSlider;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.vanillaText;

import com.google.common.eventbus.Subscribe;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings.PeriodicGaussianOreConfig;
import cubicchunks.worldgen.gui.component.UIBlockStateButton;
import cubicchunks.worldgen.gui.component.UIBlockStateSelect;
import cubicchunks.worldgen.gui.component.UILayout;
import cubicchunks.worldgen.gui.component.UIList;
import cubicchunks.worldgen.gui.component.UIRangeSlider;
import cubicchunks.worldgen.gui.component.UISplitLayout;
import cubicchunks.worldgen.gui.component.UISplitLayout.Type;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UISlider;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class OreSettingsTab {

    private final ArrayList<UIComponent<?>> componentList;

    private final UIContainer<?> container;

    private final List<UIStandardOreOptions> standardOptions = new ArrayList<>();

    private final List<UIPeriodicGaussianOreOptions> periodicGaussianOptions = new ArrayList<>();

    <T> OreSettingsTab(ExtraGui gui, CustomGeneratorSettings settings) {
        this.componentList = new ArrayList<>();
        UIList<UIComponent<?>, UIComponent<?>> layout = new UIList<>(gui, this.componentList, x -> x);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED);

        layout.add(new UIButton(gui, malisisText("add_ore")).setAutoSize(false).setSize(UIComponent.INHERITED, 30).register(
                new Object() {
                    @Subscribe
                    public void onClick(UIButton.ClickEvent evt) {
                        componentList.add(1, new UIStandardOreOptions(gui, CustomGeneratorSettings.StandardOreConfig.builder()
                                .size(8).probability(1).attempts(4).block(Blocks.TNT.getDefaultState()).create()));
                    }
                }
        ));

        for (CustomGeneratorSettings.StandardOreConfig conf : settings.standardOres) {
            layout.add(new UIStandardOreOptions(gui, conf));
        }

        for (CustomGeneratorSettings.PeriodicGaussianOreConfig conf : settings.periodicGaussianOres) {
            layout.add(new UIPeriodicGaussianOreOptions(gui, conf));
        }
        layout.setRightPadding(HORIZONTAL_PADDING + 6);
        this.container = layout;
    }

    UIContainer<?> getContainer() {
        return container;
    }

    void writeConfig(CustomGeneratorSettings conf) {
        conf.standardOres.clear();
        conf.periodicGaussianOres.clear();
        for (UIComponent<?> c : componentList) {

            if (c instanceof UIPeriodicGaussianOreOptions) {
                conf.periodicGaussianOres.add(((UIPeriodicGaussianOreOptions) c).toConfig());
            } else if (c instanceof UIStandardOreOptions) {
                conf.standardOres.add(((UIStandardOreOptions) c).toConfig());
            }
        }
    }

    private void replaceComponent(UIComponent<?> oldC, UIComponent<?> newC) {
        this.componentList.add(this.componentList.indexOf(oldC), newC);
        this.componentList.remove(oldC);
        ((UILayout<?>) this.container).setNeedsLayoutUpdate();
    }

    private class UIStandardOreOptions extends UIVerticalTableLayout {

        /*
        The layout:

        Biome selection Off
        +------+------+------+------+------+------+
        |BLOCK : <=========NAME==========> :DELETE|
        |STATE : <=BLOCKSTATE PROPERTIES=> : TYPE |
        +------+------+------+------+------+------+
        | <===SPAWN SIZE===> : <===VEIN COUNT===> |
        | <===SPAWN PROB===> : <==BIOME ON/OFF==> |
        | <============SPAWN HEIGHTS============> |
        +------+------+------+------+------+------+

        Biome selection On
        +------+------+------+------+------+------+ ----\
        |BLOCK : <=========NAME==========> :DELETE|\TABLE\
        |STATE : <=BLOCKSTATE PROPERTIES=> : TYPE |/LAYOUT\
        +--------------+--------------+-----------+        \  VERTICAL TABLE
        |  SPAWN SIZE  :  VEIN COUNT  |[V]Biome 1||        /  LAYOUT (this)
        |  SPAWN PROB  : BIOME ON/OFF |[V]Biome 2 |       /
        | <======SPAWN HEIGHTS======> |[ ]Biome 3 |      /
        +--------------+--------------^-----------+ ----/
        |                             |           |
        |<---VERTICAL TABLE LAYOUT--->|<-UI LIST->|
                  (MAIN AREA)         \->Split layout
        */
        private final UIBlockStateButton block;
        private final UIComponent<?> name;
        private final UIButton delete;
        private final UISelect<OreGenType> type;


        private final UISlider<Integer> size;
        private final UISlider<Integer> attempts;

        private final UISlider<Float> probability;
        private final UICheckBox selectBiomes;

        private final UIRangeSlider<Float> heightRange;


        private final UIList<Biome, UICheckBox> biomesArea;
        private CustomGeneratorSettings.StandardOreConfig config;

        public UIStandardOreOptions(ExtraGui gui, CustomGeneratorSettings.StandardOreConfig config) {
            super(gui, 6);

            this.config = config;

            this.block = new UIBlockStateButton(gui, config.blockstate);
            this.name = makeLabel(gui, config);
            this.delete = new UIButton(gui, malisisText("delete")).setSize(10, 20).setAutoSize(false);
            this.type = new UISelect<>(gui, 10, Arrays.asList(OreGenType.values()));
            this.size = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, config.spawnSize);
            this.attempts = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, config.spawnTries);
            this.probability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), config.spawnProbability);
            this.selectBiomes = makeCheckbox(gui, malisisText("select_biomes"), config.biomes != null);
            this.heightRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, config.minHeight, config.maxHeight);

            UISplitLayout<?> deleteTypeArea = new UISplitLayout<>(gui, Type.STACKED, delete, type).setSizeOf(UISplitLayout.Pos.SECOND, 10)
                    .setSize(0, 30);
            UIVerticalTableLayout<?> mainArea = new UIVerticalTableLayout<>(gui, 6).autoFitToContent(true);

            // use new ArrayList so it can be sorted
            biomesArea = new UIList<>(gui, new ArrayList<>(ForgeRegistries.BIOMES.getValues()), this::makeBiomeCheckbox);

            this.block.onClick(btn ->
                    new UIBlockStateSelect<>(gui).display(state -> {
                        block.setBlockState(state);
                        updateLabel(gui, name);
                    })
            );
            this.delete.register(new Object() {
                @Subscribe
                public void onClick(UIButton.ClickEvent evt) {
                    container.remove(UIStandardOreOptions.this);
                }
            });
            this.selectBiomes.register(new Object() {
                @Subscribe
                public void onClick(UICheckBox.CheckEvent evt) {
                    allowSelectBiomes(biomesArea, evt.isChecked());
                }
            });
            this.type.register(new Object() {
                @Subscribe
                public void onClick(UISelect.SelectEvent evt) {
                    if (evt.getNewValue() == OreGenType.PERIODIC_GAUSSIAN) {
                        replaceComponent(UIStandardOreOptions.this, new UIPeriodicGaussianOreOptions(gui,
                                PeriodicGaussianOreConfig.builder().fromStandard(toConfig()).create()));
                    }
                }
            });
            this.type.select(OreGenType.UNIFORM);

            setupMainArea(mainArea);
            allowSelectBiomes(biomesArea, this.selectBiomes.isChecked());
            setupBiomeArea(config, biomesArea);
            setupThis(gui, deleteTypeArea, mainArea, biomesArea);
        }

        private CustomGeneratorSettings.StandardOreConfig toConfig() {
            return CustomGeneratorSettings.StandardOreConfig.builder()
                    .biomes(this.selectBiomes.isChecked() ? this.biomesArea.getData().toArray(new Biome[0]) : null)
                    .attempts(this.attempts.getValue())
                    .block(this.block.getState())
                    .minHeight(this.heightRange.getMinValue())
                    .maxHeight(this.heightRange.getMaxValue())
                    .probability(this.probability.getValue())
                    .size(this.size.getValue())
                    .create();
        }

        private void setupMainArea(UIVerticalTableLayout<?> mainArea) {
            int y = -1;
            mainArea.add(this.size, new GridLocation(0, ++y, 3));
            mainArea.add(this.attempts, new GridLocation(3, y, 3));
            mainArea.add(this.probability, new GridLocation(0, ++y, 3));
            mainArea.add(this.selectBiomes, new GridLocation(3, y, 3));
            mainArea.add(this.heightRange, new GridLocation(0, ++y, 6));
        }

        private void allowSelectBiomes(UIList<Biome, UICheckBox> biomes, boolean checked) {
            biomes.setVisible(checked);
            if (!biomes.isVisible()) {
                // TODO: is this the expected behavior for users? Or should the selected ones persist?
                biomes.getData().forEach(e -> biomes.component(e).setChecked(true));
            }
        }

        private void setupBiomeArea(CustomGeneratorSettings.StandardOreConfig config, UIList<Biome, UICheckBox> biomesArea) {
            biomesArea.setRightPadding(6);

            if (config.biomes != null) {
                config.biomes.forEach(b -> {
                    biomesArea.component(b).setChecked(true);
                });
            }

            ((List<Biome>) biomesArea.getData()).sort((b1, b2) ->
                    biomesArea.component(b1).isChecked() && !biomesArea.component(b2).isChecked() ? 1 : 0
            );
        }

        private void setupThis(ExtraGui gui, UIComponent<?> deleteTypeArea, UIComponent<?> mainArea, UILayout<?> biomesArea) {
            UISplitLayout split =
                    new UISplitLayout<>(gui, Type.SIDE_BY_SIDE, mainArea, biomesArea).sizeWeights(2, 1).autoFitToContent(true).userResizable(false);

            this.autoFitToContent(true);
            this.add(this.name, new GridLocation(1, 0, 4));
            this.add(this.block, new GridLocation(0, 0, 1));
            this.add(deleteTypeArea, new GridLocation(5, 0, 1));
            this.add(split, new GridLocation(0, 1, 6));
            biomesArea.setHeightFunc(() -> ((UIContainer) split.getFirst()).getContentHeight());
        }

        private UICheckBox makeBiomeCheckbox(Biome biome) {
            return new UICheckBox(getGui(), String.format("%s (%s)", biome.getBiomeName(), biome.getRegistryName()));
        }

        private UIContainer<?> makeLabel(ExtraGui gui, CustomGeneratorSettings.StandardOreConfig config) {
            UIVerticalTableLayout<?> label = new UIVerticalTableLayout<>(gui, 1).setInsets(0, 0, 0, 0);
            updateLabel(gui, label);
            return label;
        }
        private void updateLabel(ExtraGui gui, UIComponent<?> label) {

            ((UIContainer) label).removeAll();

            String stateStr = block.getState().toString();
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


            UIComponent<?> l1 = label(gui, name);
            UIComponent<?> l2 = label(gui, props);
            ((UIContainer<?>) label).add(l1, l2);
            label.setSize(label.getWidth(), l1.getHeight() + l2.getHeight());
        }
    }

    // TODO: avoid duplicating code here
    private class UIPeriodicGaussianOreOptions extends UIVerticalTableLayout {

        /*
        The layout:

        Biome selection Off
        +------+------+------+------+------+------+
        |BLOCK : <=========NAME==========> :DELETE|
        |STATE : <=BLOCKSTATE PROPERTIES=> : TYPE |
        +------+------+------+------+------+------+
        | <===SPAWN SIZE===> : <===VEIN COUNT===> |
        | <===SPAWN PROB===> : <==BIOME ON/OFF==> |
        | <======MEAN======> : <=====SPACING====> |
        | <============STD DEVIATION============> |
        | <============SPAWN HEIGHTS============> |
        +------+------+------+------+------+------+

        Biome selection On
        +------+------+------+------+------+------+ ----\
        |BLOCK : <=========NAME==========> :DELETE|\TABLE\
        |STATE : <=BLOCKSTATE PROPERTIES=> : TYPE |/LAYOUT\
        +--------------+--------------+-----------+        \
        |  SPAWN SIZE  :  VEIN COUNT  |[V]Biome 1||         \  VERTICAL TABLE
        |  SPAWN PROB  : BIOME ON/OFF |[V]Biome 2 |         /  LAYOUT (this)
        | <===MEAN===> : <=SPACING==> |[V]Biome 3 |        /
        | <======STD DEVIATION======> |[ ]Biome 4 |       /
        | <======SPAWN HEIGHTS======> |[ ]Biome 5 |      /
        +--------------+--------------^-----------+ ----/
        |                             |           |
        |<---VERTICAL TABLE LAYOUT--->|<-UI LIST->|
                  (MAIN AREA)         \->Split layout
        */
        private final UIBlockStateButton block;
        private final UIComponent<?> name;
        private final UIButton delete;
        private final UISelect<OreGenType> type;


        private final UISlider<Integer> size;
        private final UISlider<Integer> attempts;

        private final UISlider<Float> mean;
        private final UISlider<Float> spacing;

        private final UISlider<Float> stdDev;

        private final UISlider<Float> probability;
        private final UICheckBox selectBiomes;

        private final UIRangeSlider<Float> heightRange;


        private final UIList<Biome, UICheckBox> biomesArea;

        private PeriodicGaussianOreConfig config;

        public UIPeriodicGaussianOreOptions(ExtraGui gui, PeriodicGaussianOreConfig config) {
            super(gui, 6);

            this.config = config;

            this.block = new UIBlockStateButton(gui, config.blockstate);
            this.name = makeLabel(gui, config);
            this.delete = new UIButton(gui, malisisText("delete")).setSize(10, 20).setAutoSize(false);
            this.type = new UISelect<>(gui, 10, Arrays.asList(OreGenType.values()));
            this.size = makeIntSlider(gui, malisisText("spawn_size", " %d"), 1, 50, config.spawnSize);
            this.attempts = makeIntSlider(gui, malisisText("spawn_tries", " %d"), 1, 40, config.spawnTries);
            this.mean = makeFloatSlider(gui, malisisText("mean_height", " %.3f"), -4.0f, 4.0f, config.heightMean);
            this.spacing = makePositiveExponentialSlider(gui, malisisText("spacing_height", " %.3f"), -1f, 6.0f, config.heightSpacing);
            this.stdDev = makeFloatSlider(gui, malisisText("height_std_dev", " %.3f"), 0f, 1f, config.heightStdDeviation);
            this.probability = makeFloatSlider(gui, malisisText("spawn_probability", " %.3f"), config.spawnProbability);
            this.selectBiomes = makeCheckbox(gui, malisisText("select_biomes"), config.biomes != null);
            this.heightRange = makeRangeSlider(gui, vanillaText("spawn_range"), -2.0f, 2.0f, config.minHeight, config.maxHeight);

            UISplitLayout<?> deleteTypeArea =
                    new UISplitLayout<>(gui, Type.STACKED, delete, type).sizeWeights(1, 1).setSizeOf(UISplitLayout.Pos.SECOND, 10)
                            .setSize(0, 30);
            UIVerticalTableLayout<?> mainArea = new UIVerticalTableLayout<>(gui, 6).autoFitToContent(true);

            // use new ArrayList so it can be sorted
            biomesArea = new UIList<>(gui, new ArrayList<>(ForgeRegistries.BIOMES.getValues()), this::makeBiomeCheckbox);

            this.block.onClick(btn ->
                    new UIBlockStateSelect<>(gui).display(state -> {
                        block.setBlockState(state);
                        updateLabel(gui, name);
                    })
            );
            this.delete.register(new Object() {
                @Subscribe
                public void onClick(UIButton.ClickEvent evt) {
                    standardOptions.remove(UIPeriodicGaussianOreOptions.this);
                    container.remove(UIPeriodicGaussianOreOptions.this);
                }
            });
            this.selectBiomes.register(new Object() {
                @Subscribe
                public void onClick(UICheckBox.CheckEvent evt) {
                    allowSelectBiomes(biomesArea, evt.isChecked());
                }
            });
            this.type.register(new Object() {
                @Subscribe
                public void onClick(UISelect.SelectEvent<OreGenType> evt) {
                    if (evt.getNewValue() == OreGenType.UNIFORM) {
                        replaceComponent(UIPeriodicGaussianOreOptions.this, new UIStandardOreOptions(gui, CustomGeneratorSettings.StandardOreConfig
                                .builder().fromPeriodic(toConfig()).create()));
                    }
                }
            });
            this.type.select(OreGenType.PERIODIC_GAUSSIAN);

            setupMainArea(mainArea);
            allowSelectBiomes(biomesArea, this.selectBiomes.isChecked());
            setupBiomeArea(config, biomesArea);
            setupThis(gui, deleteTypeArea, mainArea, biomesArea);
        }

        private PeriodicGaussianOreConfig toConfig() {
            return CustomGeneratorSettings.PeriodicGaussianOreConfig.builder()
                    .biomes(this.selectBiomes.isChecked() ? this.biomesArea.getData().toArray(new Biome[0]) : null)
                    .attempts(this.attempts.getValue())
                    .block(this.block.getState())
                    .minHeight(this.heightRange.getMinValue())
                    .maxHeight(this.heightRange.getMaxValue())
                    .probability(this.probability.getValue())
                    .size(this.size.getValue())
                    .heightMean(this.mean.getValue())
                    .heightSpacing(this.spacing.getValue())
                    .heightStdDeviation(this.stdDev.getValue())
                    .create();
        }

        private void setupMainArea(UIVerticalTableLayout<?> mainArea) {
            int y = -1;
            mainArea.add(this.size, new GridLocation(0, ++y, 3));
            mainArea.add(this.attempts, new GridLocation(3, y, 3));
            mainArea.add(this.probability, new GridLocation(0, ++y, 3));
            mainArea.add(this.selectBiomes, new GridLocation(3, y, 3));
            mainArea.add(this.mean, new GridLocation(0, ++y, 3));
            mainArea.add(this.spacing, new GridLocation(3, y, 3));
            mainArea.add(this.stdDev, new GridLocation(0, ++y, 6));
            mainArea.add(this.heightRange, new GridLocation(0, ++y, 6));
        }

        private void allowSelectBiomes(UIList<Biome, UICheckBox> biomes, boolean checked) {
            biomes.setVisible(checked);
            if (!biomes.isVisible()) {
                // TODO: is this the expected behavior for users? Or should the selected ones persist?
                biomes.getData().forEach(e -> biomes.component(e).setChecked(true));
            }
        }

        private void setupBiomeArea(PeriodicGaussianOreConfig config, UIList<Biome, UICheckBox> biomesArea) {
            biomesArea.setRightPadding(6);

            if (config.biomes != null) {
                config.biomes.forEach(b -> {
                    biomesArea.component(b).setChecked(true);
                });
            }

            ((List<Biome>) biomesArea.getData()).sort((b1, b2) ->
                    biomesArea.component(b1).isChecked() && !biomesArea.component(b2).isChecked() ? 1 : 0
            );
        }

        private void setupThis(ExtraGui gui, UIComponent<?> deleteTypeArea, UIComponent<?> mainArea, UILayout<?> biomesArea) {
            UISplitLayout split =
                    new UISplitLayout<>(gui, Type.SIDE_BY_SIDE, mainArea, biomesArea).sizeWeights(2, 1).autoFitToContent(true).userResizable(false);

            this.autoFitToContent(true);
            this.add(this.name, new GridLocation(1, 0, 4));
            this.add(this.block, new GridLocation(0, 0, 1));
            this.add(deleteTypeArea, new GridLocation(5, 0, 1));
            this.add(split, new GridLocation(0, 1, 6));
            biomesArea.setHeightFunc(() -> ((UIContainer) split.getFirst()).getContentHeight());
        }

        private UICheckBox makeBiomeCheckbox(Biome biome) {
            return new UICheckBox(getGui(), String.format("%s (%s)", biome.getBiomeName(), biome.getRegistryName()));
        }

        private UIContainer<?> makeLabel(ExtraGui gui, CustomGeneratorSettings.PeriodicGaussianOreConfig config) {
            UIVerticalTableLayout<?> label = new UIVerticalTableLayout<>(gui, 1).setInsets(0, 0, 0, 0);
            updateLabel(gui, label);
            return label;
        }

        private void updateLabel(ExtraGui gui, UIComponent<?> label) {

            ((UIContainer) label).removeAll();

            String stateStr = block.getState().toString();
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


            UIComponent<?> l1 = label(gui, name);
            UIComponent<?> l2 = label(gui, props);
            ((UIContainer<?>) label).add(l1, l2);
            label.setSize(label.getWidth(), l1.getHeight() + l2.getHeight());
        }
    }

    public enum OreGenType {
        UNIFORM, PERIODIC_GAUSSIAN
    }
}
