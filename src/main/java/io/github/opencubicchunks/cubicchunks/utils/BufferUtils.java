package io.github.opencubicchunks.cubicchunks.utils;

import net.minecraft.network.FriendlyByteBuf;

public class BufferUtils {
    public static void writeSignedVarInt(FriendlyByteBuf buf, int value) {
        buf.writeVarInt(value >= 0 ? value << 1 | 1 : ~value << 1);
    }

    public static int readSignedVarInt(FriendlyByteBuf buf) {
        int val = buf.readVarInt();
        return (val & 1) == 0 ? ~(val >>> 1) : val >>> 1;
    }
}