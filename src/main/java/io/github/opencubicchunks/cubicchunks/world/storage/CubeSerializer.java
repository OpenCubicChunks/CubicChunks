package io.github.opencubicchunks.cubicchunks.world.storage;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.EmptyTickList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

public class CubeSerializer {

    public static ICube loadCube(World world, CubePos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(cubePath))))) {
            ChunkStatus status = ChunkStatus.getAll().get(in.readUnsignedByte());
            ChunkSection[] sections = new ChunkSection[ICube.CUBE_SIZE];

            for (int i = 0; i < ICube.CUBE_SIZE; i++) {
                boolean isEmpty = in.readBoolean();
                if (!isEmpty) {
                    ChunkSection chunkSection = new ChunkSection(pos.minCubeY() + Coords.indexToY(i));
                    sections[i] = chunkSection;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState state = Block.BLOCK_STATE_IDS.getByValue(in.readInt());
                                if (state != null) {
                                    chunkSection.setBlockState(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }

            ICube cube;
            if (status.getType() == ChunkStatus.Type.PROTOCHUNK) {
                cube = new CubePrimer(pos, sections);
                cube.setCubeStatus(status);

                if (cube.getCubeStatus().isAtLeast(ChunkStatus.FEATURES)) {
                    ((CubePrimer)cube).setLightManager(world.getChunkProvider().getLightManager());
                }

            } else {
                Cube cubeIn = new Cube(world, pos, null, UpgradeData.EMPTY, EmptyTickList.get(), EmptyTickList.get(), 0L, sections, null);
                cubeIn.setCubeStatus(status);
                cube = new CubePrimerWrapper(cubeIn);
            }
//            for (CompoundNBT tileEntityNTB : tileEntityNTBs) {
//                cube.addCubeTileEntity(tileEntityNTB);
//            }
            return cube;
        }
    }

    @Nullable
    public static ICube loadCubeOld(World world, CubePos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(cubePath))))) {
            ChunkStatus status = ChunkStatus.getAll().get(in.readUnsignedByte());
            ChunkSection[] sections = new ChunkSection[ICube.CUBE_SIZE];

            for (int i = 0; i < ICube.CUBE_SIZE; i++) {
                boolean isEmpty = in.readBoolean();
                if (!isEmpty) {
                    ChunkSection chunkSection = new ChunkSection(pos.minCubeY() + Coords.indexToY(i));
                    sections[i] = chunkSection;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState state = Block.BLOCK_STATE_IDS.getByValue(in.readInt());
                                if (state != null) {
                                    chunkSection.setBlockState(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }

            ICube cube;
            if (status.getType() == ChunkStatus.Type.PROTOCHUNK) {
                cube = new CubePrimer(pos, sections);
                cube.setCubeStatus(status);

                if (cube.getCubeStatus().isAtLeast(ChunkStatus.FEATURES)) {
                    ((CubePrimer)cube).setLightManager(world.getChunkProvider().getLightManager());
                }

            } else {

                Cube cubeIn = new Cube(world, pos, null, UpgradeData.EMPTY, EmptyTickList.get(), EmptyTickList.get(), 0L, sections, null);
                cubeIn.setCubeStatus(status);
                cube = new CubePrimerWrapper(cubeIn);
            }
            return cube;
        }
    }

    public static void writeCube(World world, ICube cube, Path worldDir) throws IOException {
        CubePos pos = cube.getCubePos();
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath.getParent())) {
            Files.createDirectories(cubePath.getParent());
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(cubePath))))) {
            out.writeByte(cube.getCubeStatus().ordinal());

            for (ChunkSection s : cube.getCubeSections()) {
                boolean exists = s != Chunk.EMPTY_SECTION && !s.isEmpty();
                out.writeBoolean(!exists);
                if (exists) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                out.writeInt(Block.BLOCK_STATE_IDS.get(s.getBlockState(x, y, z)));
                            }
                        }
                    }
                }
            }
        }
    }
}
