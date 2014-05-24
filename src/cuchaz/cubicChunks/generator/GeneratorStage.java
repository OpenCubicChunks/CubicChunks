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


public enum GeneratorStage
{
	Terrain,
	Biomes,
	Features,
	Population,
	Lighting,
	Live;
	
	public static GeneratorStage getFirstStage( )
	{
		return values()[0];
	}
	
	public static GeneratorStage getLastStage( )
	{
		return values()[values().length - 1];
	}
	
	public boolean isLastStage( )
	{
		return ordinal() == values().length - 1;
	}
}
