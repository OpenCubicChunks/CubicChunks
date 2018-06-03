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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cubicchunks.worldgen.gui.component.UIBorderLayout;
import cubicchunks.worldgen.gui.component.UIColoredPanel;
import cubicchunks.worldgen.gui.component.UIFlatTerrainLayer;
import cubicchunks.worldgen.gui.component.UIMultilineLabel;
import cubicchunks.worldgen.gui.component.UITabbedContainer;
import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.renderer.font.FontOptions;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SelectBlockGui extends ExtraGui {

    public static final int WIDTH_1_COL = 6;
    public static final int WIDTH_2_COL = 3;
    public static final int WIDTH_3_COL = 2;

    static final int VERTICAL_PADDING = 30;
    static final int HORIZONTAL_PADDING = 25;
    static final int VERTICAL_INSETS = 2;
    static final int HORIZONTAL_INSETS = 4;
    static final int BTN_WIDTH = 60;

    private UITabbedContainer tabs;
    private UIFlatTerrainLayer layer;
    private Block block;

    public SelectBlockGui(UIFlatTerrainLayer layerFor, @Nullable Block blockFor) {
        super();
        layer = layerFor;
        block = blockFor;
    }

    /**
     * Called before display() if this {@link MalisisGui} is not constructed
     * yet.<br> Called when Ctrl+R is pressed to rebuild the GUI.
     */
    @Override
    public void construct() {
        tabs = makeTabContainer();
        SelectBlockTab sbct = null;
        if (block == null) {
            List<IBlockState> blockStates = new ArrayList<>();
            Block.REGISTRY.forEach(block -> {
                if (!block.hasTileEntity(block.getDefaultState())) {
                    blockStates.add(block.getDefaultState());
                }
            });
            sbct = new SelectBlockTab(this, layer, blockStates, SelectBlockTab.ClickAction.OPEN_CHILD);
        } else {
            sbct = new SelectBlockTab(this, layer, block.getBlockState().getValidStates(), SelectBlockTab.ClickAction.SET_STATE_AND_OPEN_PARENT);
        }
        tabs.addTab(inPanel(sbct.getContainer()), malisisText("select_block"));
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
        final int xSize = UIComponent.INHERITED - HORIZONTAL_PADDING * 1 - HORIZONTAL_INSETS * 2;
        final int ySize = VERTICAL_PADDING;
        final int xPos = HORIZONTAL_INSETS;
        UIButton prev = new UIButton(this, malisisText("previous_page")).setSize(BTN_WIDTH, 20);
        UIButton next = new UIButton(this, malisisText("next_page")).setSize(BTN_WIDTH, 20);

        UIMultilineLabel label = new UIMultilineLabel(this)
                .setTextAnchor(Anchor.CENTER)
                .setFontOptions(FontOptions.builder().color(0xFFFFFF).shadow().build());

        UIBorderLayout upperLayout = new UIBorderLayout(this)
                .setSize(xSize, ySize)
                .setPosition(xPos, 0)
                .add(label, UIBorderLayout.Border.CENTER);

        UIBorderLayout lowerLayout = new UIBorderLayout(this)
                .setSize(xSize, ySize)
                .setAnchor(Anchor.BOTTOM).setPosition(xPos, 0);

        UITabbedContainer tabGroup = new UITabbedContainer(this, prev, next, label::setText);
        tabGroup.add(upperLayout, lowerLayout);

        return tabGroup;
    }
}
