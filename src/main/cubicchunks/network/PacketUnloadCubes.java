package cubicchunks.network;

import java.io.IOException;
import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import cubicchunks.world.cube.Cube;


public class PacketUnloadCubes implements IPacket<INetHandler> {

	public static final int MAX_SIZE = 65535;
	
	public long[] cubeAddresses;
	
	public PacketUnloadCubes(List<Cube> cubes) {
		
		if (cubes.size() > MAX_SIZE) {
			throw new IllegalArgumentException("Don't send more than " + MAX_SIZE + " cube unloads at a time!");
		}
		
		cubeAddresses = new long[cubes.size()];
		for (int i=0; i<cubes.size(); i++) {
			cubeAddresses[i] = cubes.get(i).getAddress();
		}
	}

	@Override
	public void read(PacketBuffer in)
	throws IOException {
		cubeAddresses = new long[in.readUnsignedShort()];
		for (int i=0; i<cubeAddresses.length; i++) {
			cubeAddresses[i] = in.readLong();
		}
	}

	@Override
	public void write(PacketBuffer out)
	throws IOException {
		out.writeShort(cubeAddresses.length);
		for (int i=0; i<cubeAddresses.length; i++) {
			out.writeLong(cubeAddresses[i]);
		}
	}

	@Override
	public void handle(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
