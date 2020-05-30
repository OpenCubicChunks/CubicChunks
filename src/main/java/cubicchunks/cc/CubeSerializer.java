package cubicchunks.cc;

import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.cube.CubePrimer;
import cubicchunks.cc.chunk.cube.CubePrimerWrapper;
import cubicchunks.cc.utils.Coords;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import cubicchunks.cc.chunk.util.CubePos;
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

public class CubeSerializer {

    public static ICube loadCube(World world, CubePos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes32/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(cubePath))))) {
            ChunkStatus status = ChunkStatus.getStatus(in.readUnsignedByte());
            ChunkSection[] sections = new ChunkSection[ICube.CUBESIZE];

            for (int i = 0; i < ICube.CUBESIZE; i++) {
                boolean isEmpty = in.readBoolean();
                if (!isEmpty) {
                    ChunkSection chunkSection = new ChunkSection(pos.minCubeY() + Coords.indexTo32Y(i));
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
            } else {
                cube = new CubePrimerWrapper(new Cube(world, pos, sections, null));
            }
            cube.setCubeStatus(status);
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
