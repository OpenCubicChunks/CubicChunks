package cuchaz.cubicChunks.generator.terrain;

import cuchaz.cubicChunks.util.Coords;
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
	
	public static final int X_SECTIONS = Coords.CUBE_MAX_X / ( X_SECTION_SIZE - 1 ) + 1;
	public static final int Y_SECTIONS = Coords.CUBE_MAX_Y / ( Y_SECTION_SIZE - 1 ) + 1;
	public static final int Z_SECTIONS = Coords.CUBE_MAX_Z / ( Z_SECTION_SIZE - 1 ) + 1;
}
