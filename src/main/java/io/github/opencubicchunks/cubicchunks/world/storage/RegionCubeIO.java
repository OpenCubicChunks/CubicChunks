package io.github.opencubicchunks.cubicchunks.world.storage;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.minecraft.nbt.CompressedStreamTools.writeCompressed;

public class RegionCubeIO {

    private static final long kB = 1024;
    private static final long MB = kB * 1024;
    private static final Logger LOGGER = CubicChunks.LOGGER;

    @Nonnull private World world;
    @Nonnull private final File storageFolder;
    private SaveCubeColumns save;
    @Nonnull private ConcurrentMap<ChunkPos, SaveEntry<EntryLocation2D>> columnsToSave;
    @Nonnull private ConcurrentMap<CubePos, SaveEntry<EntryLocation3D>> cubesToSave;

    public RegionCubeIO(World world, File storageFolder) throws IOException {
        this.world = world;
        this.storageFolder = storageFolder;

        initSave();

        // init chunk save queue
        this.columnsToSave = new ConcurrentHashMap<>();
        this.cubesToSave = new ConcurrentHashMap<>();
    }

    @Nonnull
    private synchronized SaveCubeColumns getSave() throws IOException {
        if (save == null) {
            initSave();
        }
        return save;
    }

    private void initSave() throws IOException {
        // TODO: make path a constructor argument
        File file;
        if (world instanceof ServerWorld) {
            file = DimensionType.getStorageFolder(world.dimension(), storageFolder);
        } else {
            //TODO: implement client world
            throw new IOException("NOT IMPLEMENTED");
//            Path path = Paths.get(".").toAbsolutePath().resolve("clientCache").resolve("DIM" + world.dimension());
        }

        this.save = SaveCubeColumns.create(file.toPath());
    }

    @Nullable public CompoundNBT loadCubeNBT(CubePos cubePos) throws IOException {
        SaveCubeColumns save = this.getSave();

        Optional<ByteBuffer> buf = save.load(new EntryLocation3D(cubePos.getX(), cubePos.getY(), cubePos.getZ()), true);
        if(!buf.isPresent())
            return null;

        return CompressedStreamTools.readCompressed(new ByteArrayInputStream(buf.get().array()));
    }

    public void storeCubeNBT(CubePos cubePos, CompoundNBT cubeNBT) throws IOException {
        SaveCubeColumns save = this.getSave();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeCompressed(cubeNBT, outputStream);

        ByteBuffer buf = ByteBuffer.wrap(outputStream.toByteArray());

        save.save3d(new EntryLocation3D(cubePos.getX(), cubePos.getY(), cubePos.getZ()), buf);
    }

    private static class SaveEntry<T extends IKey<?>> {

        private final T pos;
        private final CompoundNBT nbt;

        SaveEntry(T pos, CompoundNBT nbt) {
            this.pos = pos;
            this.nbt = nbt;
        }
    }
}