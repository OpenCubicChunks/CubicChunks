/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.generator;

import cubicchunks.CubeCache;
import cubicchunks.generator.features.CubicCaveGen;
import cubicchunks.generator.features.CubicRavineGen;
import cubicchunks.util.CubeProcessor;
import cubicchunks.world.Cube;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;

public class FeatureProcessor extends CubeProcessor {
	
	private CubicCaveGen caveGenerator;
	private MapGenStronghold m_strongholdGenerator;
	private MapGenVillage m_villageGenerator;
	private MapGenMineshaft m_mineshaftGenerator;
	private MapGenScatteredFeature m_scatteredFeatureGenerator;
	private CubicRavineGen ravineGenerator;
	
	private World worldObj;
	
	public FeatureProcessor(String name, CubeCache provider, int batchSize) {
		super(name, provider, batchSize);
		
		caveGenerator = new CubicCaveGen();
		m_strongholdGenerator = new MapGenStronghold();
		m_villageGenerator = new MapGenVillage();
		m_mineshaftGenerator = new MapGenMineshaft();
		m_scatteredFeatureGenerator = new MapGenScatteredFeature();
		ravineGenerator = new CubicRavineGen();
	}
	
	@Override
	public boolean calculate(Cube cube) {
		worldObj = cube.getWorld();
		
		// generate world features
		caveGenerator.generate(worldObj, cube);
		ravineGenerator.generate(worldObj, cube);
		/*
		 * UNDONE: enable feature generation if( m_mapFeaturesEnabled ) { m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks ); m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks ); m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks ); m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks ); }
		 */
		
		return true;
	}
}
