package cubicchunks.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldServer;
import cubicchunks.client.WorldClientContext;
import cubicchunks.server.WorldServerContext;


public class WorldContexts {
	
	public static WorldContext get(World world) {
		// TODO: hide client things from the server
		if (world instanceof WorldClient) {
			return WorldClientContext.get((WorldClient)world);
		} else if (world instanceof WorldServer) {
			return WorldServerContext.get((WorldServer)world);
		}
		throw new Error("Unknown world type!");
	}
}
