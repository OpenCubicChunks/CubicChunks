package cubicchunks;

import static org.junit.Assert.assertEquals;

import cubicchunks.util.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class TestPacketUtil {

    @Test
    public void testSignedVarInts() {
        testRange(-128, 128, 0);
        testRange(-128, 128, 6);
        testRange(-128, 128, 12);
        testRange(-128, 128, 18);
        testRange(-128, 128, 24);
        testRange(Integer.MIN_VALUE, Integer.MIN_VALUE + 128, 0);
        testRange(Integer.MAX_VALUE - 128, Integer.MAX_VALUE, 0);
    }

    private void testRange(int min, int max, int bitshift) {
        // 5* because max 5 bytes per int
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[5 * (max - min + 1)]);
        buf.writerIndex(0);
        for (long i = min; i <= max; i++) {
            int val = (int) i << bitshift;
            PacketUtils.writeSignedVarInt(buf, val);
        }
        buf.readerIndex(0);
        for (long i = min; i <= max; i++) {
            int val = (int) i << bitshift;
            assertEquals(val, PacketUtils.readSignedVarInt(buf));
        }
    }
}
