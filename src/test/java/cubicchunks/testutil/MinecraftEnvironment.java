package cubicchunks.testutil;

import cubicchunks.CubicChunks;
import net.minecraft.init.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import org.apache.logging.log4j.LogManager;

import java.util.Hashtable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MinecraftEnvironment {
	/**
	 * Does whatever is needed to initialize minecraft and mod environment
	 */
	public static void init() {
		Bootstrap.register();
		CubicChunks.LOGGER = LogManager.getLogger();
	}

	/**
	 * Creates a fake server
	 */
	public static MinecraftServer createFakeServer() {
		PlayerList playerList = mock(PlayerList.class);
		MinecraftServer server = mock(MinecraftServer.class);
		when(server.getPlayerList()).thenReturn(playerList);

		server.worldTickTimes =new Hashtable<>();
		return server;
	}
}
