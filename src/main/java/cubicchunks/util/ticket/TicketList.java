/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.util.ticket;

import java.util.HashSet;
import java.util.Set;

public class TicketList {

	private boolean tick = false;
	private int tickRefs = 0;
	private Set<ITicket> tickets = new HashSet<>();

	/**
	 * Removes a ticket form these tickets if present
	 *
	 * @param ticket the ticket to remove
	 */
	public void remove(ITicket ticket) {
		if (tickets.remove(ticket) && ticket.shouldTick()) {
			tickRefs--;
		}
	}

	/**
	 * Add a ticket to these tickets if not already present
	 *
	 * @param ticket the ticket to add
	 */
	public void add(ITicket ticket) {
		if (tickets.add(ticket) && ticket.shouldTick()) {
			tickRefs++;
		}
	}

	/**
	 * @param ticket the ticket we want to see if is in these tickets
	 *
	 * @return <code>true</code> if this list contained {@code ticket}, <code>false</code> otherwise
	 */
	public boolean contains(ITicket ticket) {
		return tickets.contains(ticket);
	}

	/**
	 * Check if there are any tickets specifying that they want this cube to be ticked
	 * @return <code>true</code> if this cube can be ticked, false otherwise
	 */
	public boolean shouldTick() {
		return tickRefs > 0;
	}

	/**
	 * Check if there are any tickets preventing this cube from being unloaded
	 * @return <code>true</code> if this cube can be unloaded, <code>false</code> otherwise
	 */
	public boolean canUnload() {
		return tickets.isEmpty();
	}
}
