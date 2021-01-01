/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;

import net.minecraft.world.chunk.Chunk;

/**
 * Async loading of columns. Roughly equivalent to Forge's ChunkIOProvider
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class AsyncColumnIOProvider extends AsyncIOProvider<Chunk> {

    @Nonnull private final ICubeIO loader;
    @Nullable private ICubeIO.PartialData<Chunk> columnData; // The target
    @Nonnull private final QueuedColumn colInfo;
    private ICubeGenerator generator;
    // some mods will try to access blocks in ChunkDataEvent.Load
    // this needs the column to be already known by the chunk provider so that it can load cubes without trying to load the column again
    // this callback temporarily puts the cube into a special field in the chunk provider
    private final Consumer<Chunk> setProviderLoadingColumn;

    AsyncColumnIOProvider(QueuedColumn colInfo, ICubeIO loader, ICubeGenerator generator, Consumer<Chunk> setProviderLoadingColumn) {
        this.loader = loader;
        this.colInfo = colInfo;
        this.generator = generator;
        this.setProviderLoadingColumn = setProviderLoadingColumn;
    }

    @Override public void run() {
        try {
            this.columnData = this.loader.loadColumnAsyncPart(this.colInfo.world, this.colInfo.x, this.colInfo.z);
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not load column in {} @ ({}, {})", this.colInfo.world, this.colInfo.x, this.colInfo.z, e);
        } finally {
            synchronized (this) {
                this.finished = true;
                this.notifyAll();
            }
        }
    }

    @Override void runSynchronousPart() {
        assert columnData != null;
        if (columnData.getObject() != null) {
            this.loader.loadColumnSyncPart(columnData);
            Chunk column = this.columnData.getObject();
            assert column != null;
            try {
                setProviderLoadingColumn.accept(column);
                MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(column, this.columnData.getNbt()));
            } finally {
                setProviderLoadingColumn.accept(null);
            }
            column.setLastSaveTime(this.colInfo.world.getTotalWorldTime());
            // this actually initializes internal information about that chunk's structures
            generator.recreateStructures(column);
        }
        runCallbacks();
    }

    @Nullable @Override Chunk get() {
        return columnData == null ? null : columnData.getObject();
    }
}
