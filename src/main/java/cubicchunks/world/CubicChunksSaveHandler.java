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
package cubicchunks.world;

import cubicchunks.server.ServerCubeCache;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;
import java.io.File;

public class CubicChunksSaveHandler implements ISaveHandler {
	private ICubicWorldServer world;
	private final ISaveHandler originalHandler;

	public CubicChunksSaveHandler(ICubicWorldServer world, ISaveHandler originalHandler) {
		this.world = world;
		this.originalHandler = originalHandler;
	}

	@Override public WorldInfo loadWorldInfo() {
		return originalHandler.loadWorldInfo();
	}

	@Override public void checkSessionLock() throws MinecraftException {
		originalHandler.checkSessionLock();
	}

	@Override public IChunkLoader getChunkLoader(@Nonnull WorldProvider provider) {
		return originalHandler.getChunkLoader(provider);
	}

	@Override public void saveWorldInfoWithPlayer(@Nonnull WorldInfo worldInformation, @Nonnull NBTTagCompound tagCompound) {
		originalHandler.saveWorldInfoWithPlayer(worldInformation, tagCompound);
	}

	@Override public void saveWorldInfo(@Nonnull WorldInfo worldInformation) {
		originalHandler.saveWorldInfo(worldInformation);
	}

	@Override public IPlayerFileData getPlayerNBTManager() {
		return originalHandler.getPlayerNBTManager();
	}

	@Override public void flush() {
		originalHandler.flush();
		ServerCubeCache cache = world.getCubeCache();
		cache.flush();
	}

	@Override public File getWorldDirectory() {
		return originalHandler.getWorldDirectory();
	}

	@Override public File getMapFileFromName(@Nonnull String mapName) {
		return originalHandler.getMapFileFromName(mapName);
	}

	@Override public TemplateManager getStructureTemplateManager() {
		return originalHandler.getStructureTemplateManager();
	}
}
