package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CCHeightmap {
	private static final Predicate<BlockState> NOT_AIR = (blockState) -> !blockState.isAir();
	private static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = (blockState) -> blockState.getMaterial().blocksMotion();
	private final BitStorage data;
	private final Predicate<BlockState> isOpaque;
	private final ChunkAccess chunk;
	public static final int BLOCK_COLUMNS_PER_COLUMN = 16 * 16; //Always needs to be a power of 2.

	int scale;

	private boolean isDirty = false;

	public CCHeightmap(int scale) {
		this.isOpaque = null;
		this.chunk = null;
		this.scale = scale;
		this.data = new BitStorage((4 + scale) + 1, BLOCK_COLUMNS_PER_COLUMN); // If the highest bit is 1, the value is null.
	}

	public BitStorage getData() {
		return data;
	}

	public void setDirty() {
		isDirty = true;
	}

	public int getScale() {
		return scale;
	}
}