package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class SingleThreadedAITaskExecutor implements AITaskExecutor {
	private static final int MAX_ATTEMPTS = 64;
	private final int maxWaitingTasks;
	private final TaskQueue queue = new TaskQueue();

	public SingleThreadedAITaskExecutor(final int tasks) {
		maxWaitingTasks = tasks;
	}

	@Override
	public Optional<AITaskExecutionContext> submitTask(final AITask task) {
		if (queue.length < maxWaitingTasks) {
			return Optional.of(new AITaskExecutionContextImpl(queue.push(task)));
		}
		return Optional.empty();
	}

	@Override
	public Optional<AITaskExecutionContext> submitTaskBefore(final AITask task, final AITaskExecutionContext executionContext) {
		if (executionContext.valid()) {
			final AITaskExecutionContextImpl impl = (AITaskExecutionContextImpl) executionContext;
			if (queue.length + 1 >= maxWaitingTasks) {
				final Node node = queue.popEnd();
				node.task.cancel();
				if (MerlinAI.DEBUG) {
					System.out.println("Canceled task " + node.task + ", took " + node.duration + "ms");
				}
			}
			return Optional.of(new AITaskExecutionContextImpl(queue.insertBefore(task, impl.node)));
		}
		return Optional.empty();
	}

	@Override
	public Optional<AITaskExecutionContext> submitTaskAfter(final AITask task, final AITaskExecutionContext executionContext) {
		if (executionContext.valid()) {
			final AITaskExecutionContextImpl impl = (AITaskExecutionContextImpl) executionContext;
			if (queue.length + 1 >= maxWaitingTasks) {
				return Optional.empty();
			}
			return Optional.of(new AITaskExecutionContextImpl(queue.insertAfter(task, impl.node)));
		}
		return Optional.empty();
	}

	@Override
	public void runTasks(final int maxMillis) {
		final List<Node> finished = new ArrayList<>();
		final List<Node> canceled = new ArrayList<>();
		while (queue.length > 0) {
			final AITask task = queue.head.task;
			if (task.done()) {
				finished.add(queue.pop());
			} else {
				if (queue.head.attempts > MAX_ATTEMPTS) {
					canceled.add(queue.pop());
				} else {
					if (MerlinAI.DEBUG) {
						final long preMillis = System.currentTimeMillis();
						task.runIteration();
						queue.head.duration += System.currentTimeMillis() - preMillis;
					} else {
						task.runIteration();
					}
					queue.head.attempts++;
					if (task.done()) {
						finished.add(queue.pop());
					}
				}
			}
		}
		for (final Node task : finished) {
			task.task.runFinish();
			if (MerlinAI.DEBUG) {
				System.out.println("Finished task " + task.task + ", took " + task.duration + "ms");
			}
		}
		for (final Node task : canceled) {
			task.task.cancel();
			if (MerlinAI.DEBUG) {
				System.out.println("Canceled task " + task.task + ", took " + task.duration + "ms");
			}
		}
	}

	private static final class AITaskExecutionContextImpl implements AITaskExecutionContext {
		private final Node node;

		private AITaskExecutionContextImpl(final Node node) {
			this.node = node;
		}

		@Override
		public boolean valid() {
			return !node.invalidated;
		}
	}

	private static final class TaskQueue {
		private final Object parentKey = new Object();
		private int length;
		private Node head;
		private Node tail;

		public Node popEnd() {
			if (length == 0) {
				throw new NoSuchElementException();
			}
			if(head==tail) {
				Node h = head;
				h.invalidated = true;
				head = tail = null;
				length--;
				return h;
			}
			final Node t = tail;
			tail = tail.before;
			if (tail != null) {
				tail.after = null;
			}
			t.invalidated = true;
			length--;
			return t;
		}

		public Node pop() {
			if (length == 0) {
				throw new NoSuchElementException();
			}
			if(head==tail) {
				Node h = head;
				h.invalidated = true;
				head = tail = null;
				length--;
				return h;
			}
			final Node h = head;
			head = h.after;
			if (head != null) {
				head.before = null;
			}
			h.invalidated = true;
			length--;
			return h;
		}

		public Node push(final AITask task) {
			final Node node = new Node(task, parentKey);
			node.invalidated = false;
			if (tail != null) {
				tail.after = node;
				node.before = tail;
				tail = node;
				length += 1;
			} else {
				head = tail = node;
				length = 1;
			}
			return node;
		}

		public Node insertBefore(final AITask task, final Node node) {
			if (node.parentKey != parentKey || node.invalidated) {
				throw new RuntimeException();
			}
			final Node n = new Node(task, parentKey);
			n.invalidated = false;
			if (node == head) {
				n.after = head;
				head.before = n;
				head = n;
			} else {
				final Node before = node.before;
				if (before != null) {
					before.after = n;
				}
				n.before = before;
				node.before = n;
				n.after = node;
			}
			length++;
			return n;
		}

		public Node insertAfter(final AITask task, final Node node) {
			if (node.parentKey != parentKey || node.invalidated) {
				throw new RuntimeException();
			}
			final Node n = new Node(task, parentKey);
			n.invalidated = false;
			if (node == tail) {
				n.before = tail;
				tail.after = n;
				tail = n;
			} else {
				final Node after = node.after;
				if (after != null) {
					after.before = n;
				}
				n.after = after;
				node.after = n;
				n.before = node;
			}
			length++;
			return n;
		}
	}

	private static final class Node {
		private final AITask task;
		private final Object parentKey;
		private Node before;
		private Node after;
		private int attempts = 0;
		private long duration = 0;
		private boolean invalidated = true;

		private Node(final AITask task, final Object key) {
			this.task = task;
			parentKey = key;
		}
	}
}
