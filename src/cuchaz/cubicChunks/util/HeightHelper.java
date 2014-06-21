package cuchaz.cubicChunks.util;

import cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig;

public class HeightHelper
{
	public static double getScaledHeight_Double( double y )
	{
		return GlobalGeneratorConfig.maxElev * (y - 64D) / 64D;
	}

	public static double getVanillaHeight_Double( double y )
	{
		return 64D + 64D * y / GlobalGeneratorConfig.maxElev;
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
