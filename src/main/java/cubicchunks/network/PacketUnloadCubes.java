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
import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import cubicchunks.world.cube.Cube;
import net.minecraft.network.Packet;


public class PacketUnloadCubes implements Packet {

	public static final int MAX_SIZE = 65535;
	
	public long[] cubeAddresses;
	
	public PacketUnloadCubes(List<Cube> cubes) {
		
		if (cubes.size() > MAX_SIZE) {
			throw new IllegalArgumentException("Don't send more than " + MAX_SIZE + " cube unloads at a time!");
		}
		
		cubeAddresses = new long[cubes.size()];
		int i = 0;
		for (Cube cube : cubes) {
			cubeAddresses[i] = cube.getAddress();
			i++;
		}
	}

	@Override
	public void readPacketData(PacketBuffer in)
	throws IOException {
		cubeAddresses = new long[in.readUnsignedShort()];
		for (int i=0; i<cubeAddresses.length; i++) {
			cubeAddresses[i] = in.readLong();
		}
	}

	@Override
	public void writePacketData(PacketBuffer out)
	throws IOException {
		out.writeShort(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
	}

	@Override
	public void processPacket(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
