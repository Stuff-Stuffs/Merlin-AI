package io.github.artificial_intellicrafters.merlin_ai.api.util;

import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeaps;

import java.util.Comparator;
import java.util.function.Predicate;

public class PathingHeapQueue<K> extends ObjectHeapPriorityQueue<K> {
	public PathingHeapQueue(final int capacity, final Comparator<? super K> c) {
		super(capacity, c);
	}

	public PathingHeapQueue(final Comparator<? super K> c) {
		super(c);
	}

	public void removeFirstReference(final K old) {
		final K[] heap = this.heap;
		for (int i = 0; i < heap.length; i++) {
			if (old == heap[i]) {
				heap[i] = heap[--size];
				heap[size] = null;
				if (size != 0) {
					ObjectHeaps.downHeap(heap, size, i, c);
				}
				return;
			}
		}
	}

	public void removeFirst(final Predicate<K> predicate) {
		final K[] heap = this.heap;
		int size = size();
		for (int i = 0; i < heap.length; i++) {
			if (predicate.test(heap[i])) {
				heap[i] = heap[--size];
				heap[size] = null;
				if (size != 0) {
					ObjectHeaps.downHeap(heap, size, i, c);
				}
				return;
			}
		}
	}
}
