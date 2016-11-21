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

import net.minecraft.client.gui.GuiScreen;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import cubicchunks.debug.DebugTools;
import cubicchunks.debug.DebugWorldType;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.proxy.CommonProxy;
import cubicchunks.server.chunkio.async.forge.AsyncWorldIOExecutor;
import cubicchunks.util.AddressTools;
import cubicchunks.world.type.CustomCubicWorldType;
import cubicchunks.world.type.FlatCubicWorldType;
import cubicchunks.world.type.VanillaCubicWorldType;

@Mod(modid = CubicChunks.MODID,
     name = "CubicChunks",
     version = "@@VERSION@@",
     guiFactory = "cubicchunks.client.GuiFactory")
public class CubicChunks {

	public static final boolean DEBUG_ENABLED = System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
	public static final String MODID = "cubicchunks";
	public static Logger LOGGER = LogManager.getLogger("EarlyCubicChunks");//use some logger even before it's set. useful for unit tests
	@Instance(value = MODID)
	public static CubicChunks instance;
	@SidedProxy(clientSide = "cubicchunks.proxy.ClientProxy", serverSide = "cubicchunks.proxy.ServerProxy")
	public static CommonProxy proxy;
	private static Config config;
	private static Set<IConfigUpdateListener> configChangeListeners = Collections.newSetFromMap(new WeakHashMap<>());

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		LOGGER = e.getModLog();

		config = new Config(new Configuration(e.getSuggestedConfigurationFile()));
		MinecraftForge.EVENT_BUS.register(this); // Register our config reload hook
		AsyncWorldIOExecutor.registerListeners();

		if (DEBUG_ENABLED) {
			DebugTools.init();
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.registerEvents();

		VanillaCubicWorldType.create();
		FlatCubicWorldType.create();
		CustomCubicWorldType.create();
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
		if (eventArgs.getModID().equals(CubicChunks.MODID)) {
			config.syncConfig();
			for (IConfigUpdateListener l : configChangeListeners) {
				l.onConfigUpdate(config);
			}
		}
	}

	public static void addConfigChangeListener(IConfigUpdateListener listener) {
		configChangeListeners.add(listener);
		//notify if the config is already there
		if (config != null) {
			listener.onConfigUpdate(config);
		}
	}

	public static class Config {
		public static final int DEFAULT_MAX_GENERATED_CUBES_PER_TICK = 49*16;
		public static final int DEFAULT_LIGHTING_TICK_BUDGET = 10;
		public static final int DEFAULT_VERTICAL_CUBE_LOAD_DISTANCE = 8;
		public static final int DEFAULT_MIN_WORLD_HEIGHT = -4096;
		public static final int DEFAULT_MAX_WORLD_HEIGHT = 4096;
		private int maxGeneratedCubesPerTick;
		private int lightingTickBudget;
		private int verticalCubeLoadDistance;
		private int worldHeightLowerBound;
		private int worldHeightUpperBound;
		private Configuration configuration;

		private Config(Configuration configuration) {
			loadConfig(configuration);
			syncConfig();
		}

		void loadConfig(Configuration configuration) {
			this.configuration = configuration;
		}

		void syncConfig() {
			maxGeneratedCubesPerTick = configuration.getInt("maxGeneratedCubesPerTick", Configuration.CATEGORY_GENERAL,
				DEFAULT_MAX_GENERATED_CUBES_PER_TICK, 1, Integer.MAX_VALUE, "The number of cubic chunks to generate per tick.");
			lightingTickBudget = configuration.getInt("lightingTickBudget", Configuration.CATEGORY_GENERAL,
				DEFAULT_LIGHTING_TICK_BUDGET, 1, Integer.MAX_VALUE, "The maximum amount of time in milliseconds per tick to spend performing lighting calculations.");
			verticalCubeLoadDistance = configuration.getInt("verticalCubeLoadDistance", Configuration.CATEGORY_GENERAL,
				DEFAULT_VERTICAL_CUBE_LOAD_DISTANCE, 2, 32, "Similar to Minecraft's view distance, only for vertical chunks.");
			worldHeightLowerBound = configuration.getInt("worldHeightLowerBound", Configuration.CATEGORY_GENERAL,
				DEFAULT_MIN_WORLD_HEIGHT, AddressTools.MIN_BLOCK_Y, 0, "The lower boundary on the world. Blocks will not generate or load below this point.");
			worldHeightUpperBound = configuration.getInt("worldHeightUpperBound", Configuration.CATEGORY_GENERAL,
				DEFAULT_MAX_WORLD_HEIGHT, 256, AddressTools.MAX_BLOCK_Y, "The upper boundary on the world. Blocks will not generate or load above this point.");

			if (configuration.hasChanged()) configuration.save();
		}

		public int getMaxGeneratedCubesPerTick() {
			return maxGeneratedCubesPerTick;
		}

		public int getLightingTickBudget() {
			return lightingTickBudget;
		}

		public int getVerticalCubeLoadDistance() {
			return verticalCubeLoadDistance;
		}

		public int getWorldHeightLowerBound() {
			return worldHeightLowerBound;
		}

		public int getWorldHeightUpperBound() {
			return worldHeightUpperBound;
		}

		public static class GUI extends GuiConfig {
			public GUI(GuiScreen parent) {
				super(parent, new ConfigElement(config.configuration.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), MODID, false, false, GuiConfig.getAbridgedConfigPath(config.configuration.toString()));
			}
		}
	}
}
