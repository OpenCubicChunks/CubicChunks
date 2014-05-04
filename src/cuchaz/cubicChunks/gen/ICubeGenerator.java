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
package cuchaz.cubicChunks.gen;

import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public interface ICubeGenerator
{
	Column generateColumn( int cubeX, int cubeZ );
	Cube generateCube( Column column, int cubeX, int cubeY, int cubeZ );
	void populate( ICubeGenerator generator, int cubeX, int cubeY, int cubeZ );
	List<BiomeGenBase.SpawnListEntry> getPossibleCreatures( EnumCreatureType creatureType, int cubeX, int cubeY, int cubeZ );
	ChunkPosition getNearestStructure( World world, String structureType, int blockX, int blockY, int blockZ );
	void recreateStructures( int cubeX, int cubeY, int cubeZ );
}
