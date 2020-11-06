package io.github.opencubicchunks.cubicchunks.network;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.cubeToSection;

import io.github.opencubicchunks.cubicchunks.chunk.IClientCubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PacketCubes {
    // vanilla has max chunk size of 2MB, it works out to be 128kB for a 32^3 cube
    private static final int MAX_CUBE_SIZE = (IBigCube.DIAMETER_IN_SECTIONS * IBigCube.DIAMETER_IN_SECTIONS * IBigCube.DIAMETER_IN_SECTIONS) * 128 * 1024;

    private final CubePos[] cubePositions;
    private final BigCube[] cubes;
    private final BitSet cubeExists;
    private final byte[] packetData;
    private final List<CompoundTag> tileEntityTags;

    public PacketCubes(List<BigCube> cubes) {
        this.cubes = cubes.toArray(new BigCube[0]);
        this.cubePositions = new CubePos[this.cubes.length];
        this.cubeExists = new BitSet(cubes.size());
        this.packetData = new byte[calculateDataSize(cubes)];
        fillDataBuffer(wrapBuffer(this.packetData), cubes, cubeExists);
        this.tileEntityTags = cubes.stream()
                .flatMap(cube -> cube.getTileEntityMap().values().stream())
                .map(BlockEntity::getUpdateTag)
                .collect(Collectors.toList());
    }

    PacketCubes(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.cubes = new BigCube[count];
        this.cubePositions = new CubePos[count];
        for (int i = 0; i < count; i++) {
            cubePositions[i] = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());
        }

        // one long stores information about 64 chunks
        int length = MathUtil.ceilDiv(cubes.length, 64);
        this.cubeExists = BitSet.valueOf(buf.readLongArray(new long[length]));
        int packetLength = buf.readVarInt();
        if (packetLength > MAX_CUBE_SIZE * cubes.length) {
            throw new RuntimeException("Cubes Packet trying to allocate too much memory on read: " +
                    packetLength + " bytes for " + cubes.length + " cubes");
        }
        this.packetData = new byte[packetLength];
        buf.readBytes(this.packetData);
        int teTagCount = buf.readVarInt();
        this.tileEntityTags = new ArrayList<>(teTagCount);
        for (int i = 0; i < teTagCount; i++) {
            this.tileEntityTags.add(buf.readNbt());
        }
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(cubes.length);
        for (BigCube cube : cubes) {
            buf.writeInt(cube.getCubePos().getX());
            buf.writeInt(cube.getCubePos().getY());
            buf.writeInt(cube.getCubePos().getZ());
        }

        buf.writeLongArray(cubeExists.toLongArray());

        buf.writeVarInt(this.packetData.length);
        buf.writeBytes(this.packetData);
        buf.writeVarInt(this.tileEntityTags.size());

        for (CompoundTag compoundnbt : this.tileEntityTags) {
            buf.writeNbt(compoundnbt);
        }
    }

    public static class Handler {

        public static void handle(PacketCubes packet, Level worldIn) {
            ClientLevel world = (ClientLevel) worldIn;

            FriendlyByteBuf dataReader = wrapBuffer(packet.packetData);
            BitSet cubeExists = packet.cubeExists;
            for (int i = 0; i < packet.cubes.length; i++) {
                CubePos pos = packet.cubePositions[i];
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                ((IClientCubeProvider) world.getChunkSource()).replaceWithPacketData(
                        x, y, z, null, dataReader, new CompoundTag(), cubeExists.get(i));

                // TODO: full cube info
                //            if (cube != null /*&&fullCube*/) {
                //                world.addEntitiesToChunk(cube.getColumn());
                //            }
                for (int dx = 0; dx < IBigCube.DIAMETER_IN_SECTIONS; dx++) {
                    for (int dy = 0; dy < IBigCube.DIAMETER_IN_SECTIONS; dy++) {
                        for (int dz = 0; dz < IBigCube.DIAMETER_IN_SECTIONS; dz++) {
                            world.setSectionDirtyWithNeighbors(
                                    cubeToSection(x, dx),
                                    cubeToSection(y, dy),
                                    cubeToSection(z, dz));
                        }
                    }
                }

                for (CompoundTag nbt : packet.tileEntityTags) {
                    BlockPos tePos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
                    BlockEntity te = world.getBlockEntity(tePos);
                    if (te != null) {
                        te.load(world.getBlockState(tePos), nbt);
                    }
                }
            }
        }
    }
    private static void fillDataBuffer(FriendlyByteBuf buf, List<BigCube> cubes, BitSet existingChunks) {
        buf.writerIndex(0);
        int i = 0;
        for (BigCube cube : cubes) {
            if (!cube.isEmptyCube()) {
                existingChunks.set(i);
                cube.write(buf);
            }
            i++;
        }
    }

    private static FriendlyByteBuf wrapBuffer(byte[] packetData) {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(packetData);
        return new FriendlyByteBuf(bytebuf);
    }

    private static int calculateDataSize(List<BigCube> cubes) {
        return cubes.stream().filter(c -> !c.isEmptyCube()).mapToInt(BigCube::getSize).sum();
    }
}