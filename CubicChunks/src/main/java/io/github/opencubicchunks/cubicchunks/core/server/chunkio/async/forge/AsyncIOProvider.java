/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import mcp.MethodsReturnNonnullByDefault;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Interface for grouping asynchronous world IO access together, synchronized to the start of the next tick
 * after loading finishes
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
abstract class AsyncIOProvider<T> implements Runnable {

    private final ConcurrentLinkedQueue<Consumer<T>> callbacks = new ConcurrentLinkedQueue<>();
    volatile boolean finished = false;

    /**
     * Add a callback to this access group, to be executed when the load finishes
     *
     * @param callback The callback to execute
     */
    void addCallback(Consumer<T> callback) {
        this.callbacks.add(callback);
    }

    /**
     * Remove a callback. It will no longer be executed when the load finshes
     *
     * @param callback The callback to remove
     */
    void removeCallback(Consumer<T> callback) {
        this.callbacks.remove(callback);
    }

    /**
     * Run all callbacks waiting for the load. Assumes that the load is finished; calling this before is undefined
     * behavior.
     */
    void runCallbacks() {
        T value = this.get();
        for (Consumer<T> callback : this.callbacks) // Sponge: Runnable -> Consumer<Cube>
        {
            callback.accept(value);
        }

        this.callbacks.clear();
    }

    /**
     * True if the target has been loaded and is available for use
     *
     * @return if this is finished
     */
    boolean isFinished() {
        return finished;
    }

    /**
     * Check if any callbacks are registered as waiting for this load.
     *
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
     *
     * @return The loaded object
     */
    @Nullable
    abstract T get();
}
