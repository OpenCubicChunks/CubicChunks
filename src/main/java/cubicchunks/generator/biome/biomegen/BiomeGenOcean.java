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
package main.java.cubicchunks.generator.biome.biomegen;

<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenOcean.java
=======
import java.util.Random;

import main.java.cubicchunks.world.Cube;
import net.minecraft.world.World;

>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenOcean.java
public class BiomeGenOcean extends CubeBiomeGenBase
{
	public BiomeGenOcean( int id )
	{
		super( id );
		this.spawnableCreatureList.clear();
	}

	@Override
	public CubeBiomeGenBase.TempCategory func_150561_m()
	{
		return CubeBiomeGenBase.TempCategory.OCEAN;
	}
}
