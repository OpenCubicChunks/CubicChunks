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
package cubicchunks.client;

import cubicchunks.CommonEventHandler;
import cubicchunks.CubicChunks;
import cubicchunks.CubicChunks.Config.IntOptions;
import cubicchunks.event.CreateNewWorldEvent;
import cubicchunks.server.ICubicPlayerList;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldSettings;
import cubicchunks.world.type.ICubicWorldType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ClientEventHandler {

    @SubscribeEvent
    public void onWorldClientTickEvent(TickEvent.ClientTickEvent evt) {
        ICubicWorld world = (ICubicWorld) FMLClientHandler.instance().getWorldClient();
        //does the world exist? Is the game paused?
        if (world == null || Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        if (evt.phase == TickEvent.Phase.END && world.isCubicWorld()) {
            world.tickCubicWorld();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // no need to check side, this is only registered in client proxy
        ICubicPlayerList playerList = ((ICubicPlayerList)FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList());
        int prevDist = playerList.getVerticalViewDistance();
        int newDist = IntOptions.VERTICAL_CUBE_LOAD_DISTANCE.getValue();
        if (prevDist != newDist) {
            CubicChunks.LOGGER.info("Changing vertical view distance to {}, from {}", newDist, prevDist);
            playerList.setVerticalViewDistance(newDist);
        }
    }

    @SubscribeEvent
    public void initGuiEvent(InitGuiEvent.Post event) {

        GuiScreen currentGui = event.getGui();
        if (currentGui instanceof GuiVideoSettings) {
            GuiVideoSettings gvs = (GuiVideoSettings) currentGui;
            try {
                GuiOptionsRowList gowl = (GuiOptionsRowList) gvs.optionsRowList;
                GuiOptionsRowList.Row row = this.createRow(100, CubicChunks.Config.IntOptions.VERTICAL_CUBE_LOAD_DISTANCE, gvs.width);
                gowl.options.add(1, row);
            } catch (NoSuchFieldError err) {
                int idx = 3;
                int btnSpacing = 20;
                CubicChunks.LOGGER.error("Couldn't add vertical view distance options, maybe optifine is installed? Attempting optifine-specific "
                        + "option add", err.toString());
                gvs.buttonList.add(idx, new GuiCustomSlider(100, gvs.width / 2 - 155 + 160, gvs.height / 6 + btnSpacing * (idx / 2) - 12,
                        CubicChunks.Config.IntOptions.VERTICAL_CUBE_LOAD_DISTANCE));
                // reposition all buttons except the last 4 (last 3 and done)
                for (int i = 0; i < gvs.buttonList.size() - 4; i++) {
                    GuiButton btn = gvs.buttonList.get(i);
                    int x = gvs.width / 2 - 155 + i % 2 * 160;
                    int y = gvs.height / 6 + 21 * (i / 2) - 12;
                    btn.xPosition = x;
                    btn.yPosition = y;
                }
                // now position the last 3 buttons excluding "done" to be 3-in-a-row
                for (int i = gvs.buttonList.size() - 4; i < gvs.buttonList.size() - 1; i++) {
                    GuiButton btn = gvs.buttonList.get(i);

                    int newBtnWidth = 150 * 2 / 3;
                    int minX = gvs.width / 2 - 155;
                    int maxX = gvs.width / 2 - 155 + 160 + btn.width;

                    int minXCenter = minX + newBtnWidth / 2;
                    int maxXCenter = maxX - newBtnWidth / 2;

                    int x = minXCenter + (i % 3) * (maxXCenter - minXCenter) / 2 - newBtnWidth / 2;
                    int y = gvs.height / 6 + 21 * (gvs.buttonList.size() - 4) / 2 - 12;

                    btn.xPosition = x;
                    btn.yPosition = y;
                    btn.width = newBtnWidth;
                }
            }
        }
    }

    private GuiOptionsRowList.Row createRow(int buttonId, CubicChunks.Config.IntOptions option, int width) {
        GuiCustomSlider slider = new GuiCustomSlider(buttonId, width / 2 - 155 + 160, 0, option);
        return new GuiOptionsRowList.Row(slider, null);
    }


    private class GuiCustomSlider extends GuiButton {

        private float sliderValue;
        public boolean dragging;
        private final IntOptions option;

        public GuiCustomSlider(int buttonId, int x, int y, CubicChunks.Config.IntOptions optionIn) {
            super(buttonId, x, y, 150, 20, "");
            this.sliderValue = 1.0F;
            this.option = optionIn;
            this.sliderValue = optionIn.getNormalValue();
            this.displayString = this.createDisplayString(option);
        }

        /**
         * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering
         * over this button and 2 if it IS hovering over this button.
         */
        protected int getHoverState(boolean mouseOver) {
            return 0;
        }

        /**
         * Fired when the mouse button is dragged. Equivalent of
         * MouseListener.mouseDragged(MouseEvent e).
         */
        protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                if (this.dragging) {
                    this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                    this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
                    this.option.setValueFromNormal(this.sliderValue);
                    this.sliderValue = this.option.getNormalValue();
                    this.displayString = this.createDisplayString(option);
                }

                mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.drawTexturedModalRect(this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)), this.yPosition, 0, 66, 4, 20);
                this.drawTexturedModalRect(this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
            }
        }

        /**
         * Returns true if the mouse has been pressed on this control.
         * Equivalent of MouseListener.mousePressed(MouseEvent e).
         */
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (super.mousePressed(mc, mouseX, mouseY)) {
                this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
                this.option.setValueFromNormal(this.sliderValue);
                this.displayString = this.createDisplayString(option);
                this.dragging = true;
                return true;
            } else {
                return false;
            }
        }

        private String createDisplayString(IntOptions option2) {
            return I18n.format(CubicChunks.MODID + ".gui." + CubicChunks.Config.getNicelyFormattedName(option.name()), option.getValue());
        }

        /**
         * Fired when the mouse button is released. Equivalent of
         * MouseListener.mouseReleased(MouseEvent e).
         */
        public void mouseReleased(int mouseX, int mouseY) {
            this.dragging = false;
        }
    }

    @Mod.EventBusSubscriber(modid = CubicChunks.MODID,value = Side.CLIENT )
    public static class WorldSelectionCubicChunks {

        private static final int MAP_TYPE_ID = 5;
        private static final int ALLOW_CHEATS_ID = 6;
        private static final int CUSTOMIZE_ID = 8;
        private static final int MORE_WORLD_OPTIONS = 3;

        private static final int CC_ENABLE_BUTTON_ID = 11;

        @SubscribeEvent
        public static void guiInit(InitGuiEvent.Post event) {
            GuiScreen gui = event.getGui();
            if (isCreateWorldGui(gui)) {
                init((GuiCreateWorld) gui, event.getButtonList());
            }
        }

        private static void init(GuiCreateWorld gui, List<GuiButton> buttons) {
            if (getButton(buttons, CC_ENABLE_BUTTON_ID).isPresent()) {
                return;
            }
            GuiButton enableCC = new GuiButton(CC_ENABLE_BUTTON_ID, 0, 0, 20, 20, "enable");
            enableCC.visible = false;
            buttons.add(enableCC);
            Optional<GuiButton> customizeButton = getButton(buttons, CUSTOMIZE_ID);
            Optional<GuiButton> allowCheats = getButton(buttons, ALLOW_CHEATS_ID);
            customizeButton.ifPresent(b -> allowCheats.ifPresent(c -> {
                b.yPosition = c.yPosition - 21;
                GuiButton mapTypeButton = getButton(buttons, MAP_TYPE_ID).get();
                enableCC.xPosition = c.xPosition;
                enableCC.yPosition = b.yPosition;
                enableCC.width = c.width;
                enableCC.height = c.height;
                enableCC.visible = mapTypeButton.visible;

                refreshText(gui, enableCC);
            }));
        }
        private static void refreshText(GuiCreateWorld gui, GuiButton enableBtn) {
            enableBtn.displayString = I18n.format("cubicchunks.gui.worldmenu." +
                    (CubicChunks.Config.BoolOptions.FORCE_CUBIC_CHUNKS.getValue() ? "cc_enable" : "cc_disable"));
        }

        @SubscribeEvent
        public static void actionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
            GuiScreen gui = event.getGui();
            GuiButton button = event.getButton();
            if (isCreateWorldGui(gui)) {
                switch (button.id) {
                    case MORE_WORLD_OPTIONS: {
                        init((GuiCreateWorld) gui, event.getButtonList());
                        // fall through
                    }
                    case MAP_TYPE_ID: {
                        GuiButton enableCC = null, mapType = null;
                        for (GuiButton b : event.getButtonList()) {
                            if (b.id == CC_ENABLE_BUTTON_ID) {
                                enableCC = b;
                            } else if (b.id == MAP_TYPE_ID) {
                                mapType = b;
                            }
                        }
                        assert enableCC != null;
                        boolean isCubicChunksType = WorldType.WORLD_TYPES[((GuiCreateWorld) gui).selectedIndex] instanceof ICubicWorldType;
                        enableCC.visible = mapType != null && !isCubicChunksType && mapType.visible;
                        break;
                    }
                    case CC_ENABLE_BUTTON_ID: {
                        CubicChunks.Config.BoolOptions.FORCE_CUBIC_CHUNKS.flip();
                        refreshText((GuiCreateWorld) gui, button);
                        break;
                    }
                }
            }
        }

        @SubscribeEvent public static void onCreateWorldSettings(CreateNewWorldEvent event) {
            ((ICubicWorldSettings) (Object) event.settings).setCubic(CubicChunks.Config.BoolOptions.FORCE_CUBIC_CHUNKS.getValue());
        }

        private static boolean isCreateWorldGui(GuiScreen gui) {
            return gui instanceof GuiCreateWorld;
        }

        private static Optional<GuiButton> getButton(List<GuiButton> buttons, int id) {
            return buttons.stream().filter(b -> b.id == id).findFirst();
        }
    }
}
