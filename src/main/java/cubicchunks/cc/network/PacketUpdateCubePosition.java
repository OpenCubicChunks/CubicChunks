package cubicchunks.cc.network;

import cubicchunks.cc.chunk.util.CubePos;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;

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
        public static void handle(cubicchunks.cc.network.PacketUpdateCubePosition packet, World worldIn) {

        }
    }
}
