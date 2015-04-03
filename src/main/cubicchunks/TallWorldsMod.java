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
	
	/*
	public void handleEvent(BuildSizeEvent event) {
		event.setCustomBuildHeight(Coords.cubeToMaxBlock(AddressTools.MaxY));
		event.setCustomBuildDepth(Coords.cubeToMinBlock(AddressTools.MinY));
		
		log.info(String.format("Set build height to [%d,%d]", event.getCustomBuildDepth(), event.getCustomBuildHeight()));
	}
	
	public void handleEvent(EntityPlayerMPUpdateEvent event) {
		EntityPlayerMP player = event.getPlayer();
		WorldServer world = (WorldServer)player.theItemInWorldManager.theWorld;
		CubePlayerManager playerManager = (CubePlayerManager)world.getPlayerManager();
		playerManager.onPlayerUpdate(player);
	}
	
	public void handleEvent(UpdateRenderPositionEvent event) {
		// block position is the position of the viewpoint entity
		int blockX = event.getBlockX();
		int blockY = event.getBlockY();
		int blockZ = event.getBlockZ();
		
		// move back 8 blocks?? (why?)
		blockX -= 8;
		blockY -= 8;
		blockZ -= 8;
		
		int minBlockX = Integer.MAX_VALUE;
		int minBlockY = Integer.MAX_VALUE;
		int minBlockZ = Integer.MAX_VALUE;
		int maxBlockX = Integer.MIN_VALUE;
		int maxBlockY = Integer.MIN_VALUE;
		int maxBlockZ = Integer.MIN_VALUE;
		
		// get view dimensions
		int blockViewDx = event.getRenderCubeDx() * 16;
		int blockViewHdx = blockViewDx / 2;
		int blockViewDy = event.getRenderCubeDy() * 16;
		int blockViewHdy = blockViewDy / 2;
		int blockViewDz = event.getRenderCubeDz() * 16;
		int blockViewHdz = blockViewDz / 2;
		
		for (int renderX = 0; renderX < event.getRenderCubeDx(); renderX++) {
			int posBlockX = renderX * 16;
			
			// compute parameter of coordinate transformation
			int blockWidthsX = posBlockX + blockViewHdx - blockX;
			if (blockWidthsX < 0) {
				blockWidthsX -= blockViewDx - 1;
			}
			blockWidthsX /= blockViewDx;
			
			// translate by player position
			posBlockX -= blockWidthsX * blockViewDx;
			
			// update bounds
			if (posBlockX < minBlockX) {
				minBlockX = posBlockX;
			}
			if (posBlockX > maxBlockX) {
				maxBlockX = posBlockX;
			}
			
			for (int renderZ = 0; renderZ < event.getRenderCubeDz(); renderZ++) {
				int posBlockZ = renderZ * 16;
				
				// compute parameter of coordinate transformation
				int blockWidthsZ = posBlockZ + blockViewHdz - blockZ;
				if (blockWidthsZ < 0) {
					blockWidthsZ -= blockViewDz - 1;
				}
				blockWidthsZ /= blockViewDz;
				
				// translate by player position
				posBlockZ -= blockWidthsZ * blockViewDz;
				
				// update bounds
				if (posBlockZ < minBlockZ) {
					minBlockZ = posBlockZ;
				}
				if (posBlockZ > maxBlockZ) {
					maxBlockZ = posBlockZ;
				}
				
				for (int renderY = 0; renderY < event.getRenderCubeDy(); renderY++) {
					int posBlockY = renderY * 16;
					
					// compute parameter of coordinate transformation
					int blockHeightsY = posBlockY + blockViewHdy - blockY;
					if (blockHeightsY < 0) {
						blockHeightsY -= blockViewDy - 1;
					}
					blockHeightsY /= blockViewDy;
					
					// translate by player position
					posBlockY -= blockHeightsY * blockViewDy;
					
					// update bounds
					if (posBlockY < minBlockY) {
						minBlockY = posBlockY;
					}
					if (posBlockY > maxBlockY) {
						maxBlockY = posBlockY;
					}
					
					// update renderer
					WorldRenderer renderer = event.getRenderer(renderX, renderY, renderZ);
					boolean neededUpdate = renderer.needsUpdate;
					renderer.setPosition(posBlockX, posBlockY, posBlockZ);
					if (!neededUpdate && renderer.needsUpdate) {
						event.updateRenderer(renderer);
					}
				}
			}
		}
		
		// save the bounds to the event
		event.setMinBlockX(minBlockX);
		event.setMinBlockY(minBlockY);
		event.setMinBlockZ(minBlockZ);
		event.setMaxBlockX(maxBlockX);
		event.setMaxBlockY(maxBlockY);
		event.setMaxBlockZ(maxBlockZ);
		
		event.setHandled();
	}
	
	public void handleEvent(VoidFogRangeEvent event) {
		int min = Coords.cubeToMinBlock(AddressTools.MinY);
		event.setCustomRange(min, min + 1024);
	}
	
	public void handleEvent(CheckChunksExistForEntityEvent event) {
		Entity entity = event.getEntity();
		int entityX = MathHelper.floor_double(entity.posX);
		int entityY = MathHelper.floor_double(entity.posY);
		int entityZ = MathHelper.floor_double(entity.posZ);
		
		event.setChunksExist(entity.worldObj.checkChunksExist(entityX - 32, entityY - 32, entityZ - 32, entityX + 32, entityY + 32, entityZ + 32));
	}
	*/
}
