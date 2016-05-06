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
package cubicchunks.asm.mixin.core.client;

import cubicchunks.ICubicChunksWorldType;
import cubicchunks.asm.mixin.core.MixinWorld;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.lighting.LightingManager;
import cubicchunks.world.ICubicWorldClient;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;

@Mixin(WorldClient.class)
@Implements(@Interface(iface = ICubicWorldClient.class, prefix = "world$"))
public abstract class MixinWorldClient extends MixinWorld implements ICubicWorldClient {

	@Shadow private ChunkProviderClient clientChunkProvider;

	@Shadow public abstract boolean invalidateRegionAndSetBlock(BlockPos pos, IBlockState blockState);

	@Override public void initCubicWorld() {
		this.isCubicWorld = true;
		ClientCubeCache clientCubeCache = new ClientCubeCache(this);
		this.chunkProvider = clientCubeCache;
		this.clientChunkProvider = clientCubeCache;
		this.lightingManager = new LightingManager(this);

		ICubicChunksWorldType type = (ICubicChunksWorldType) this.getWorldType();
		this.maxHeight = type.getMaxHeight();
		this.minHeight = type.getMinHeight();
	}

	@Override public ClientCubeCache getCubeCache() {
		return (ClientCubeCache) this.clientChunkProvider;
	}

	@Intrinsic public boolean world$invalidateRegionAndSetBlock(BlockPos pos, IBlockState blockState) {
		return this.invalidateRegionAndSetBlock(pos, blockState);
	}
}
