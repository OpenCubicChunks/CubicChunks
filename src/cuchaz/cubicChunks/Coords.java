package cuchaz.cubicChunks;

public class Coords
{
	public static int blockToLocal( int val )
	{
		return val & 0xf;
	}
	
	public static int blockToChunk( int val )
	{
		return val >> 4;
	}
	
	public static int localToBlock( int chunk, int local )
	{
		return ( chunk << 4 ) + local;
	}
}
