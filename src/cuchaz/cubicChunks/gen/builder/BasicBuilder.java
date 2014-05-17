package cuchaz.cubicChunks.gen.builder;

import cuchaz.cubicChunks.gen.lib.exception.ExceptionInvalidParam;
import cuchaz.cubicChunks.gen.lib.module.*;

public class BasicBuilder implements IBuilder
{	
	ModuleBase finalModule;

	// Planet seed.  Change this to generate a different planet.
	int SEED = 0;

	// Minimum elevation, in meters.  This value is approximate.
	double MIN_ELEV = -8192.0;

	// Maximum elevation, in meters.  This value is approximate.
	double MAX_ELEV = 8192.0;

	// Specifies the sea level.  This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;
	
	@Override
	public void setSeed(int seed)
	{
		this.SEED = seed;	
	}
	
	public void setMinElev(double minElev)
	{
		this.MIN_ELEV = minElev;	
	}
	
	public void setMaxElev(double maxElev)
	{
		this.MAX_ELEV = maxElev;	
	}
	/**
	 * Sets the sea level for the world. This must be between -1.0 (minimum
	 * elevation) and 1.0 (maximum elevation). I recommend dividing the desired 
	 * seaLevel by the desired build height and feeding the result into this method.
	 */
	public void setSeaLevel(double seaLevel)
	{
		this.SEA_LEVEL = seaLevel;	
	}

	@Override
	public void build() throws ExceptionInvalidParam
	{
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(SEED);
		baseContinentDef_pe0.setFrequency(1.0);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(2.2089);
		baseContinentDef_pe0.setOctaveCount(10);
		baseContinentDef_pe0.setUp();
		
		ScaleBias scaleBias = new ScaleBias(baseContinentDef_pe0);
		scaleBias.setScale(MAX_ELEV);
		scaleBias.setBias(SEA_LEVEL);
		
		finalModule = scaleBias;
	}

	@Override
	public double getValue(double x, double y, double z)
	{
		return finalModule.getValue(x, y, z);
	}

}
