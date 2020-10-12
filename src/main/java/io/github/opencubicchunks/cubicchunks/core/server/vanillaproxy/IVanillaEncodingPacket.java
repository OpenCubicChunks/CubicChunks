package io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public interface IVanillaEncodingPacket {
    void writeVanillaPacketData(PacketBuffer buf, EntityPlayerMP player) throws IOException;
}
