package cuchaz.cubicChunks.util;

import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import net.minecraft.util.MathHelper;

public class HeightHelper
{
	public static double getScaledHeight_Double( double y )
	{
		return NewTerrainProcessor.maxElev * (y - 64D) / 64D;
	}

	public static double getVanillaHeight_Double( double y )
	{
		return 64D + 64D * y / (double)NewTerrainProcessor.maxElev;
	}

	public static int getScaledHeight( int y )
	{
		return (int)Math.round( getScaledHeight_Double( y ) );
	}

	public static int getVanillaHeight( int y )
	{
		return (int)Math.round( getVanillaHeight_Double( y ) );
	}
}
