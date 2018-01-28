/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.io.File;
import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubicSaveHandler implements ISaveHandler {

    private ICubicWorldInternal.Server world;
    private final ISaveHandler originalHandler;

    public CubicSaveHandler(WorldServer world, ISaveHandler originalHandler) {
        this.world = (ICubicWorldInternal.Server) world;
        this.originalHandler = originalHandler;
    }

    @Override public WorldInfo loadWorldInfo() {
        return originalHandler.loadWorldInfo();
    }

    @Override public void checkSessionLock() throws MinecraftException {
        originalHandler.checkSessionLock();
    }

    @Override public IChunkLoader getChunkLoader(WorldProvider provider) {
        return originalHandler.getChunkLoader(provider);
    }

    @Override
    public void saveWorldInfoWithPlayer(WorldInfo worldInformation, NBTTagCompound tagCompound) {
        originalHandler.saveWorldInfoWithPlayer(worldInformation, tagCompound);
    }

    @Override public void saveWorldInfo(WorldInfo worldInformation) {
        originalHandler.saveWorldInfo(worldInformation);
    }

    @Override public IPlayerFileData getPlayerNBTManager() {
        return originalHandler.getPlayerNBTManager();
    }

    @Override public void flush() {
        originalHandler.flush();
        CubeProviderServer cache = world.getCubeCache();
        try {
            cache.flush();
        } catch (IOException e) {
            // ignore because that's what vanilla does
            CubicChunks.LOGGER.error(e);
        }
    }

    @Override public File getWorldDirectory() {
        return originalHandler.getWorldDirectory();
    }

    @Override public File getMapFileFromName(String mapName) {
        return originalHandler.getMapFileFromName(mapName);
    }

    @Override public TemplateManager getStructureTemplateManager() {
        return originalHandler.getStructureTemplateManager();
    }
}
