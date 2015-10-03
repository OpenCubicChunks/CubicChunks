
package cubicchunks.util;

import java.lang.reflect.Field;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;

public class WorldClientAccess {
	private static final Field wc_clientChunkProvider = ReflectionUtil.findFieldNonStatic(WorldClient.class, ChunkProviderClient.class);
	
	static {
		wc_clientChunkProvider.setAccessible(true);
	}
	
	public static final void setChunkProviderClient(WorldClient wc, ChunkProviderClient cpc) {
		ReflectionUtil.set(wc, wc_clientChunkProvider, cpc);
	}
}
