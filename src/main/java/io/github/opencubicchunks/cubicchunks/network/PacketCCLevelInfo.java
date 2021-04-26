package io.github.opencubicchunks.cubicchunks.network;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

public class PacketCCLevelInfo {
    private final String worldStyle;

    public PacketCCLevelInfo(CubicLevelHeightAccessor.WorldStyle worldStyle) {
        this.worldStyle = worldStyle.name();
    }

    PacketCCLevelInfo(FriendlyByteBuf buf) {
        this.worldStyle = buf.readUtf();
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.worldStyle);
    }

    public static class Handler {
        public static void handle(PacketCCLevelInfo packet, Level worldIn) {
            ((CubicLevelHeightAccessor) worldIn).setWorldStyle(CubicLevelHeightAccessor.WorldStyle.valueOf(packet.worldStyle.toUpperCase()));
        }
    }
}
