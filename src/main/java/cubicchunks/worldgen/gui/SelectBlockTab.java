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

import cubicchunks.worldgen.gui.component.UIFlatTerrainLayer;

import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_PADDING;

import java.util.Collection;

import cubicchunks.worldgen.gui.component.UIBlockStateButton;
import cubicchunks.worldgen.gui.component.UIItemGrid;
import net.malisis.core.client.gui.component.UIComponent;
import net.minecraft.block.state.IBlockState;

public class SelectBlockTab {

    private final UIItemGrid container;
    private final ExtraGui gui;
    public UIFlatTerrainLayer layer;

    SelectBlockTab(ExtraGui guiFor, UIFlatTerrainLayer layerFor, Collection<IBlockState> blockStates, String clickAction) {
        this.gui = guiFor;
        layer = layerFor;
        UIItemGrid layout = new UIItemGrid(gui, layerFor, 24);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        blockStates.forEach(blockState -> {
            Runnable action = null;
            if (clickAction.equalsIgnoreCase("openChild")) {
                action = () -> {
                    new SelectBlockGui(this.layer, blockState.getBlock()).display();
                };
            } else if (clickAction.equalsIgnoreCase("setBlockStateAndOpenParent")) {
                action = () -> {
                    this.layer.setBlockState(blockState);
                    this.layer.getGui().display();
                };
            } else if (clickAction.equalsIgnoreCase("launchSelectBlockGui")) {
                action = () -> {
                    new SelectBlockGui(this.layer, null).display();
                };
            }
            UIBlockStateButton uiButton = new UIBlockStateButton(gui, blockState, action);
            layout.add(uiButton);
        });
        layout.init();
        this.container = layout;
    }

    UIItemGrid getContainer() {
        return container;
    }
}
