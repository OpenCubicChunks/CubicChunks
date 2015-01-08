package cuchaz.cubicChunks.generator.biome.alternateGen;

import java.util.ArrayList;
import java.util.List;

class NoiseArrays
{
	private final List<double[][]> arrays = new ArrayList<double[][]>( 4 );

	NoiseArrays( double[][] volatility, double[][] height, double[][] temperature, double[][] rainfall )
	{
		//add 4 null elements to set them later.
		while( arrays.size() < 4 ) arrays.add( null );
		arrays.set( Type.VOLATILITY.ordinal(), volatility );
		arrays.set( Type.HEIGHT.ordinal(), height );
		arrays.set( Type.TEMPERATURE.ordinal(), temperature );
		arrays.set( Type.RAINFALL.ordinal(), rainfall );
	}

	double[][] get( Type type )
	{
		return arrays.get( type.ordinal() );
	}

	enum Type
	{
		VOLATILITY,
		HEIGHT,
		TEMPERATURE,
		RAINFALL
	}
	
}
