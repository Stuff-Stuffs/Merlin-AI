package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class SingleThreadedAITaskExecutor implements AITaskExecutor {
	private final PriorityQueue<WrappedTask> taskQueue;
	private final int maxWaitingTasks;
	private long order = 0;

	public SingleThreadedAITaskExecutor(final int maxWaitingTasks) {
		this.maxWaitingTasks = maxWaitingTasks;
		taskQueue = new PriorityQueue<>(maxWaitingTasks);
	}

	@Override
	public boolean submitTask(final AITask task) {
		if (taskQueue.size() < maxWaitingTasks) {
			taskQueue.add(new WrappedTask(task, order++));
			return true;
		}
		if (!taskQueue.isEmpty() && taskQueue.peek().task.priority() > task.priority()) {
			taskQueue.poll();
			taskQueue.add(new WrappedTask(task, order++));
		}
		return false;
	}

	@Override
	public void runTasks(final int maxMillis) {
		final long startMillis = System.currentTimeMillis();
		final List<WrappedTask> finished = new ArrayList<>();
		while (System.currentTimeMillis() - startMillis <= maxMillis) {
			WrappedTask task;
			while (true) {
				task = taskQueue.peek();
				if (task == null) {
					break;
				}
				if (task.task.done()) {
					taskQueue.poll();
					finished.add(task);
				} else {
					break;
				}
			}
			if (task != null) {
				if (MerlinAI.DEBUG) {
					final long preMillis = System.currentTimeMillis();
					task.task.runIteration();
					task.duration += System.currentTimeMillis() - preMillis;
				} else {
					task.task.runIteration();
				}
			} else {
				break;
			}
		}
		for (final WrappedTask task : finished) {
			task.task.runFinish();
			if (MerlinAI.DEBUG) {
				System.out.println("Finished task " + task + ", took " + task.duration + "ms");
			}
		}
	}

	private static final class WrappedTask implements Comparable<WrappedTask> {
		private final AITask task;
		private final long order;
		private long duration = 0;

		private WrappedTask(final AITask task, final long order) {
			this.task = task;
			this.order = order;
		}

		@Override
		public int compareTo(final WrappedTask o) {
			final int i = Integer.compare(task.priority(), o.task.priority());
			if (i != 0) {
				return i;
			}
			return Long.compare(order, o.order);
		}

		@Override
		public String toString() {
			return "WrappedTask[" + "task=" + task + ", " + "order=" + order + ']';
		}
	}
}
