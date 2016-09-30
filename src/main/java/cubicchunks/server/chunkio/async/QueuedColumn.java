package cubicchunks.server.chunkio.async;

import com.google.common.base.Objects;
import net.minecraft.world.World;

class QueuedColumn {
	final int x;
	final int z;
	final World world;

	QueuedColumn(int x, int z, World world) {
		this.x = x;
		this.z = z;
		this.world = world;
	}

	@Override
	public int hashCode() {
		return (x * 31 + z * 29) ^ world.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof QueuedColumn) {
			QueuedColumn other = (QueuedColumn) object;
			return x == other.x && z == other.z && world == other.world;
		}

		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(this.world)
				.add("x", this.x)
				.add("z", this.z)
				.toString();
	}
}
