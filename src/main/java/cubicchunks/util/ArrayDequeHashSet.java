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
package cubicchunks.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

public class ArrayDequeHashSet<E> implements Deque<E>, Set<E> {
	private Set<E> set = new HashSet<>();
	private Deque<E> deque = new ArrayDeque<>();

	@Override
	public void addFirst(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		if (set.add(e)) {
			deque.addFirst(e);
		} else {
			deque.remove(e);
			deque.addFirst(e);
		}
		assert set.size() == deque.size();
	}

	@Override
	public void addLast(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		if (set.add(e)) {
			deque.addLast(e);
		} else {
			deque.remove(e);
			deque.add(e);
		}
		assert set.size() == deque.size();
	}

	@Override
	public boolean offerFirst(E e) {
		addFirst(e);
		assert set.size() == deque.size();
		return true;
	}

	@Override
	public boolean offerLast(E e) {
		addLast(e);
		assert set.size() == deque.size();
		return true;
	}

	@Override
	public E removeFirst() {
		E e = deque.removeFirst();
		set.remove(e);
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public E removeLast() {
		E e = deque.removeLast();
		set.remove(e);
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public E pollFirst() {
		E e = deque.pollFirst();
		if (e != null) {
			set.remove(e);
		}
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public E pollLast() {
		E e = deque.pollLast();
		if (e != null) {
			set.remove(e);
		}
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public E getFirst() {
		return deque.getFirst();
	}

	@Override
	public E getLast() {
		return deque.getLast();
	}

	@Override
	public E peekFirst() {
		return deque.peekFirst();
	}

	@Override
	public E peekLast() {
		return deque.peekLast();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		set.remove(o);
		boolean b = deque.removeFirstOccurrence(o);
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		//there is only one occurrence of any element
		return removeFirstOccurrence(o);
	}

	@Override
	public boolean add(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		boolean b = set.add(e) && deque.add(e);
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public boolean offer(E e) {
		return offerLast(e);
	}

	@Override
	public E remove() {
		E removed = deque.remove();
		set.remove(removed);
		assert set.size() == deque.size();
		return removed;
	}

	@Override
	public E poll() {
		E e = deque.poll();
		if (e != null)
			set.remove(e);
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public E element() {
		return deque.element();
	}

	@Override
	public E peek() {
		return deque.peek();
	}

	@Override
	public void push(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		if (set.add(e)) {
			deque.push(e);
		}
		assert set.size() == deque.size();
	}

	@Override
	public E pop() {
		E e = deque.pop();
		set.remove(e);
		assert set.size() == deque.size();
		return e;
	}

	@Override
	public boolean remove(Object o) {
		boolean b;
		if (b = set.remove(o)) {
			deque.remove(o);
		}
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public boolean containsAll(@Nonnull Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(@Nonnull Collection<? extends E> c) {
		for (E e : c) {
			if (e == null) {
				throw new NullPointerException();
			}
			if (!set.contains(e)) {
				deque.add(e);
			}
		}
		boolean b = set.addAll(c);
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public boolean removeAll(@Nonnull Collection<?> c) {
		boolean b = set.removeAll(c);
		boolean b2 = deque.removeAll(c);
		assert b == b2;
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public boolean retainAll(@Nonnull Collection<?> c) {
		boolean b = set.retainAll(c);
		if (b) {
			deque.retainAll(c);
		}
		assert set.size() == deque.size();
		return b;
	}

	@Override
	public void clear() {
		deque.clear();
		set.clear();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public int size() {
		assert set.size() == deque.size();
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		assert set.isEmpty() == deque.isEmpty();
		return set.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			Iterator<E> dequeIt = deque.iterator();
			E lastNext;

			@Override
			public boolean hasNext() {
				return dequeIt.hasNext();
			}

			@Override
			public E next() {
				return lastNext = dequeIt.next();
			}

			@Override
			public void remove() {
				set.remove(lastNext);
				dequeIt.remove();
				assert set.size() == deque.size();
			}
		};
	}

	@Override
	public Object[] toArray() {
		return deque.toArray();
	}

	@Override
	public <T> T[] toArray(@Nonnull T[] a) {
		return deque.toArray(a);
	}

	@Override
	public Iterator<E> descendingIterator() {
		return new Iterator<E>() {
			Iterator<E> dequeIt = deque.descendingIterator();
			E lastNext;

			@Override
			public boolean hasNext() {
				return dequeIt.hasNext();
			}

			@Override
			public E next() {
				return lastNext = dequeIt.next();
			}

			@Override
			public void remove() {
				set.remove(lastNext);
				dequeIt.remove();
				assert set.size() == deque.size();
			}
		};
	}
}
