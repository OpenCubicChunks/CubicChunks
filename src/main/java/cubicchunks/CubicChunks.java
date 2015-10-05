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

import cubicchunks.asm.WorldHeightAccess;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.proxy.CommonProxy;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = CubicChunks.MODID, name = "CubicChunks", version = "${version}")
public class CubicChunks {

	public static Logger LOGGER;

	public static final String MODID = "cubicchunks";

	@Instance(value = MODID)
	public static CubicChunks instance;

	@SidedProxy(clientSide="cubicchunks.proxy.ClientProxy", serverSide="cubicchunks.proxy.CommonProxy")
	public static CommonProxy proxy;
	public static WorldType CC_WORLD_TYPE;

	private CubicChunkSystem ccSystem;
	private CCEventHandler evtHandler;
	private CCFmlEventHandler fmlEvtHandler;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		LOGGER = e.getModLog();
		this.ccSystem = new CubicChunkSystem();
		WorldHeightAccess.registerChunkSystem(ccSystem);
		PacketDispatcher.registerPackets();
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) {

		
		CC_WORLD_TYPE = new CubicChunksWorldType(ccSystem);
		this.evtHandler = new CCEventHandler(ccSystem);
		this.fmlEvtHandler = new CCFmlEventHandler(ccSystem);
		
		MinecraftForge.EVENT_BUS.register(this.evtHandler);
		FMLCommonHandler.instance().bus().register(this.fmlEvtHandler);


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
