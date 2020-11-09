package io.github.opencubicchunks.cubicchunks.world.storage;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
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

import static net.minecraft.nbt.NbtIo.writeCompressed;

public class RegionCubeIO {

    private static final long kB = 1024;
    private static final long MB = kB * 1024;
    private static final Logger LOGGER = CubicChunks.LOGGER;

    @Nonnull private Level world;
    @Nonnull private final File storageFolder;
    private SaveCubeColumns save;
    @Nonnull private ConcurrentMap<ChunkPos, SaveEntry<EntryLocation2D>> columnsToSave;
    @Nonnull private ConcurrentMap<CubePos, SaveEntry<EntryLocation3D>> cubesToSave;

    public RegionCubeIO(Level world, File storageFolder) throws IOException {
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
        if (world instanceof ServerLevel) {
            file = DimensionType.getStorageFolder(world.dimension(), storageFolder);
        } else {
            //TODO: implement client world
            throw new IOException("NOT IMPLEMENTED");
//            Path path = Paths.get(".").toAbsolutePath().resolve("clientCache").resolve("DIM" + world.dimension());
        }

        this.save = SaveCubeColumns.create(file.toPath());
    }

    @Nullable public CompoundTag loadCubeNBT(CubePos cubePos) throws IOException {
        SaveCubeColumns save = this.getSave();

        Optional<ByteBuffer> buf = save.load(new EntryLocation3D(cubePos.getX(), cubePos.getY(), cubePos.getZ()), true);
        if(!buf.isPresent())
            return null;

        return NbtIo.readCompressed(new ByteArrayInputStream(buf.get().array()));
    }

    public void storeCubeNBT(CubePos cubePos, CompoundTag cubeNBT) throws IOException {
        SaveCubeColumns save = this.getSave();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeCompressed(cubeNBT, outputStream);

        ByteBuffer buf = ByteBuffer.wrap(outputStream.toByteArray());

        save.save3d(new EntryLocation3D(cubePos.getX(), cubePos.getY(), cubePos.getZ()), buf);
    }

    private static class SaveEntry<T extends IKey<?>> {

        private final T pos;
        private final CompoundTag nbt;

        SaveEntry(T pos, CompoundTag nbt) {
            this.pos = pos;
            this.nbt = nbt;
        }
    }
}