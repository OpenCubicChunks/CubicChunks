package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event fired from client thread every time opacity of a top block
 * changed <b>before</b> any change is performed.
 *
 * This is not an {@link #isCancelable()} event. This event is fired on the
 * {@link MinecraftForge#EVENT_BUS}.
 */
public class ClientHeightMapUpdateEvent extends Event {

	private final ClientHeightMap heightMap;
	private final BlockPos pos;
	private final int newOpacity;

	public ClientHeightMapUpdateEvent(ClientHeightMap heightMapIn, BlockPos posIn, int newOpacityIn) {
		heightMap = heightMapIn;
		pos = posIn;
		newOpacity = newOpacityIn;
	}
	
	public BlockPos getPos(){
		return pos;
	}
	
	public ClientHeightMap getHeightMap(){
		return heightMap;
	}
	
	public int getNewOpacity(){
		return newOpacity;
	}
}
