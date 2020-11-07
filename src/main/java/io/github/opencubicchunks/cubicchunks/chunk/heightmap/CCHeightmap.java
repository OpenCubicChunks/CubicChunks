package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import net.minecraft.util.BitStorage;

public class CCHeightmap {
	// Always needs to be a power of 2.
	public static final int BLOCK_COLUMN_WIDTH = 16;
	public static final int BLOCK_COLUMNS_PER_COLUMN = BLOCK_COLUMN_WIDTH * BLOCK_COLUMN_WIDTH;

	/**
	 * Stores the relative position of the highest block within this heightmap region for each x,z position.
	 * A highest bit of 1 indicates a null value (i.e. no blocks)
	 */
	private final BitStorage heightData;
	/**
	 * Indicates whether the height for a given x,z position is dirty and needs to be recomputed.
	 */
	private final BitStorage dirty;

	int scale;

	public static int getIndexForCoords(int x, int z) {
		return z * BLOCK_COLUMN_WIDTH + x;
	}

	public CCHeightmap(int scale) {
		this.scale = scale;
		// 4 for log2(BLOCK_COLUMN_WIDTH), +1 for null values
		this.heightData = new BitStorage((4 + scale) + 1, BLOCK_COLUMNS_PER_COLUMN);
		this.dirty = new BitStorage(1, BLOCK_COLUMNS_PER_COLUMN);
	}

//	public BitStorage getData() {
//		return data;
//	}

	public void setDirty(int x, int z) {
		this.dirty.set(getIndexForCoords(x, z), 1);
	}

	public void setHeight(int x, int z, int relativeHeight) {
		this.heightData.set(getIndexForCoords(x, z), relativeHeight);
	}

	public int getScale() {
		return scale;
	}
}
