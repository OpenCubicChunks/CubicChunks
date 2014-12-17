package cuchaz.cubicChunks.generator.terrain;

import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_X;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Y;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Z;

public class GlobalGeneratorConfig
{
	public static final double maxElev = 64;
	
	// these are constants. Changing them may cause issues.
	public static final int X_SECTION_SIZE = 4 + 1;
	public static final int Y_SECTION_SIZE = 8 + 1;
	public static final int Z_SECTION_SIZE = 4 + 1;
	
	public static final int CUBE_X_SIZE = 16;
	
	public static final int CUBE_Y_SIZE = 16;
	
	public static final int CUBE_Z_SIZE = 16;
	
	public static final int X_SECTIONS = CUBE_X_SIZE / ( X_SECTION_SIZE - 1 ) + 1;
	
	public static final int Y_SECTIONS = CUBE_Y_SIZE / ( Y_SECTION_SIZE - 1 ) + 1;
	
	public static final int Z_SECTIONS = CUBE_Z_SIZE / ( Z_SECTION_SIZE - 1 ) + 1;
}
