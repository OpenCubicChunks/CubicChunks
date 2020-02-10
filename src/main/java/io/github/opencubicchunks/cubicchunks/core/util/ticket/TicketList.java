/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.util.ticket;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TicketList {

    private final Cube cube;
    private int tickRefs = 0;
    @Nonnull private List<ITicket> tickets = Lists.newArrayListWithCapacity(1);

    // null cube means it's "BlankCube"
    public TicketList(@Nullable Cube cube) {
        this.cube = cube;
    }

    /**
     * Removes a ticket form this ticket list if present
     *
     * @param ticket the ticket to remove
     */
    public void remove(ITicket ticket) {
        if (cube == null) {
            return;
        }
        if (tickets.remove(ticket) && ticket.shouldTick()) {
            tickRefs--;
            assert tickRefs >= 0;
            if (tickRefs == 0) {
                ((ICubicWorldInternal.Server) cube.getWorld()).removeForcedCube(cube);
            }
        }
    }

    /**
     * Add a ticket to this ticket list if not already present
     *
     * @param ticket the ticket to add
     */
    public void add(ITicket ticket) {
        if (cube == null) {
            return;
        }
        if (tickets.contains(ticket)) {
            return; // we already have that ticket
        }
        tickets.add(ticket);
        tickRefs += ticket.shouldTick() ? 1 : 0; // keep track of the number of tickets that want to tick
        if (ticket.shouldTick()) {
            assert tickRefs > 0;
            if (tickRefs == 1) { // if it just got increased from zero
                ((ICubicWorldInternal.Server) cube.getWorld()).addForcedCube(cube);
            }
        }

    }

    /**
     * @param ticket the ticket to check for
     *
     * @return {@code true} if this list contains {@code ticket}, {@code false} otherwise
     */
    public boolean contains(ITicket ticket) {
        return tickets.contains(ticket);
    }

    /**
     * @return Should the world be ticking the Cube corresponding to this ticket list
     */
    public boolean shouldTick() {
        return tickRefs > 0;
    }

    /**
     * @return {@code true} if this cube can be unloaded, {@code false} otherwise
     */
    public boolean canUnload() {
        return tickets.isEmpty();
    }

    public boolean anyMatch(Predicate<ITicket> predicate) {
        return tickets.stream().anyMatch(predicate);
    }
}
