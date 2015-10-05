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

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class PacketUnloadColumns implements IMessage {

	public static final int MAX_SIZE = 0xFFFF;
	
	public long[] columnAddresses;

	public PacketUnloadColumns(){}
	public PacketUnloadColumns(List<Long> columns) {
		
		if (columns.size() > MAX_SIZE) {
			throw new IllegalArgumentException("Don't send more than " + MAX_SIZE + " column unloads at a time!");
		}
		
		columnAddresses = new long[columns.size()];
		
		int i = 0;
		for (long addr : columns) {
			columnAddresses[i] = addr;
			i++;
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		columnAddresses = new long[buf.readUnsignedShort()];
		for (int i=0; i<columnAddresses.length; i++) {
			columnAddresses[i] = buf.readLong();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeShort(columnAddresses.length);
		for (long addr : columnAddresses) {
			buf.writeLong(addr);
		}
	}

	public static class Handler extends AbstractClientMessageHandler<PacketUnloadColumns> {

		@Override
		public IMessage handleClientMessage(EntityPlayer player, PacketUnloadColumns message, MessageContext ctx) {
			ClientHandler.getInstance().handle(message);
			return null;
		}
	}
}
