/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

@Mod(
	modid = TallWorldsMod.MODID,
	name = "CubicChunks",
	version = "1.8.11-0.5"
)
public class TallWorldsMod {
	
	public static final String MODID = "cubicchunks";
	public static Logger LOGGER;
	
	@Instance(value = MODID)
	public static TallWorldsMod instance;
	
	public static WorldType CC_WORLD_TYPE;
	private CubicChunkSystem ccSystem;
	private CCEventHandler evtHandler;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		LOGGER = e.getModLog();
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
		
		LOGGER.info("Initializing Tall Worlds..."); 
		
		this.ccSystem = new CubicChunkSystem();
		
		CC_WORLD_TYPE = new CubicChunksWorldType(ccSystem);
		this.evtHandler = new CCEventHandler(ccSystem);
		
		MinecraftForge.EVENT_BUS.register(evtHandler);
		//TODO: Port it to forge
		/*
		ConnectionState.PLAY.registerPacket(PacketDirection.CLIENTBOUND, PacketBulkCubeData.class);
		ConnectionState.directionMaps.put(PacketBulkCubeData.class, ConnectionState.PLAY);
		ConnectionState.PLAY.registerPacket(PacketDirection.CLIENTBOUND, PacketUnloadCubes.class);
		ConnectionState.directionMaps.put(PacketUnloadCubes.class, ConnectionState.PLAY);
		ConnectionState.PLAY.registerPacket(PacketDirection.CLIENTBOUND, PacketUnloadColumns.class);
		ConnectionState.directionMaps.put(PacketUnloadColumns.class, ConnectionState.PLAY);
		ConnectionState.PLAY.registerPacket(PacketDirection.CLIENTBOUND, PacketCubeBlockChange.class);
		ConnectionState.directionMaps.put(PacketCubeBlockChange.class, ConnectionState.PLAY);
		ConnectionState.PLAY.registerPacket(PacketDirection.CLIENTBOUND, PacketCubeChange.class);
		ConnectionState.directionMaps.put(PacketCubeChange.class, ConnectionState.PLAY);
		*/
	}
}
