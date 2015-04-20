package cubicchunks.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import cubicchunks.world.cube.Cube;


public class PacketCubeChange implements IPacket<INetHandler> {
	
	public long cubeAddress;
	public byte[] data;

	public PacketCubeChange(Cube cube) {
		cubeAddress = cube.getAddress();
		
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			WorldEncoder.encodeCube(out, cube);
			out.close();
			data = buf.toByteArray();
		} catch (IOException ex) {
			// writing to byte arrays doesn't throw exceptions... Java is dumb sometimes
			throw new Error(ex);
		}
	}
	
	public void decodeCube(Cube cube) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));		
			WorldEncoder.decodeCube(in, cube);
			in.close();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	@Override
	public void read(PacketBuffer in)
	throws IOException {
		cubeAddress = in.readLong();
		in.readBytes(data);
	}

	@Override
	public void write(PacketBuffer out)
	throws IOException {
		out.writeLong(cubeAddress);
		out.writeBytes(data);
	}

	@Override
	public void handle(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
