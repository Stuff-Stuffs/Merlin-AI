package io.github.artificial_intellicrafters.merlin_ai.api.util;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import java.util.Comparator;
import java.util.NoSuchElementException;

class PathingHeapQueue<K extends AStar.WrappedPathNode<?>> {
	private int size = 0;
	private Object[] heap = new Object[0];
	private final Comparator<? super K> c;

	public PathingHeapQueue(final Comparator<? super K> c) {
		this.c = c;
	}

	public void removeFirstReference(final K old) {
		if (old.index != -1) {
			if (size == 1) {
				dequeue();
			} else {
				heap[old.index] = heap[--size];
				((K)(heap[old.index])).index = old.index;
				downHeap(heap, size, old.index);
				old.index = -1;
			}
		}
	}

	public boolean isEmpty() {
		return size ==0;
	}

	public void enqueue(final K x) {
		if (size == heap.length) {
			heap = ObjectArrays.grow(heap, size + 1);
		}
		heap[size++] = x;
		upHeap(heap, size, size - 1);
	}

	public K dequeue() {
		if (size == 0) {
			throw new NoSuchElementException();
		}
		final K result = (K) heap[0];
		heap[0] = heap[--size];
		heap[size] = null;
		if (size != 0) {
			((K)(heap[0])).index = 0;
			downHeap(heap, size, 0);
		}
		result.index = -1;
		return result;
	}

	public void downHeap(final Object[] heap, final int size, int i) {
		assert i < size;
		final K e = (K) heap[i];
		int child;
		while ((child = (i << 1) + 1) < size) {
			K t = (K) heap[child];
			final int right = child + 1;
			if (right < size && c.compare((K)heap[right], t) < 0) {
				t = (K) heap[child = right];
			}
			if (c.compare(e, t) <= 0) {
				break;
			}
			heap[i] = t;
			t.index = i;
			i = child;
		}
		heap[i] = e;
		e.index = i;
	}

	public void upHeap(final Object[] heap, final int size, int i) {
		assert i < size;
		final K e = (K) heap[i];
		while (i != 0) {
			final int parent = (i - 1) >>> 1;
			final K t = (K) heap[parent];
			if (c.compare(t, e) <= 0) {
				break;
			}
			heap[i] = t;
			t.index = i;
			i = parent;
		}
		heap[i] = e;
		e.index = i;
	}
}
