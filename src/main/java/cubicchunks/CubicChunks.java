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
package cubicchunks;

import cubicchunks.debug.DebugWorldType;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.proxy.CommonProxy;
import cubicchunks.util.AddressTools;
import cubicchunks.util.ReflectionUtil;
import cubicchunks.world.CubicWorldProviderSurface;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import static cubicchunks.CubicChunks.Config.syncConfig;

@Mod(modid = CubicChunks.MODID, name = "CubicChunks", version = "@@VERSION@@}", guiFactory = "cubicchunks.client.GuiFactory")
public class CubicChunks {

	public static Logger LOGGER;

	public static final String MODID = "cubicchunks";

	@Instance(value = MODID)
	public static CubicChunks instance;

	@SidedProxy(clientSide = "cubicchunks.proxy.ClientProxy", serverSide = "cubicchunks.proxy.ServerProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		LOGGER = e.getModLog();
		//Set custom WorldProvider for Overworld
		//for vanilla world this world provider will work the same way as vanilla world provider

		//set "clazz" field
		ReflectionUtil.setFieldValueSrg(DimensionType.OVERWORLD, "field_186077_g", CubicWorldProviderSurface.class);

		Config.loadConfig(new Configuration(e.getSuggestedConfigurationFile()));
		syncConfig();
		MinecraftForge.EVENT_BUS.register(this); // Register our config reload hook
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.registerEvents();

		VanillaCubicChunksWorldType.create();
		FlatCubicChunksWorldType.create();
		CustomCubicChunksWorldType.create();
		DebugWorldType.create();
		LOGGER.debug("Registered world types");

		PacketDispatcher.registerPackets();
	}

	@EventHandler
	public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
		proxy.setBuildLimit(event.getServer());
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if(eventArgs.getModID().equals(CubicChunks.MODID))
			Config.syncConfig();
	}

	public static class Config {
		private static int maxGeneratedCubesPerTick;
		private static int lightingTickBudget;
		private static int verticalCubeLoadDistance;
		private static int worldHeightLowerBound;
		private static int worldHeightUpperBound;
		private static Configuration configuration;

		static void loadConfig(Configuration configuration) {
			Config.configuration = configuration;
		}

		static void syncConfig() {
			maxGeneratedCubesPerTick = configuration.getInt("maxGeneratedCubesPerTick", Configuration.CATEGORY_GENERAL,
					49*16, 1, Integer.MAX_VALUE, "The number of cubic chunks to generate per tick.");
			lightingTickBudget = configuration.getInt("lightingTickBudget", Configuration.CATEGORY_GENERAL, 10, 1, Integer.MAX_VALUE, "The maximum amount of time in milliseconds per tick to spend performing lighting calculations.");
			verticalCubeLoadDistance = configuration.getInt("verticalCubeLoadDistance", Configuration.CATEGORY_GENERAL, 8, 2, 32, "Similar to Minecraft's view distance, only for vertical chunks.");
			worldHeightLowerBound = configuration.getInt("worldHeightLowerBound", Configuration.CATEGORY_GENERAL, -4096, AddressTools.MIN_BLOCK_Y, 0, "The lower boundary on the world. Blocks will not generate or load below this point.");
			worldHeightUpperBound = configuration.getInt("worldHeightUpperBound", Configuration.CATEGORY_GENERAL, 4096, 256, AddressTools.MAX_BLOCK_Y, "The upper boundary on the world. Blocks will not generate or load above this point.");

			if (configuration.hasChanged()) configuration.save();
		}

		public static class GUI extends GuiConfig {
			public GUI(GuiScreen parent) {
				super(parent, new ConfigElement(configuration.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), "CubicChunks", false, false, GuiConfig.getAbridgedConfigPath(configuration.toString()));
			}
		}

		public static int getMaxGeneratedCubesPerTick() {
			return maxGeneratedCubesPerTick;
		}

		public static int getLightingTickBudget() {
			return lightingTickBudget;
		}

		public static int getVerticalCubeLoadDistance() {
			return verticalCubeLoadDistance;
		}

		public static int getWorldHeightLowerBound() {
			return worldHeightLowerBound;
		}

		public static int getWorldHeightUpperBound() {
			return worldHeightUpperBound;
		}
	}
}
