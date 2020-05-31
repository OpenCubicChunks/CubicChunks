package io.github.opencubicchunks.cubicchunks.network;

import io.github.opencubicchunks.cubicchunks.chunk.IClientCubeProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;

public class PacketUpdateCubePosition {
    private final SectionPos pos;

    public PacketUpdateCubePosition(SectionPos posIn)
    {
        this.pos = posIn;
    }

    PacketUpdateCubePosition(PacketBuffer buf)
    {
        this.pos = SectionPos.of(buf.readInt(), buf.readInt(), buf.readInt());
    }

    void encode(PacketBuffer buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
    }

    public static class Handler {
        public static void handle(PacketUpdateCubePosition packet, World worldIn) {
            AbstractChunkProvider chunkProvider = worldIn.getChunkProvider();
            ((IClientCubeProvider) chunkProvider).setCenter(packet.pos.getX(), packet.pos.getY(), packet.pos.getZ());
        }
    }
}
