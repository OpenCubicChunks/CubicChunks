package cubicchunks.cc.utils;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.junit.Test;

import java.util.stream.IntStream;

public class BufferUtilsTest {
    @Test
    public void testReadWrite() {
        // IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE).parallel().forEach(this::testValue);
        testValue(Integer.MAX_VALUE);
        testValue(Integer.MIN_VALUE);
        testValue(0);
        testValue(1);
        testValue(-1);
    }

    private void testValue(int val) {
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(new byte[8]));
        buf.resetWriterIndex();
        buf.resetReaderIndex();

        BufferUtils.writeSignedVarInt(buf, val);
        assertEquals(val, BufferUtils.readSignedVarInt(buf));
    }
}