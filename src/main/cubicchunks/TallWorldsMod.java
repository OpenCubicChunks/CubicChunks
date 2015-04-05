/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cubicchunks.client.ClientCubeCache;
import cubicchunks.server.CubeIO;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.world.column.Column;
import cuchaz.m3l.M3L;
import cuchaz.m3l.api.registry.AlreadyRegisteredException;

@Mod(
	modid = TallWorldsMod.Id,
	name = "Tall Worlds",
	version = "1.8.3-0.1"
)
public class TallWorldsMod {
	
	public static final String Id = "tallworlds";
	public static final Logger log = LoggerFactory.getLogger(Id);
	
	@Mod.Instance(Id)
	public static TallWorldsMod instance;
	
	private CubicChunkSystem m_system;
	
	public CubicChunkSystem getSystem() {
		// TODO: maybe this could be named better...
		return m_system;
	}
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		
		// HACKHACK: tweak the class load order so the runtime obfuscator works correctly
		// load subclasses of Minecraft classes first, so the runtime obfuscator can record
		// the superclass information before any other classes need it
		// TODO: make a way to explicitly record superclass info without needing to change load order
		Column.class.getName();
		CubePlayerManager.class.getName();
		ServerCubeCache.class.getName();
		ClientCubeCache.class.getName();
		CubeIO.class.getName();
		
		// register our chunk system
		m_system = new CubicChunkSystem();
		try {
			M3L.instance.getRegistry().chunkSystem.register(m_system);
		} catch (AlreadyRegisteredException ex) {
			log.error("Cannot register cubic chunk system. Someone else beat us to it. =(", ex);
		}
	}
}
