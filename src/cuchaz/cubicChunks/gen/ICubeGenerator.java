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
