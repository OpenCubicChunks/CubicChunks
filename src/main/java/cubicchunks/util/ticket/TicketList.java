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

import java.util.LinkedList;

public class TicketList {

	private boolean tick = false;
	private LinkedList<ITicket> tickets = new LinkedList<>();

	/**
	 * Removes a ticket form this tickets
	 *
	 * @param ticket the ticket to remove
	 */
	public void remove(ITicket ticket){
		tickets.remove(ticket);
		scanShouldTick();
	}

	/**
	 * Add a ticket to this tickets
	 *
	 * @param ticket the ticket to add
	 */
	public void add(ITicket ticket){
		if(tickets.contains(ticket)){
			return; // we already have that ticket
		}
		tickets.add(ticket);
		tick |= ticket.shouldTick(); // no need to scan the whole list when adding
	}

	/**
	 * @param ticket the ticket we want to see if is in this tickets
	 * @return Does this ticket tickets contain {@Code ticket}
	 */
	public boolean contains(ITicket ticket){
		return tickets.contains(ticket);
	}

	/**
	 * @return Should the world be ticking the Cube corresponding to this ticket tickets
	 */
	public boolean shouldTick(){
		return tick;
	}

	/**
	 * @return Weather or not this ticket tickets permits unloading
	 */
	public boolean canUnload(){
		return tickets.isEmpty();
	}

	private void scanShouldTick(){
		for(ITicket ticket : tickets){
			if(ticket.shouldTick()){
				tick = true;
				return;
			}
		}
		tick = false;
	}
}
