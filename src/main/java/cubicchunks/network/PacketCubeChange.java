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
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketCubeChange implements IMessage {
	
	public long cubeAddress;
	public byte[] data;

	public PacketCubeChange(){}

	public PacketCubeChange(Cube cube) {
		cubeAddress = cube.getAddress();

		try {
			data = new byte[WorldEncoder.getEncodedSize(cube)];
			PacketBuffer out = new PacketBuffer(Unpooled.wrappedBuffer(data));
			out.writerIndex(0);
			WorldEncoder.encodeCube(out, cube);
		} catch (IOException ex) {
			// writing to byte arrays doesn't throw exceptions... Java is dumb sometimes
			throw new Error(ex);
		}
	}
	
	public void decodeCube(Cube cube) {
		try {
			PacketBuffer in = new PacketBuffer(Unpooled.wrappedBuffer(data));
			in.readerIndex(0);
			WorldEncoder.decodeCube(in, cube);
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		cubeAddress = buf.readLong();
		int len = buf.readInt();
		data = new byte[len];
		buf.readBytes(data);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(cubeAddress);
		buf.writeInt(data.length);
		buf.writeBytes(data);
	}

	public static class Handler extends AbstractClientMessageHandler<PacketCubeChange> {

		@Override
		public IMessage handleClientMessage(EntityPlayer player, PacketCubeChange message, MessageContext ctx) {
			ClientHandler.getInstance().handle(message);
			return null;
		}
	}
}
