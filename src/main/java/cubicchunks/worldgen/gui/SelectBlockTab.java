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

import com.google.common.base.Preconditions;
import cubicchunks.worldgen.gui.component.UIFlatTerrainLayer;

import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_PADDING;

import java.util.Collection;

import cubicchunks.worldgen.gui.component.UIBlockStateButton;
import cubicchunks.worldgen.gui.component.UIItemGrid;
import net.malisis.core.client.gui.component.UIComponent;
import net.minecraft.block.state.IBlockState;

/**
 * @deprecated This class is mixing general GUI stuff with FlatCubic GUI behavior. Either fix it or remove the class.
 */
@Deprecated()
public class SelectBlockTab {

    private final UIItemGrid container;
    private final ExtraGui gui;
    public UIFlatTerrainLayer layer;

    SelectBlockTab(ExtraGui guiFor, UIFlatTerrainLayer layerFor, Collection<IBlockState> blockStates, ClickAction clickAction) {
        Preconditions.checkNotNull(clickAction);
        this.gui = guiFor;
        layer = layerFor;
        UIItemGrid layout = new UIItemGrid(gui, layerFor);
        layout.setSize(UIComponent.INHERITED, UIComponent.INHERITED);
        layout.setPadding(HORIZONTAL_PADDING, 0);
        blockStates.forEach(blockState -> {
            Runnable action;
            switch (clickAction) {
                case OPEN_CHILD: {
                    action = () -> {
                        new SelectBlockGui(this.layer, blockState.getBlock()).display();
                    };
                    break;
                }
                case SET_STATE_AND_OPEN_PARENT: {
                    action = () -> {
                        this.layer.setBlockState(blockState);
                        this.layer.getGui().display();
                    };
                    break;
                }
                default:
                    throw new Error("Unknown ClickAction enum value " + clickAction);
            }
            UIBlockStateButton uiButton = new UIBlockStateButton(gui, blockState).onClick(btn -> action.run());
            layout.add(uiButton);
        });
        this.container = layout;
    }

    UIItemGrid getContainer() {
        return container;
    }

    public enum ClickAction {
        OPEN_CHILD, SET_STATE_AND_OPEN_PARENT
    }
}
