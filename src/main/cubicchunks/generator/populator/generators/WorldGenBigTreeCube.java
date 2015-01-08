package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;

public class WorldGenBigTreeCube extends WorldGenAbstractTreeCube
{
	private final WorldGenBigTree gen;

	public WorldGenBigTreeCube( boolean doBlockNotify )
	{
		super( doBlockNotify );
		gen = new WorldGenBigTree( doBlockNotify );
	}

	@Override
	public boolean generate( World var1, Random var2, int var3, int var4, int var5 )
	{
		return gen.generate( var1, var2, var3, var4, var5 );
	}

	@Override
	public void setScale( double x, double y, double z )
	{
		gen.setScale( x, y, z );
	}

}
