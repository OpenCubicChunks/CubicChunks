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

import com.google.common.base.Throwables;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketCube implements IMessage {

	private long cubeAddress;
	private byte[] data;
	private PacketBuffer in;

	public PacketCube() {}

	public PacketCube(Cube cube) {
		this.cubeAddress = cube.getAddress();
		this.data = new byte[WorldEncoder.getEncodedSize(cube)];
		PacketBuffer out = new PacketBuffer(WorldEncoder.createByteBufForWrite(this.data));
		try {
			WorldEncoder.encodeCube(out, cube);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.cubeAddress = buf.readLong();
		this.data = new byte[buf.readInt()];
		buf.readBytes(this.data);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(this.cubeAddress);
		buf.writeInt(this.data.length);
		buf.writeBytes(this.data);
	}

	public long getCubeAddress() {
		return cubeAddress;
	}

	public byte[] getData() {
		return data;
	}

	public static class Handler extends AbstractClientMessageHandler<PacketCube> {
		@Override
		public IMessage handleClientMessage(EntityPlayer player, PacketCube message, MessageContext ctx) {
			ClientHandler.getInstance().handle(message);
			return null;
		}
	}
}
