package cubicchunks.util;

import net.minecraft.util.ClassInheritanceMultiMap;

import java.util.ConcurrentModificationException;

import cubicchunks.CubicChunks;

public class ClassInheritanceMultiMapFactory {
	/**
	 * Creates new ClassInheritanceMultiMap without possibility of ConcurrentModificationException
	 */
	public static <T> ClassInheritanceMultiMap<T> create(Class<T> c) {
		/*
		 * TODO: Check if it's still relevant in future versions
		 *
		 * This is a hack to workaround a vanilla threading issue.
		 * This is bad and should be removed as soon as the issue is fixed
		 *
		 * The only way to actually fix it is to change vanilla code, which I'm not going to do.
		 * Replacing ALL_KNOWN with concurrent hash set won't fix it, synchronization is necessary here.
		 * The issue is that one thread may add something to ALL_KNOWN while the constructor in other thread
		 * is iterating over the set.
		 * It rarely/never happens in vanilla because timings happen to be just right to avoid the issue
		 */
		ClassInheritanceMultiMap<T> obj = null;
		do {
			try {
				obj = new ClassInheritanceMultiMap<>(c);
			} catch (ConcurrentModificationException ex) {
				CubicChunks.LOGGER.error("Error creating ClassInheritanceMultiMap, this is threading issue, trying again...", ex);
			}
		} while (obj == null);

		return obj;
	}
}
