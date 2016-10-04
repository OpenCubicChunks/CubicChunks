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
package cubicchunks.testutil;

import cubicchunks.CubicChunks;
import net.minecraft.init.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import org.apache.logging.log4j.LogManager;

import java.util.Hashtable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


//A few hacks to make tests possible
public class MinecraftEnvironment {
	private static boolean isInit = false;
	/**
	 * Does whatever is needed to initialize minecraft and mod environment
	 */
	public static void init() {
		if(isInit) {
			return;
		}
		isInit = true;
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
