package cuchaz.cubicChunks.gen.builder;

import cuchaz.cubicChunks.gen.lib.exception.ExceptionInvalidParam;

public abstract interface IBuilder 
{	
	public void setSeed(int seed);
	
	public void setSeaLevel(double seaLevel);
	
	public void build() throws ExceptionInvalidParam;
	
	public double getValue(double x, double y, double z);
}
