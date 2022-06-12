package io.github.artificial_intellicrafters.merlin_ai.api.util;

import java.util.NoSuchElementException;

//This file is a modified file from the JHeaps library, https://www.jheaps.org/
//This file is under the Apache-2.0 License which you can get a copy of  here https://www.apache.org/licenses/LICENSE-2.0

public class PathingHeap<V> {
	private Node<V> root;
	private long size;
	private PathingHeap<V> other;

	public PathingHeap() {
		root = null;
		size = 0;
		other = this;
	}

	public Node<V> insert(final double key, final V value) {
		final Node<V> n = new Node<>(this, key, value);
		root = link(root, n);
		size++;
		return n;
	}

	public Node<V> deleteMin() {
		if (size == 0) {
			throw new NoSuchElementException();
		}
		// assert root.o_s == null && root.y_s == null;

		final Node<V> oldRoot = root;

		// cut all children, combine them and overwrite old root
		root = combine(cutChildren(root));

		// decrease size
		size--;
		oldRoot.valid = false;
		return oldRoot;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public long size() {
		return size;
	}

	public void clear() {
		root = null;
		size = 0;
	}

	public static class Node<V> {
		private PathingHeap<V> heap;

		private double key;
		private V value;
		private Node<V> o_c;
		private Node<V> y_s;
		private Node<V> o_s;
		private boolean valid = true;

		private Node(final PathingHeap<V> heap, final double key, final V value) {
			this.heap = heap;
			this.key = key;
			this.value = value;
			o_c = null;
			y_s = null;
			o_s = null;
		}

		public boolean isValid() {
			return valid;
		}

		public double getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public void setValue(final V value) {
			this.value = value;
		}

		public void decreaseKey(final double newKey) {
			getOwner().decreaseKey(this, newKey);
		}

		public void delete() {
			getOwner().delete(this);
		}

		PathingHeap<V> getOwner() {
			if (heap.other != heap) {
				// find root
				PathingHeap<V> root = heap;
				while (root != root.other) {
					root = root.other;
				}
				// path-compression
				PathingHeap<V> cur = heap;
				while (cur.other != root) {
					final PathingHeap<V> next = cur.other;
					cur.other = root;
					cur = next;
				}
				heap = root;
			}
			return heap;
		}
	}

	private void decreaseKey(final Node<V> n, final double newKey) {
		final int c = Double.compare(newKey, n.key);


		if (c > 0) {
			throw new IllegalArgumentException("Keys can only be decreased!");
		}
		n.key = newKey;
		if (c == 0 || root == n) {
			return;
		}

		// unlink from parent
		if (n.y_s != null) {
			n.y_s.o_s = n.o_s;
		}
		if (n.o_s.o_c == n) { // I am the oldest :(
			n.o_s.o_c = n.y_s;
		} else { // I have an older sibling!
			n.o_s.y_s = n.y_s;
		}
		n.y_s = null;
		n.o_s = null;

		root = link(root, n);
	}

	/*
	 * Delete a node
	 */
	private void delete(final Node<V> n) {
		n.valid = false;
		if (root == n) {
			deleteMin();
			n.o_c = null;
			n.y_s = null;
			n.o_s = null;
			return;
		}

		// unlink from parent
		if (n.y_s != null) {
			n.y_s.o_s = n.o_s;
		}
		if (n.o_s.o_c == n) { // I am the oldest :(
			n.o_s.o_c = n.y_s;
		} else { // I have an older sibling!
			n.o_s.y_s = n.y_s;
		}
		n.y_s = null;
		n.o_s = null;

		// perform delete-min at tree rooted at this
		final Node<V> t = combine(cutChildren(n));

		root = link(root, t);

		size--;
	}

	/*
	 * Two pass pair and compute root.
	 */
	private Node<V> combine(final Node<V> l) {
		if (l == null) {
			return null;
		}

		// left-right pass
		Node<V> pairs = null;
		Node<V> it = l, p_it;
		while (it != null) {
			p_it = it;
			it = it.y_s;

			if (it == null) {
				// append last node to pair list
				p_it.y_s = pairs;
				p_it.o_s = null;
				pairs = p_it;
			} else {
				final Node<V> n_it = it.y_s;

				// disconnect both
				p_it.y_s = null;
				p_it.o_s = null;
				it.y_s = null;
				it.o_s = null;

				// link trees
				p_it = link(p_it, it);

				// append to pair list
				p_it.y_s = pairs;
				pairs = p_it;

				// advance
				it = n_it;
			}
		}

		// second pass (reverse order - due to add first)
		it = pairs;
		Node<V> f = null;
		while (it != null) {
			final Node<V> nextIt = it.y_s;
			it.y_s = null;
			f = link(f, it);
			it = nextIt;
		}

		return f;
	}

	private Node<V> cutChildren(final Node<V> n) {
		final Node<V> child = n.o_c;
		n.o_c = null;
		if (child != null) {
			child.o_s = null;
		}
		return child;
	}

	private Node<V> link(final Node<V> f, final Node<V> s) {
		if (s == null) {
			return f;
		} else if (f == null) {
			return s;
		} else if (Double.compare(f.key, s.key) <= 0) {
			s.y_s = f.o_c;
			s.o_s = f;
			if (f.o_c != null) {
				f.o_c.o_s = s;
			}
			f.o_c = s;
			return f;
		} else {
			return link(s, f);
		}
	}
}
