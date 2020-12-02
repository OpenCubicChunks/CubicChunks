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

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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

    AsyncColumnIOProvider(QueuedColumn colInfo, ICubeIO loader, ICubeGenerator generator) {
        this.loader = loader;
        this.colInfo = colInfo;
        this.generator = generator;
    }

    @Override public void run() {
        try {
            this.columnData = this.loader.loadColumnAsyncPart(this.colInfo.x, this.colInfo.z);
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
        if (columnData != null) {
            this.loader.loadColumnSyncPart(columnData);
            Chunk column = this.columnData.getObject();
            assert column != null;
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(column, this.columnData.getNbt()));
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
