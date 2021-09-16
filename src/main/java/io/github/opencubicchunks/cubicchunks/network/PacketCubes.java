package io.github.opencubicchunks.cubicchunks.network;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.client.multiplayer.ClientCubeCache;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public class PacketCubes {
    // vanilla has max chunk size of 2MB, it works out to be 128kB for a 32^3 cube
    private static final int MAX_CUBE_SIZE = (CubeAccess.DIAMETER_IN_SECTIONS * CubeAccess.DIAMETER_IN_SECTIONS * CubeAccess.DIAMETER_IN_SECTIONS) * 128 * 1024;

    private final CubePos[] cubePositions;
    private final LevelCube[] cubes;
    private final BitSet cubeExists;
    private final List<int[]> biomeDataArrays;
    private final byte[] packetData;
    private final List<CompoundTag> tileEntityTags;

    public PacketCubes(List<LevelCube> cubes) {
        this.cubes = cubes.toArray(new LevelCube[0]);
        this.cubePositions = new CubePos[this.cubes.length];
        this.cubeExists = new BitSet(cubes.size());
        this.biomeDataArrays = fillBiomeData();
        this.packetData = new byte[calculateDataSize(cubes)];
        fillDataBuffer(wrapBuffer(this.packetData), cubes, cubeExists);
        this.tileEntityTags = cubes.stream()
            .flatMap(cube -> cube.getTileEntityMap().values().stream())
            .map(BlockEntity::getUpdateTag)
            .collect(Collectors.toList());
    }

    PacketCubes(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.cubes = new LevelCube[count];
        this.cubePositions = new CubePos[count];
        for (int i = 0; i < count; i++) {
            cubePositions[i] = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());
        }

        // one long stores information about 64 chunks
        int length = MathUtil.ceilDiv(cubes.length, 64);
        this.cubeExists = BitSet.valueOf(buf.readLongArray(new long[length]));

        biomeDataArrays = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            biomeDataArrays.add(buf.readVarIntArray());
        }

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
        for (LevelCube cube : cubes) {
            buf.writeInt(cube.getCubePos().getX());
            buf.writeInt(cube.getCubePos().getY());
            buf.writeInt(cube.getCubePos().getZ());
        }

        buf.writeLongArray(cubeExists.toLongArray());

        biomeDataArrays.forEach(buf::writeVarIntArray);

        buf.writeVarInt(this.packetData.length);
        buf.writeBytes(this.packetData);
        buf.writeVarInt(this.tileEntityTags.size());

        for (CompoundTag compoundnbt : this.tileEntityTags) {
            buf.writeNbt(compoundnbt);
        }
    }

    private static void fillDataBuffer(FriendlyByteBuf buf, List<LevelCube> cubes, BitSet existingChunks) {
        buf.writerIndex(0);
        int i = 0;
        for (LevelCube cube : cubes) {
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

    private static int calculateDataSize(List<LevelCube> cubes) {
        return cubes.stream().filter(c -> !c.isEmptyCube()).mapToInt(LevelCube::getSize).sum();
    }

    private List<int[]> fillBiomeData() {
        List<int[]> dataArrays = new ArrayList<>(cubes.length);
        for (LevelCube cube : cubes) {
            ChunkBiomeContainer cubeBiomeContainer = cube.getBiomes();
            if (cubeBiomeContainer != null) {
                dataArrays.add(cubeBiomeContainer.writeBiomes());
            } else {
                CubicChunks.LOGGER.error("Cube had null biomes! Sending empty array");
                dataArrays.add(new int[] { 1 });
            }
        }
        return dataArrays;
    }

    public static class Handler {

        public static void handle(PacketCubes packet, Level level) {
            ClientLevel clientLevel = (ClientLevel) level;

            FriendlyByteBuf dataReader = wrapBuffer(packet.packetData);
            BitSet cubeExists = packet.cubeExists;
            for (int i = 0; i < packet.cubes.length; i++) {
                CubePos pos = packet.cubePositions[i];
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                CubeBiomeContainer cubeBiomeContainer =
                    new CubeBiomeContainer(Minecraft.getInstance().level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY),
                        new CubeSerializer.CubeBoundsLevelHeightAccessor(CubeAccess.DIAMETER_IN_BLOCKS, pos.minCubeY(), (CubicLevelHeightAccessor) clientLevel),
                        packet.biomeDataArrays.get(i));

                ((ClientCubeCache) clientLevel.getChunkSource()).replaceWithPacketData(
                    x, y, z, cubeBiomeContainer, dataReader, new CompoundTag(), cubeExists.get(i));

                // TODO: full cube info
                //            if (cube != null /*&&fullCube*/) {
                //                world.addEntitiesToChunk(cube.getColumn());
                //            }
                for (int dx = 0; dx < CubeAccess.DIAMETER_IN_SECTIONS; dx++) {
                    for (int dy = 0; dy < CubeAccess.DIAMETER_IN_SECTIONS; dy++) {
                        for (int dz = 0; dz < CubeAccess.DIAMETER_IN_SECTIONS; dz++) {
                            clientLevel.setSectionDirtyWithNeighbors(
                                cubeToSection(x, dx),
                                cubeToSection(y, dy),
                                cubeToSection(z, dz));
                        }
                    }
                }

                for (CompoundTag nbt : packet.tileEntityTags) {
                    BlockPos blockPos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
                    BlockEntity blockEntity = clientLevel.getBlockEntity(blockPos);
                    if (blockEntity != null) {
                        blockEntity.load(nbt);
                    }
                }
            }
        }
    }
}
