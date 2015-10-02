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
package cubicchunks.network;

import java.io.IOException;
import java.util.Collection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;
import net.minecraft.network.Packet;


public class PacketCubeBlockChange implements Packet {

	public long cubeAddress;
	public int[] localAddresses;
	public IBlockState[] blockStates;
	
	public PacketCubeBlockChange(Cube cube, Collection<Integer> localAddresses) {
		this.cubeAddress = cube.getAddress();
		this.localAddresses = new int[localAddresses.size()];
		this.blockStates = new IBlockState[localAddresses.size()];
		int i = 0;
		for (int localAddress : localAddresses) {
			this.localAddresses[i] = localAddress;
			this.blockStates[i] = cube.getBlockState(
				AddressTools.getLocalX(localAddress),
				AddressTools.getLocalY(localAddress),
				AddressTools.getLocalZ(localAddress)
			);
			i++;
		}
	}

	@Override
	public void readPacketData(PacketBuffer in)
	throws IOException {
		// you know what... I think these aren't even used in standalone mode
	}

	@Override
	public void writePacketData(PacketBuffer out)
	throws IOException {
		// you know what... I think these aren't even used in standalone mode
	}

	@Override
	public void processPacket(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
