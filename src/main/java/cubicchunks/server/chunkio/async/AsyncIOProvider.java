package cubicchunks.server.chunkio.async;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Interface for grouping asynchronous world IO access together, synchronized to the start of the next tick
 * after loading finishes
 * @author Malte Schütze
 */
abstract class AsyncIOProvider<T> implements Runnable {
	private final ConcurrentLinkedQueue<Consumer<T>> callbacks = new ConcurrentLinkedQueue<>();
	volatile boolean finished = false;

	/**
	 * Add a callback to this access group, to be executed when the load finishes
	 * @param callback The callback to execute
	 */
	void addCallback(Consumer<T> callback) {
		this.callbacks.add(callback);
	}

	/**
	 * Remove a callback. It will no longer be executed when the load finshes
	 * @param callback The callback to remove
	 */
	void removeCallback(Consumer<T> callback) {
		this.callbacks.remove(callback);
	}

	/**
	 * Run all callbacks waiting for the load. Assumes that the load is finished; calling this before is undefined
	 * behavior.
	 */
	void runCallbacks()
	{
		T value = this.get();
		for (Consumer<T> callback : this.callbacks) // Sponge: Runnable -> Consumer<Cube>
		{
			callback.accept(value);
		}

		this.callbacks.clear();
	}

	/**
	 * True if the target has been loaded and is available for use
	 * @return if this is finished
	 */
	boolean isFinished() {
		return finished;
	}

	/**
	 * Check if any callbacks are registered as waiting for this load.
	 * @return <code>true</code> if there is at least one callback waiting
	 */
	boolean hasCallbacks() {
		return !callbacks.isEmpty();
	}

	/**
	 * Finalize the loading operating synchronously from the main thread.
	 */
	abstract void runSynchronousPart();

	/**
	 * Retrive the loaded object. Undefined if the load hasn't finished yet
	 * @return The loaded object
	 */
	@Nullable
	abstract T get();
}
