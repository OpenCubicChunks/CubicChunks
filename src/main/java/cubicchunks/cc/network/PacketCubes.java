package cubicchunks.cc.network;

import static net.minecraft.world.chunk.Chunk.EMPTY_SECTION;

import cubicchunks.cc.chunk.IClientSectionProvider;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.utils.MathUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class PacketCubes {
    // vanilla has max chunk size of 2MB, it works out to be 128kB per cube
    private static final int MAX_CUBE_SIZE = 128 * 1024;

    private final SectionPos[] positions;
    private final BitSet cubeExists;
    private final byte[] packetData;
    private final List<CompoundNBT> tileEntityTags;

    public PacketCubes(List<Cube> cubes) {
        this.positions = cubes.keySet().toArray(new SectionPos[0]);
        this.cubeExists = new BitSet(cubes.size());
        this.packetData = new byte[calculateDataSize(cubes)];
        fillDataBuffer(wrapBuffer(this.packetData), cubes, cubeExists);
        this.tileEntityTags = new ArrayList<>();
        //this.tileEntityTags = cubes.values().stream()
        //        .flatMap(cube -> cube.getTileEntities().values().stream())
        //        .map(TileEntity::getUpdateTag)
        //        .collect(Collectors.toList());
    }

    PacketCubes(PacketBuffer buf) {
        this.positions = new SectionPos[buf.readVarInt()];
        for (int i = 0; i < this.positions.length; i++) {
            positions[i] = SectionPos.of(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }
        // one long stores information about 64 chunks
        int length = MathUtil.ceilDiv(positions.length, 64);
        this.cubeExists = BitSet.valueOf(buf.readLongArray(new long[length]));
        int packetLength = buf.readVarInt();
        if (packetLength > MAX_CUBE_SIZE * positions.length) {
            throw new RuntimeException("Cubes Packet trying to allocate too much memory on read: " +
                    packetLength + " bytes for " + positions.length + " cubes");
        }
        this.packetData = new byte[packetLength];
        buf.readBytes(this.packetData);
        int teTagCount = buf.readVarInt();
        this.tileEntityTags = new ArrayList<>(teTagCount);
        for (int i = 0; i < teTagCount; i++) {
            this.tileEntityTags.add(buf.readCompoundTag());
        }
    }

    void encode(PacketBuffer buf) {
        buf.writeVarInt(positions.length);
        for (SectionPos pos : positions) {
            buf.writeInt(pos.getSectionX());
            buf.writeInt(pos.getSectionY());
            buf.writeInt(pos.getSectionZ());
        }

        buf.writeLongArray(cubeExists.toLongArray());

        buf.writeVarInt(this.packetData.length);
        buf.writeBytes(this.packetData);
        buf.writeVarInt(this.tileEntityTags.size());

        for (CompoundNBT compoundnbt : this.tileEntityTags) {
            buf.writeCompoundTag(compoundnbt);
        }
    }

    public static class Handler {

        public static void handle(PacketCubes packet, World worldIn) {
            ClientWorld world = (ClientWorld) worldIn;

            PacketBuffer dataReader = wrapBuffer(packet.packetData);
            BitSet cubeExists = packet.cubeExists;
            for (int i = 0; i < packet.positions.length; i++) {
                SectionPos pos = packet.positions[i];
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                //ClientCubeProvider cubeProvider = (ClientCubeProvider) world.getChunkProvider();
                //            ICube cube = null;// cubeProvider.loadCube(x, y, z, null, dataReader, cubeExists.get(i));

                ((IClientSectionProvider) world.getChunkProvider()).loadSection(
                        x, y, z, null, dataReader, new CompoundNBT(), cubeExists.get(i));

                // TODO: full cube info
                //            if (cube != null /*&&fullCube*/) {
                //                world.addEntitiesToChunk(cube.getColumn());
                //            }
                world.markSurroundingsForRerender(x, y, z);

                for (CompoundNBT nbt : packet.tileEntityTags) {
                    BlockPos tePos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
                    TileEntity te = world.getTileEntity(tePos);
                    if (te != null) {
                        te.handleUpdateTag(nbt);
                    }
                }
            }
        }
    }
    private static void fillDataBuffer(PacketBuffer buf, Map<SectionPos, ChunkSection> cubes, BitSet existingChunks) {
        int i = 0;
        for (SectionPos sectionPos : cubes.keySet()) {
            ChunkSection section = cubes.get(sectionPos);
            if (section != EMPTY_SECTION && !section.isEmpty()) {
                existingChunks.set(i);
                section.write(buf);
            }
            i++;
        }
    }

    private static PacketBuffer wrapBuffer(byte[] packetData) {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(packetData);
        bytebuf.writerIndex(0);
        return new PacketBuffer(bytebuf);
    }

    private static int calculateDataSize(Map<SectionPos, ChunkSection> cubes) {
        return cubes.values().stream()
                .filter(c -> c != EMPTY_SECTION && !c.isEmpty())
                .mapToInt(ChunkSection::getSize).sum();
    }
}