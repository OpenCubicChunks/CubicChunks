package io.github.opencubicchunks.cubicchunks.network;

import io.github.opencubicchunks.cubicchunks.client.multiplayer.ClientCubeCache;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

public class PacketUpdateCubePosition {
    private final SectionPos pos;

    public PacketUpdateCubePosition(SectionPos posIn) {
        this.pos = posIn;
    }

    PacketUpdateCubePosition(FriendlyByteBuf buf) {
        this.pos = SectionPos.of(buf.readInt(), buf.readInt(), buf.readInt());
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
    }

    public static class Handler {
        public static void handle(PacketUpdateCubePosition packet, Level worldIn) {
            ChunkSource chunkProvider = worldIn.getChunkSource();
            ((ClientCubeCache) chunkProvider).setCenter(packet.pos.getX(), packet.pos.getY(), packet.pos.getZ());
        }
    }
}