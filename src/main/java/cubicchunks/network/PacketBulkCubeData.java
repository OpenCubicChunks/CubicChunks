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

import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.*;
import java.util.List;


public class PacketBulkCubeData implements IMessage {

	public long[] columnAddresses;
	public long[] cubeAddresses;
	public byte[] data;
	private DataInputStream m_in;

	//this constructor must be there to instantiate packet via reflection
	public PacketBulkCubeData() {}
	
	public PacketBulkCubeData(List<Column> columns, List<Cube> cubes) {
		
		if (columns.size() > 255) {
			throw new IllegalArgumentException("Don't send more than 255 columns at a time!");
		}
		if (cubes.size() > 255) {
			throw new IllegalArgumentException("Don't send more than 255 cubes at a time!");
		}
		
		// save the addresses
		columnAddresses = new long[columns.size()];
		for (int i=0; i<columns.size(); i++) {
			columnAddresses[i] = columns.get(i).getAddress();
		}
		cubeAddresses = new long[cubes.size()];
		for (int i=0; i<cubes.size(); i++) {
			cubeAddresses[i] = cubes.get(i).getAddress();
		}
		
		try {
			
			// encode the cubes and columns
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(buf)) {
				for (Column column : columns) {
					WorldEncoder.encodeColumn(out, column);
				}
				for (Cube cube : cubes) {
					WorldEncoder.encodeCube(out, cube);
				}
			}
			data = buf.toByteArray();

		} catch (IOException ex) {
			// writing to byte arrays doesn't throw exceptions... Java is dumb sometimes
			throw new Error(ex);
		}
	}

	public void startDecoding() {
		m_in = new DataInputStream(new ByteArrayInputStream(data));
	}
	
	public void decodeNextColumn(Column column) {
		try {
			WorldEncoder.decodeColumn(m_in, column);
		} catch (IOException ex) {
			// if you saw this exception, you're probably not using the decode functions correctly
			throw new Error(ex);
		}
	}

	public void decodeNextCube(Cube cube) {
		try {
			WorldEncoder.decodeCube(m_in, cube);
		} catch (IOException ex) {
			// if you saw this exception, you're probably not using the decode functions correctly
			throw new Error(ex);
		}
	}
	
	public void finishDecoding() {
		try {
			m_in.close();
		} catch (IOException ex) {
			// if you saw this exception, you're probably not using the decode functions correctly
			throw new Error(ex);
		}
		m_in = null;
	}

	@Override
	public void fromBytes(ByteBuf in) {
		cubeAddresses = new long[in.readUnsignedByte()];
		for (int i=0; i<cubeAddresses.length; i++) {
			cubeAddresses[i] = in.readLong();
		}
		columnAddresses = new long[in.readUnsignedByte()];
		for (int i=0; i<columnAddresses.length; i++) {
			columnAddresses[i] = in.readLong();
		}
		data = new byte[in.readInt()];
		System.out.println("Receive " + data.length + " bytes, " + cubeAddresses.length + " cubes, " + columnAddresses.length + " columns");
		in.readBytes(data);
	}

	@Override
	public void toBytes(ByteBuf out) {
		out.writeByte(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
		out.writeByte(columnAddresses.length);
		for (int i=0; i<columnAddresses.length; i++) {
			out.writeLong(columnAddresses[i]);
		}
		out.writeInt(data.length);
		System.out.println("Send " + data.length + " bytes, " + cubeAddresses.length + " cubes, " + columnAddresses.length + " columns");
		out.writeBytes(data);
	}

	public static class Handler extends AbstractClientMessageHandler<PacketBulkCubeData> {

		@Override
		public IMessage handleClientMessage(EntityPlayer player, PacketBulkCubeData message, MessageContext ctx) {
			//TODO: move packet handling to specific handler classes
			ClientHandler.getInstance().handle(message);
			return null;
		}
	}
}
