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

import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.vanillaText;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonSyntaxException;
import cubicchunks.CubicChunks;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.NoTranslationFont;
import cubicchunks.worldgen.gui.component.UIBorderLayout;
import cubicchunks.worldgen.gui.component.UIColoredPanel;
import cubicchunks.worldgen.gui.component.UIMultilineLabel;
import cubicchunks.worldgen.gui.component.UITabbedContainer;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.GuiTexture;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.element.SimpleGuiShape;
import net.malisis.core.renderer.font.FontOptions;
import net.minecraft.client.gui.GuiCreateWorld;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomCubicGui extends ExtraGui {

    public static final GuiTexture CUSTOM_TEXTURE = new GuiTexture(CubicChunks.location("textures/gui/gui.png"));

    public static final int WIDTH_1_COL = 6;
    public static final int WIDTH_2_COL = 3;
    public static final int WIDTH_3_COL = 2;

    public static final int VERTICAL_PADDING = 30;
    public static final int HORIZONTAL_PADDING = 25;
    public static final int VERTICAL_INSETS = 2;
    public static final int HORIZONTAL_INSETS = 4;
    static final int BTN_WIDTH = 60;

    private final GuiCreateWorld parent;
    private UITabbedContainer tabs;

    private BasicSettingsTab basicSettings;
    private OreSettingsTab oreSettings;
    private AdvancedTerrainShapeTab advancedterrainShapeSettings;

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
        CustomGeneratorSettings conf = CustomGeneratorSettings.fromJson(parent.chunkProviderSettingsJson);
        reinit(conf);
    }

    public void reinit(CustomGeneratorSettings conf) {
        clearScreen();

        this.basicSettings = new BasicSettingsTab(this, conf);
        this.oreSettings = new OreSettingsTab(this, conf);
        this.advancedterrainShapeSettings = new AdvancedTerrainShapeTab(this, conf);

        tabs = makeTabContainer();
        tabs.addTab(inPanel(basicSettings.getContainer()), vanillaText("basic_tab_title"));
        tabs.addTab(inPanel(oreSettings.getContainer()), vanillaText("ores_tab_title"));
        tabs.addTab(inPanel(advancedterrainShapeSettings.getContainer()), vanillaText("advanced_tab_title"));
        addToScreen(tabs);
    }

    private UIContainer<?> inPanel(UIComponent<?> comp) {
        UIColoredPanel panel = new UIColoredPanel(this);
        panel.setSize(UIComponent.INHERITED, UIComponent.INHERITED - VERTICAL_PADDING * 2);
        panel.setPosition(0, VERTICAL_PADDING);
        panel.add(comp);
        return panel;
    }

    private UITabbedContainer makeTabContainer() {
        final int xSize = UIComponent.INHERITED - HORIZONTAL_PADDING * 2 - HORIZONTAL_INSETS * 2;
        final int ySize = VERTICAL_PADDING;
        final int xPos = HORIZONTAL_PADDING + HORIZONTAL_INSETS;
        UIButton prev = new UIButton(this, malisisText("previous_page")).setSize(BTN_WIDTH, 20);
        UIButton next = new UIButton(this, malisisText("next_page")).setSize(BTN_WIDTH, 20);

        UIMultilineLabel label = new UIMultilineLabel(this)
                .setTextAnchor(Anchor.CENTER)
                .setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build());

        UIBorderLayout upperLayout = new UIBorderLayout(this)
                .setSize(xSize, ySize)
                .setPosition(xPos, 0)
                .add(prev, UIBorderLayout.Border.LEFT)
                .add(next, UIBorderLayout.Border.RIGHT)
                .add(label, UIBorderLayout.Border.CENTER);

        UIButton done = new UIButton(this, malisisText("done")).setAutoSize(false).setSize(BTN_WIDTH, 20);
        done.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                CustomCubicGui.this.done();
            }
        });
        done.setPosition(0, 0);

        UIButton sharePreset = new UIButton(this, malisisText("presets")).setAutoSize(false).setSize(BTN_WIDTH, 20);
        sharePreset.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                new ExtraGui() {

                    @Override public void construct() {

                        UIButton done, cancel;
                        UITextField text;
                        UIVerticalTableLayout<?> table = new UIVerticalTableLayout<>(this, 2);
                        table.setPadding(HORIZONTAL_PADDING, 0);
                        table.setSize(UIComponent.INHERITED, UIComponent.INHERITED)
                                .setInsets(5, 5, 10, 10)
                                .add(text = new UITextField(this, "").setSize(this.width - 20 - HORIZONTAL_PADDING*2, 10),
                                        new UIVerticalTableLayout.GridLocation(0, 0, 2))
                                .add(done = new UIButton(this, malisisText("presets.done")).setAutoSize(false).setSize(0, 20),
                                        new UIVerticalTableLayout.GridLocation(1, 1, 1))
                                .add(cancel = new UIButton(this, malisisText("presets.cancel")).setAutoSize(false).setSize(0, 20),
                                        new UIVerticalTableLayout.GridLocation(0, 1, 1));
                        text.setFont(NoTranslationFont.DEFAULT);
                        text.setText(getSettingsJson());
                        text.getCursorPosition().jumpToEnd();
                        done.register(new Object() {
                            @Subscribe
                            public void onClick(UIButton.ClickEvent evt) {
                                try {
                                    CustomGeneratorSettings settings = CustomGeneratorSettings.fromJson(text.getText());
                                    CustomCubicGui.this.reinit(settings);
                                    mc.displayGuiScreen(CustomCubicGui.this);
                                } catch (JsonSyntaxException | NumberFormatException ex) {
                                    done.setFontOptions(FontOptions.builder().color(0x00FF2222).build());
                                }
                            }
                        });
                        cancel.register(new Object() {
                            @Subscribe
                            public void onClick(UIButton.ClickEvent evt) {
                                mc.displayGuiScreen(CustomCubicGui.this);
                            }
                        });
                        addToScreen(inPanel(table));
                        table.setSize(UIComponent.INHERITED, UIComponent.INHERITED);
                    }
                }.display();
            }
        });
        sharePreset.setPosition(BTN_WIDTH + 10, 0);

        UIContainer<?> container = new UIContainer<>(this);
        container.add(done, sharePreset);
        container.setSize(BTN_WIDTH * 2 + 10, 20);

        UIBorderLayout lowerLayout = new UIBorderLayout(this)
                .setSize(xSize, ySize)
                .setAnchor(Anchor.BOTTOM).setPosition(xPos, 0)
                .add(container, UIBorderLayout.Border.CENTER);

        UITabbedContainer tabGroup = new UITabbedContainer(this, prev, next, label::setText);
        tabGroup.add(upperLayout, lowerLayout);

        return tabGroup;
    }

    private void done() {
        parent.chunkProviderSettingsJson = getSettingsJson();
        this.mc.displayGuiScreen(parent);
    }

    public CustomGeneratorSettings getConfig() {
        CustomGeneratorSettings conf = CustomGeneratorSettings.defaults();
        this.basicSettings.writeConfig(conf);
        this.oreSettings.writeConfig(conf);
        this.advancedterrainShapeSettings.writeConfig(conf);
        return conf;
    }
    String getSettingsJson() {
        return getConfig().toJson();
    }
}
