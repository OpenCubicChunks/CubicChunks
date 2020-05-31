package cubicchunks.cc.network;

import cubicchunks.cc.chunk.IClientCubeProvider;
import cubicchunks.cc.chunk.util.CubePos;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;

public class PacketUnloadCube {
    private final CubePos pos;

    public PacketUnloadCube(CubePos posIn)
    {
        this.pos = posIn;
    }

    PacketUnloadCube(PacketBuffer buf)
    {
        this.pos = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());
    }

    void encode(PacketBuffer buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
    }

    public static class Handler {
        public static void handle(PacketUnloadCube packet, World worldIn) {
            AbstractChunkProvider chunkProvider = worldIn.getChunkProvider();
            ((IClientCubeProvider) chunkProvider).unloadCube(packet.pos.getX(), packet.pos.getY(), packet.pos.getZ());
        }
    }
}
