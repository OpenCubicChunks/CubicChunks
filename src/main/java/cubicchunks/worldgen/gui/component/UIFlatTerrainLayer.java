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
package cubicchunks.worldgen.gui.component;

import cubicchunks.CubicChunks;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.decoration.UISeparator;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;

import com.google.common.eventbus.Subscribe;

import cubicchunks.worldgen.generator.flat.Layer;
import cubicchunks.worldgen.gui.FlatCubicGui;
import cubicchunks.worldgen.gui.FlatLayersTab;
import cubicchunks.worldgen.gui.SelectBlockGui;

public class UIFlatTerrainLayer extends UIContainer<UIFlatTerrainLayer> {

    private static final int BTN_WIDTH = 90;
    private final FlatLayersTab flatLayersTab;
    public final Layer layer;
    private final UIButton addLayer;
    private final UIButton removeLayer;
    private final UIButton selectBlock;
    private final UILabel blockName;
    private final UILabel from;
    private final UILabel to;
    private final UISeparator separator;
    private final UITextField fromField;
    private final UITextField toField;
    private final UIBlockStateButton blockButton;

    private final FlatCubicGui gui;

    public UIFlatTerrainLayer(FlatCubicGui guiFor, FlatLayersTab flatLayersTabFor, Layer layerFor) {
        super(guiFor);
        this.setSize(UIComponent.INHERITED, 60);
        this.flatLayersTab = flatLayersTabFor;
        this.layer = layerFor;
        this.gui = guiFor;
        blockButton = new UIBlockStateButton<>(gui, layer.blockState).onClick(btn -> {
            new SelectBlockGui(this, null).display();
        }).setPosition(4, 0);
        add(blockButton);

        selectBlock = new UIButton(gui, malisisText("select_block")).setSize(BTN_WIDTH, 20).setPosition(0, 20)
                .register(new Object() {

                    @Subscribe
                    public void onClick(UIButton.ClickEvent evt) {
                        new SelectBlockGui(UIFlatTerrainLayer.this, null).display();
                    }
                });
        add(selectBlock);

        blockName = new UILabel(gui, this.layer.blockState.getBlock().getLocalizedName()).setPosition(30, 5);
        add(blockName);

        addLayer = new UIButton(gui, malisisText("add_layer")).setSize(BTN_WIDTH, 20).setPosition(0, 0)
                .setAnchor(Anchor.RIGHT)
                .register(new Object() {

                    @Subscribe
                    public void onClick(UIButton.ClickEvent evt) {
                        UIFlatTerrainLayer.this.addLayer();
                    }
                });
        add(addLayer);

        removeLayer = new UIButton(gui, malisisText("remove_layer")).setSize(BTN_WIDTH, 20).setPosition(0, 20)
                .setAnchor(Anchor.RIGHT)
                .register(new Object() {

                    @Subscribe
                    public void onClick(UIButton.ClickEvent evt) {
                        UIFlatTerrainLayer.this.removeLayer();
                    }
                });
        add(removeLayer);

        toField = new UITextField(gui, String.valueOf(this.layer.toY), false).setPosition(0, 45, Anchor.RIGHT).setSize(40, 5);
        add(toField);

        to = new UILabel(gui, malisisText("to_exclusively"), false);
        to.setPosition(-10 - toField.getWidth(), 47, Anchor.RIGHT);
        add(to);

        from = new UILabel(gui, malisisText("from"), false).setPosition(0, 47);
        add(from);

        fromField = new UITextField(gui, String.valueOf(this.layer.fromY), false).setPosition(from.getWidth() + 10, 45).setSize(40, 5);
        add(fromField);

        separator = new UISeparator(gui, false).setColor(0x767676).setPosition(0, to.getY() + to.getHeight() + 3)
                .setSize(UIComponent.INHERITED, 1);
        super.add(separator);
    }

    protected void saveConfig() {
        this.gui.saveConfig();
    }

    protected void removeLayer() {
        this.flatLayersTab.remove(this);
    }

    protected void addLayer() {
        Layer newLayer = new Layer(this.layer.toY, this.layer.toY + 1, Blocks.SANDSTONE.getDefaultState());
        this.flatLayersTab.add(this, newLayer);
    }

    public int getLevelValueFromY() {
        int fromY = layer.fromY;
        try {
            fromY = Integer.parseInt(fromField.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (fromY < CubicChunks.MIN_BLOCK_Y) {
            fromY = CubicChunks.MIN_BLOCK_Y;
        } else if (fromY > CubicChunks.MAX_BLOCK_Y) {
            fromY = CubicChunks.MAX_BLOCK_Y;
        }
        return fromY;
    }

    public int getLevelValueToY() {
        int toY = layer.toY;
        try {
            toY = Integer.parseInt(toField.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (toY < CubicChunks.MIN_BLOCK_Y) {
            toY = CubicChunks.MIN_BLOCK_Y;
        } else if (toY > CubicChunks.MAX_BLOCK_Y + 1) {
            toY = CubicChunks.MAX_BLOCK_Y + 1;
        }
        return toY;
    }

    public void setBlockState(IBlockState blockStateTo) {
        this.layer.blockState = blockStateTo;
        blockButton.setBlockState(blockStateTo);
        blockName.setText(blockStateTo.getBlock().getLocalizedName());
    }
}
