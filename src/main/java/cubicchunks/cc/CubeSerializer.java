package cubicchunks.cc;

import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.cube.CubePrimer;
import cubicchunks.cc.chunk.cube.CubePrimerWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
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

    public static ICube loadCube(World world, SectionPos pos, Path worldDir) throws IOException {
        Path cubePath = worldDir.resolve("cubes/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(Files.newInputStream(cubePath))))) {
            ChunkStatus status = ChunkStatus.getStatus(in.readUnsignedByte());
            ChunkSection section = new ChunkSection(pos.getY() * 16);

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = Block.BLOCK_STATE_IDS.getByValue(in.readInt());
                        if (state != null) {
                            section.setBlockState(x, y, z, state);
                        }
                    }
                }
            }

            ICube cube;
            if (status.getType() == ChunkStatus.Type.PROTOCHUNK) {
                cube = new CubePrimer(pos, section);
            } else {
                cube = new CubePrimerWrapper(new Cube(world, section, pos));
            }
            cube.setCubeStatus(status);
            return cube;
        }

    }

    public static void writeCube(World world, ICube cube, Path worldDir) throws IOException {
        SectionPos pos = cube.getSectionPos();
        Path cubePath = worldDir.resolve("cubes/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".bin");
        if (!Files.exists(cubePath.getParent())) {
            Files.createDirectories(cubePath.getParent());
        }
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(cubePath))))) {
            out.writeByte(cube.getCubeStatus().ordinal());
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        out.writeInt(Block.BLOCK_STATE_IDS.get(cube.getBlockState(new BlockPos(x, y, z))));
                    }
                }
            }
        }
    }
}
