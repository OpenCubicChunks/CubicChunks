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
package cubicchunks.server;

import com.google.common.collect.Maps;
import cubicchunks.ICubicChunksWorldType;
import cubicchunks.worldgen.GeneratorPipeline;
import cubicchunks.world.WorldContext;
import net.minecraft.world.WorldServer;

import java.util.Map;

public class WorldServerContext extends WorldContext {

	private static Map<WorldServer, WorldServerContext> instances;

	static {
		instances = Maps.newHashMap();
	}

	public static WorldServerContext get(final WorldServer worldServer) {
		return instances.get(worldServer);
	}

	public static void put(final WorldServer worldServer, final WorldServerContext worldServerContext) {
		instances.put(worldServer, worldServerContext);
	}

	public static void clear() {
		instances.clear();
	}
	
	private WorldServer worldServer;
	private ServerCubeCache serverCubeCache;
	private GeneratorPipeline generatorPipeline;

	public WorldServerContext(final WorldServer worldServer, final ServerCubeCache serverCubeCache) {
		super(worldServer, serverCubeCache);

		this.worldServer = worldServer;
		this.serverCubeCache = serverCubeCache;
		this.generatorPipeline = new GeneratorPipeline(serverCubeCache);

		ICubicChunksWorldType type = (ICubicChunksWorldType) worldServer.getWorldType();
		type.registerWorldGen(this.worldServer, this.generatorPipeline);
		this.generatorPipeline.checkStages();
	}

	@Override
	public WorldServer getWorld() {
		return this.worldServer;
	}

	@Override
	public ServerCubeCache getCubeCache() {
		return this.serverCubeCache;
	}

	public GeneratorPipeline getGeneratorPipeline() {
		return this.generatorPipeline;
	}
}
