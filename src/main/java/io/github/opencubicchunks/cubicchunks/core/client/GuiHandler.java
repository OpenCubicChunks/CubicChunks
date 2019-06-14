/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.CompatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final SliderPercentageOption VERT_RENDER_DISTANCE = new SliderPercentageOption(
        "cubicchunks.options.vertRenderDistance", // lang key
        AbstractOption.RENDER_DISTANCE.func_216732_b(), // minValue, func_216732_b == getMinValue
        AbstractOption.RENDER_DISTANCE.func_216733_c(), // maxValue, func_216733_c == getMaxValue
        1.0F, // increment
        (gameSettings) -> { // getRawValue
            return (double) CubicChunksConfig.CLIENT.verticalRenderDistance.get();
        },
        (gameSettings, value) -> { // setRawValue
            CubicChunksConfig.CLIENT.verticalRenderDistance.set(value.intValue());
            Minecraft.getInstance().worldRenderer.setDisplayListEntitiesDirty();
        },
        (gameSettings, option) -> { // getText
            double distance = CubicChunksConfig.CLIENT.verticalRenderDistance.get();
            return option.func_216617_a() + I18n.format("options.chunks", (int) distance);
        });

    public static void handleGui(GuiScreenEvent.InitGuiEvent.Post event) {
        Screen currentScreen = event.getGui();
        if (currentScreen instanceof VideoSettingsScreen) {
            handleVideoSettings((VideoSettingsScreen) currentScreen);
        } else if (currentScreen instanceof CreateWorldScreen) {
            handleCreateWorld((CreateWorldScreen) currentScreen);
        }
    }

    private static void handleVideoSettings(VideoSettingsScreen videoSettings) {
        if (!CompatUtil.hasOptifine()) {
            // func_214384_a == create
            OptionsRowList.Row row = OptionsRowList.Row.func_214384_a(Minecraft.getInstance().gameSettings, videoSettings.width, VERT_RENDER_DISTANCE);
            videoSettings.optionsRowList.children().add(1, row);
        } else {
            LOGGER.error("Found OptiFine, OptiFine video settings GUI support not implemented.");
            /*
            int idx = 3;
            int btnSpacing = 20;
            videoSettings.buttonList.add(idx,
                new VertViewDistanceSlider(100, videoSettings.width / 2 - 155 + 160, videoSettings.height / 6 + btnSpacing * (idx / 2) - 12));
            // reposition all buttons except the last 4 (last 3 and done)
            for (int i = 0; i < videoSettings.buttonList.size() - 4; i++) {
                GuiButton btn = videoSettings.buttonList.get(i);
                int x = videoSettings.width / 2 - 155 + i % 2 * 160;
                int y = videoSettings.height / 6 + 21 * (i / 2) - 12;
                btn.x = x;
                btn.y = y;
            }
            // now position the last 3 buttons excluding "done" to be 3-in-a-row
            for (int i = videoSettings.buttonList.size() - 4; i < videoSettings.buttonList.size() - 1; i++) {
                GuiButton btn = videoSettings.buttonList.get(i);

                int newBtnWidth = 150 * 2 / 3;
                int minX = videoSettings.width / 2 - 155;
                int maxX = videoSettings.width / 2 - 155 + 160 + btn.width;

                int minXCenter = minX + newBtnWidth / 2;
                int maxXCenter = maxX - newBtnWidth / 2;

                int x = minXCenter + (i % 3) * (maxXCenter - minXCenter) / 2 - newBtnWidth / 2;
                int y = videoSettings.height / 6 + 21 * (videoSettings.buttonList.size() - 4) / 2 - 12;

                btn.x = x;
                btn.y = y;
                btn.width = newBtnWidth;
            }
             */
        }
    }

    private static void handleCreateWorld(CreateWorldScreen createWorld) {
        // move customize button Y to the same Y as the CC enable button will be for alignment
        createWorld.btnCustomizeType.y = createWorld.btnAllowCommands.y - 21;
        Button enableCC = new Button(
            createWorld.btnAllowCommands.x, // above btnAllowCommands
            createWorld.btnCustomizeType.y, // next to customize
            150, 20, // size, same as other buttons
            I18n.format(CubicChunksConfig.SERVER.forceCCMode.get().translationKey()),
            btn -> CubicChunksConfig.SERVER.forceCCMode.set(CubicChunksConfig.SERVER.forceCCMode.get().flip())) {
            @Override public void render(int arg1, int arg2, float arg3) {
                this.visible = createWorld.btnMapType.visible && !(WorldType.WORLD_TYPES[createWorld.selectedIndex] instanceof ICubicWorldType);
                super.render(arg1, arg2, arg3);
            }
        };
        createWorld.addButton(enableCC);
    }
}
