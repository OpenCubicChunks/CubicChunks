package cubicchunks.asm;

import cubicchunks.CubicChunkSystem;
import net.minecraft.world.World;

/**
 * This class has methods to get world height and depth.
 * Should be used from asm transformed code.
 */
public final class WorldHeightAccess {
	private static CubicChunkSystem cc;

	public static int getMinHeight(World world) {
		//System.out.println("GetMinHeight");
		return cc.getMinBlockY(world);
	}

	public static int getMaxHeight(World world) {
		return cc.getMaxBlockY(world);
	}

	public static void registerChunkSystem(CubicChunkSystem cc) {
		WorldHeightAccess.cc = cc;
	}
}
