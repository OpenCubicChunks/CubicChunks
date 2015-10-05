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
package cubicchunks.network;

import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class PacketUnloadCubes implements IMessage {

	public static final int MAX_SIZE = 0xFFFF;
	
	public long[] cubeAddresses;

	public PacketUnloadCubes(){}

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
	public void fromBytes(ByteBuf in) {
		cubeAddresses = new long[in.readUnsignedShort()];
		for (int i=0; i<cubeAddresses.length; i++) {
			cubeAddresses[i] = in.readLong();
		}
	}

	@Override
	public void toBytes(ByteBuf out) {
		out.writeShort(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
	}

	public static class Handler extends AbstractClientMessageHandler<PacketUnloadCubes> {

		@Override
		public IMessage handleClientMessage(EntityPlayer player, PacketUnloadCubes message, MessageContext ctx) {
			ClientHandler.getInstance().handle(message);
			return null;
		}
	}
}
