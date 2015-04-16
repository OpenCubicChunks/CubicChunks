package cubicchunks.util;

public class MathHelper {

	public static double lerp(final double a, final double min, final double max) {
		return min + a * (max - min);
	}
}
