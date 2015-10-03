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

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CCFmlEventHandler {

	private final CubicChunkSystem ccSystem;

	public CCFmlEventHandler(CubicChunkSystem ccSystem) {
		this.ccSystem = ccSystem;
	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent evt) {
		System.out.println(evt.world.getClass() + ", phase=" + evt.phase + ", side=" + evt.side + ", type=" + evt.type);
		World world = evt.world;
		if (evt.phase == TickEvent.Phase.END && ccSystem.isTallWorld(world)) {
			switch (evt.side) {
				case CLIENT:
					ccSystem.onWorldClientTick((WorldClient) world);
					break;
				case SERVER:
					ccSystem.onWorldServerTick((WorldServer) world);
					break;
			}
		}
	}
}
