package cuchaz.cubicChunks.generator.terrain;

import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_X;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Y;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Z;

public class GlobalGeneratorConfig
{
	public static final double maxElev = 256;
	
	//these are constants. Changing them may cause issues.
	public static final int X_NOISE_SIZE = CUBE_MAX_X / 4 + 1;
	public static final int Y_NOISE_SIZE = CUBE_MAX_Y / 8 + 1;
	public static final int Z_NOISE_SIZE = CUBE_MAX_Z / 4 + 1;

	public static final int CUBE_X_SIZE = 16;

	public static final int CUBE_Y_SIZE = 16;

	public static final int CUBE_Z_SIZE = 16;

	public static final int xNoiseSize = CUBE_X_SIZE / 4 + 1;

	public static final int yNoiseSize = CUBE_Y_SIZE / 8 + 1;

    public static final int zNoiseSize = CUBE_Z_SIZE / 4 + 1;
}
