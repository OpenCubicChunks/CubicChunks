/*******************************************************************************
 * Copyright (c) 2014 Nick Whitney.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Nick - initial implementation
 ******************************************************************************/
package main.java.cubicchunks.generator.biome.biomegen;

import java.util.ArrayList;
import java.util.List;

import cuchaz.magicMojoModLoader.classTransformation.transformers.*;

public class BiomeGenRegistry
{
	private static List<CubeBiomeGenBase> biomeList;
	
	protected static final CubeBiomeGenBase.Height oceanRange = new CubeBiomeGenBase.Height(-1.0F, 0.1F);
	
	static
	{
		biomeList = new ArrayList<CubeBiomeGenBase>();
		biomeList.add( new BiomeGenBeach(0).setColor(112).setBiomeName("Ocean").setHeightRange(oceanRange) );
	}
	
	public static List<CubeBiomeGenBase> getBiomes( )
	{
		return new ArrayList<CubeBiomeGenBase>( biomeList );
	}
}