/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.generator;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.generator.features.CubicCaveGen;
import cuchaz.cubicChunks.generator.features.CubicRavineGen;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;

public class FeatureProcessor extends CubeProcessor
{
	private CubicCaveGen caveGenerator;
	private MapGenStronghold m_strongholdGenerator;
	private MapGenVillage m_villageGenerator;
	private MapGenMineshaft m_mineshaftGenerator;
	private MapGenScatteredFeature m_scatteredFeatureGenerator;
	private CubicRavineGen ravineGenerator;
	
	private World worldObj;
	
	public FeatureProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
		
		caveGenerator = new CubicCaveGen();
		m_strongholdGenerator = new MapGenStronghold();
		m_villageGenerator = new MapGenVillage();
		m_mineshaftGenerator = new MapGenMineshaft();
		m_scatteredFeatureGenerator = new MapGenScatteredFeature();
		ravineGenerator = new CubicRavineGen();	
	}
	
	@Override
	public boolean calculate( Cube cube )
	{	
		worldObj = cube.getWorld();
		
		// generate world features
		caveGenerator.generate( worldObj, cube );
		ravineGenerator.generate( worldObj, cube );
		/* UNDONE: enable feature generation
		if( m_mapFeaturesEnabled )
		{
			m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
		}
		*/
		
		return true;
	}
}
