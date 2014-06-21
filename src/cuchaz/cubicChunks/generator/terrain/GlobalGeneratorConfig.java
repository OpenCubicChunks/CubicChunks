package cuchaz.cubicChunks.generator.terrain;

import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_X;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Y;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Z;

public class GlobalGeneratorConfig
{
	public static final double maxElev = 128;
	
	//these are constants. Changing them may cause issued.
	public static final int X_NOISE_SIZE = CUBE_MAX_X / 4 + 1;
	public static final int Y_NOISE_SIZE = CUBE_MAX_Y / 8 + 1;
	public static final int Z_NOISE_SIZE = CUBE_MAX_Z / 4 + 1;
}
