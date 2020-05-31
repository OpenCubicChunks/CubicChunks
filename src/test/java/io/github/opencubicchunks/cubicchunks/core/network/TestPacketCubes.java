package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.test.MinecraftTestRunner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MinecraftTestRunner.class)
public class TestPacketCubes {

    @Test
    public void testSerializeDeserialize() {
        List<ICube> cubes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ICube cube = mock(ICube.class);
            when(cube.getPos()).thenReturn(SectionPos.of(i, 0, 0));
            ChunkSection section = new ChunkSection(0);
            if (i % 2 == 0) {
                section.setBlockState(0, 0, 0, Blocks.STONE.getDefaultState());
            }
            when(cube.getStorage()).thenReturn(section);
            when(cube.getTileEntities()).thenReturn(new HashMap<>());
        }
        ByteBuf byteBuf = Unpooled.buffer();
        PacketBuffer buf = new PacketBuffer(byteBuf);
        PacketCubes packet = new PacketCubes(cubes);
        packet.encode(buf);

        buf.resetReaderIndex();
        PacketCubes readPacket = new PacketCubes(buf);

        assertEquals(packet.getCubeExists(), readPacket.getCubeExists());
        assertEquals(packet.getTileEntityTags(), readPacket.getTileEntityTags());
        assertArrayEquals(packet.getPacketData(), readPacket.getPacketData());
        assertArrayEquals(packet.getPositions(), readPacket.getPositions());
        throw new RuntimeException("test exception");
    }
}
