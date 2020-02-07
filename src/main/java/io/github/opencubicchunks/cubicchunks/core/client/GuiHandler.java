/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2020 OpenCubicChunks
 *  Copyright (c) 2015-2020 contributors
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

import static java.lang.Math.floorMod;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig.ForceCCMode;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.access.CreateWorldScreenAccess;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.access.VideoSettingsScreenAccess;
import io.github.opencubicchunks.cubicchunks.core.util.CompatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final SliderPercentageOption VERT_RENDER_DISTANCE = new SliderPercentageOption(
        "cubicchunks.options.vertRenderDistance", // lang key
        AbstractOption.RENDER_DISTANCE.getMinValue(),
        AbstractOption.RENDER_DISTANCE.getMaxValue(),
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
            return option.getDisplayString() + I18n.format("options.chunks", (int) distance);
        });

    private static final IteratableOption FORCE_CUBIC_WORLD_OPTION = new IteratableOption(
        "cubicchunks.options.forceCCWorld",
        (gameSettings, delta) -> {
            int newId = floorMod(CubicChunksConfig.SERVER.forceCCMode.get().ordinal() + delta, ForceCCMode.values().length);
            CubicChunksConfig.SERVER.forceCCMode.set(ForceCCMode.values()[newId]);
        },
        (gameSettings, option) -> option.getDisplayString() + I18n.format(CubicChunksConfig.SERVER.forceCCMode.get().translationKey())
    );

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
            OptionsRowList.Row row = OptionsRowList.Row.create(Minecraft.getInstance().gameSettings, videoSettings.width, VERT_RENDER_DISTANCE);
            ((VideoSettingsScreenAccess) videoSettings).getOptionsRowList().children().add(1, row);
        } else {
            LOGGER.error("Found OptiFine, OptiFine video settings GUI support not implemented.");
        }
    }

    private static void handleCreateWorld(CreateWorldScreen createWorld) {
        CreateWorldScreenAccess createWorldAccess = (CreateWorldScreenAccess) createWorld;
        // move customize button Y to the same Y as the CC enable button will be for alignment
        createWorldAccess.getBtnCustomizeType().y = createWorldAccess.getBtnAllowCommands().y - 21;
        Widget enableCC = FORCE_CUBIC_WORLD_OPTION.createWidget(Minecraft.getInstance().gameSettings,
                createWorldAccess.getBtnAllowCommands().x, createWorldAccess.getBtnCustomizeType().y, 150);
        Widget fakeWidget = new Widget(0, 0, createWorld.width, createWorld.height, "") {
            @Override public void render(int arg1, int arg2, float arg3) {
                enableCC.visible = createWorldAccess.getBtnMapType().visible
                        && !(WorldType.WORLD_TYPES[createWorldAccess.getSelectedIndex()] instanceof ICubicWorldType);
            }
        };
        fakeWidget.active = false;
        createWorldAccess.invokeAddButton(fakeWidget);
        createWorldAccess.invokeAddButton(enableCC);
    }
}
