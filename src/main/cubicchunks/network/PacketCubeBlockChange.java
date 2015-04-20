package cubicchunks.network;

import java.io.IOException;
import java.util.Collection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;


public class PacketCubeBlockChange implements IPacket<INetHandler> {

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
	public void read(PacketBuffer in)
	throws IOException {
		// you know what... I think these aren't even used in standalone mode
	}

	@Override
	public void write(PacketBuffer out)
	throws IOException {
		// you know what... I think these aren't even used in standalone mode
	}

	@Override
	public void handle(INetHandler vanillaHandler) {
		// don't use the vanilla handler, use our own
		// TODO: make a real network system for M3L
		ClientHandler.getInstance().handle(this);
	}
}
