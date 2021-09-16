package io.github.opencubicchunks.cubicchunks.network;

import io.github.opencubicchunks.cubicchunks.client.multiplayer.ClientCubeCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

public class PacketCubeCacheRadius {
    private final int hDistance;
    private final int vDistance;

    public PacketCubeCacheRadius(int hDistance, int vDistance) {
        this.hDistance = hDistance;
        this.vDistance = vDistance;
    }

    PacketCubeCacheRadius(FriendlyByteBuf buf) {
        hDistance = buf.readVarInt();
        vDistance = buf.readVarInt();
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(hDistance);
        buf.writeVarInt(vDistance);
    }

    public static class Handler {
        public static void handle(PacketCubeCacheRadius packet, Level level) {
            ChunkSource chunkSource = level.getChunkSource();
            ((ClientCubeCache) chunkSource).updateCubeViewRadius(packet.hDistance, packet.vDistance);
        }
    }
}