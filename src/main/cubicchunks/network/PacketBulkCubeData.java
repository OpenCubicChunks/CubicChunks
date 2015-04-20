package cubicchunks.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;


public class PacketBulkCubeData implements IPacket<INetHandler> {

	public long[] columnAddresses;
	public long[] cubeAddresses;
	public byte[] data;
	private DataInputStream m_in;
	
	public PacketBulkCubeData() {
	}
	
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
			DataOutputStream out = new DataOutputStream(buf);
			for (int i=0; i<columns.size(); i++) {
				Column column = columns.get(i);
				WorldEncoder.encodeColumn(out, column);
			}
			for (int i=0; i<cubes.size(); i++) {
				Cube cube = cubes.get(i);
				WorldEncoder.encodeCube(out, cube);
			}
			out.close();
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
	public void read(PacketBuffer in)
	throws IOException {
		cubeAddresses = new long[in.readUnsignedByte()];
		for (int i=0; i<cubeAddresses.length; i++) {
			cubeAddresses[i] = in.readLong();
		}
		columnAddresses = new long[in.readUnsignedByte()];
		for (int i=0; i<columnAddresses.length; i++) {
			columnAddresses[i] = in.readLong();
		}
		in.readBytes(data);
	}

	@Override
	public void write(PacketBuffer out)
	throws IOException {
		out.writeByte(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
		out.writeByte(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
		out.writeBytes(data);
	}

	@Override
	public void handle(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
