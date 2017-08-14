package cubicchunks.util;

import cubicchunks.network.AbstractClientMessageHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUtils {

    private static final int MASK_6 = (1 << 6) - 1;
    private static final int MASK_7 = (1 << 7) - 1;

    public static void write(ByteBuf buf, BlockPos pos) {
        writeSignedVarInt(buf, pos.getX());
        writeSignedVarInt(buf, pos.getY());
        writeSignedVarInt(buf, pos.getZ());
    }

    public static BlockPos readBlockPos(ByteBuf buf) {
        return new BlockPos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
    }

    public static void write(ByteBuf buf, CubePos pos) {
        writeSignedVarInt(buf, pos.getX());
        writeSignedVarInt(buf, pos.getY());
        writeSignedVarInt(buf, pos.getZ());
    }

    public static CubePos readCubePos(ByteBuf buf) {
        return new CubePos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
    }

    /**
     * Writes signed int with variable length encoding, using at most 5 bytes.
     *
     * Unlike vanilla/forge one, this ensures that value and ~value are written using the same amount of bytes
     */
    public static void writeSignedVarInt(ByteBuf buf, int i) {
        int signBit = (i >>> 31) << 6;
        int val = i < 0 ? ~i : i;
        assert val >= 0;

        writeVarIntByte(buf, (val & MASK_6) | signBit, (val >>= 6) > 0);
        while (val > 0) {
            writeVarIntByte(buf, (val & MASK_7), (val >>= 7) > 0);
        }
    }

    /**
     * Reads signed int with variable length encoding, using at most 5 bytes.
     *
     * @see PacketUtils#writeSignedVarInt(ByteBuf, int)
     */
    public static int readSignedVarInt(ByteBuf buf) {
        int val = 0;
        int b = buf.readUnsignedByte();
        boolean sign = ((b >> 6) & 1) != 0;

        val |= b & MASK_6;
        int shift = 6;
        while ((b & 0x80) != 0) {
            if (shift > Integer.SIZE) {
                throw new RuntimeException("VarInt too big");
            }
            b = buf.readUnsignedByte();
            val |= (b & MASK_7) << shift;
            shift += 7;
        }
        return sign ? ~val : val;
    }

    private static void writeVarIntByte(ByteBuf buf, int i, boolean hasMore) {
        buf.writeByte(i | (hasMore ? 0x80 : 0));
    }

    public static <T extends IMessage> void ensureMainThread(AbstractClientMessageHandler<T> handler,
            EntityPlayer player, T message, MessageContext ctx) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handler.handleClientMessage(player, message, ctx));
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
