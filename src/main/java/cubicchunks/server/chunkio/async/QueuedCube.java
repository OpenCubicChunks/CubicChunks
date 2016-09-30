package cubicchunks.server.chunkio.async;

import com.google.common.base.Objects;
import net.minecraft.world.World;

/**
 * Taking from Sponge, with modifications
 */
class QueuedCube {
	final int x;
	final int y;
	final int z;
	final World world;

	QueuedCube(int x, int y, int z, World world) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
	}

	@Override
	public int hashCode() {
		return (x * 31 + y * 23 * z * 29) ^ world.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof QueuedCube) {
			QueuedCube other = (QueuedCube) object;
			return x == other.x && y == other.y && z == other.z && world == other.world;
		}

		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(this.world)
				.add("x", this.x)
				.add("y", this.y)
				.add("z", this.z)
				.toString();
	}
}
