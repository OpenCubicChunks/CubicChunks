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

import cubicchunks.world.cube.Cube;

import java.util.Comparator;
import java.util.LinkedList;

public class TicketList {

	private static final Comparator<ITicket> ORDER = (one, two) -> {
		if(one.shouldTick() && !two.shouldTick()){
			return -1;
		}else if(!one.shouldTick() && two.shouldTick()){
			return 1;
		}
		return 0;
	};

	private LinkedList<ITicket> list = new LinkedList<>();

	public TicketList(){
		super();
	}

	public void remove(ITicket ticket){
		list.remove(ticket);
	}

	public void add(ITicket ticket){
		list.add(ticket);
		list.sort(ORDER);
	}

	public boolean contains(ITicket ticket){
		return list.contains(ticket);
	}

	public boolean shouldTick(){
		ITicket first = list.getFirst(); // Note: things are sorted
		return first != null && first.shouldTick();
	}

	public boolean canUnload(){
		return list.isEmpty();
	}

	public void forceUnload(Cube cube){
		list.forEach((ticket) -> {
			ticket.forceUnload(cube);
		});
	}
}
