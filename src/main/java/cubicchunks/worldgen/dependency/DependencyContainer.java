package cubicchunks.worldgen.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cubicchunks.world.cube.Cube;

public class DependencyContainer extends HashSet<Dependency> implements Dependency {

	@Override
	public boolean isSatisfied() {
		return this.size() == 0 || this.stream().allMatch(d -> d.isSatisfied());
	}

	@Override
	public boolean update(Cube cube) {
		boolean done = true;
		Iterator<Dependency> iter = this.iterator();
		while (iter.hasNext()) {
			Dependency dependency = iter.next();
			if (dependency.update(cube)) {
				iter.remove();
			} else {
				done = false;
			}
		}
		return done;
	}

	@Override
	public Collection<Long> getRequirements() {
		Set<Long> requirements = new HashSet<Long>();
		this.stream().forEach(d -> requirements.addAll(d.getRequirements()));
		return requirements;
	}

	@Override
	public boolean dependsOn(Cube cube) {
		return this.size() > 0 && this.stream().anyMatch(d -> d.dependsOn(cube));
	}

}
