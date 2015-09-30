/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import cubicchunks.client.ClientCubeCache;
import cubicchunks.network.PacketBulkCubeData;
import cubicchunks.network.PacketCubeBlockChange;
import cubicchunks.network.PacketCubeChange;
import cubicchunks.network.PacketUnloadColumns;
import cubicchunks.network.PacketUnloadCubes;
import cubicchunks.server.CubeIO;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.world.column.Column;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
	modid = TallWorldsMod.Id,
	name = "Tall Worlds",
	version = "1.8.3-0.3.1"
)
public class TallWorldsMod {
	
	public static final String Id = "tallworlds";
	public static Logger LOGGER;
	
	@Mod.Instance(Id)
	public static TallWorldsMod instance;
	
	private CubicChunkSystem m_system;
	
	public CubicChunkSystem getSystem() {
		// TODO: maybe this could be named better...
		return m_system;
	}
        
        public void preInit(FMLPreInitializationEvent e) {
            LOGGER = e.getModLog();
        }
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		
		LOGGER.info("Initializing Tall Worlds...");
		
		// HACKHACK: tweak the class load order so the runtime obfuscator works correctly
		// load subclasses of Minecraft classes first, so the runtime obfuscator can record
		// the superclass information before any other classes need it
		// TODO: make a way to explicitly record superclass info without needing to change load order
		Column.class.getName();
		CubePlayerManager.class.getName();
		ServerCubeCache.class.getName();
		
		// client only loads
		CodeAnnotation.startClientOnly();
		ClientCubeCache.class.getName();
		CodeAnnotation.stopClientOnly();
		
		CubeIO.class.getName();
		
		// register our chunk system
		m_system = new CubicChunkSystem();
		try {
			M3L.instance.getRegistry().chunkSystem.register(m_system);
		} catch (AlreadyRegisteredException ex) {
			LOGGER.error("Cannot register cubic chunk system. Someone else beat us to it. =(", ex);
		}
		
		// register our packets
		// I'm not even sure this is used in standalone mode...
		// TODO: get a real M3L networking system
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
	}
}
