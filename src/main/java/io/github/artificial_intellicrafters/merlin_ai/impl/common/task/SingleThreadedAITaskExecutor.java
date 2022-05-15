package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;

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
		return false;
	}

	@Override
	public void runTasks(final int maxMillis) {
		final long startMillis = System.currentTimeMillis();
		final List<AITask> finished = new ArrayList<>();
		while (System.currentTimeMillis() - startMillis <= maxMillis) {
			WrappedTask task;
			while (true) {
				task = taskQueue.peek();
				if (task == null) {
					return;
				}
				if (task.task().done()) {
					taskQueue.poll();
					finished.add(task.task());
				} else {
					break;
				}
			}
			task.task().runIteration();
		}
		for (final AITask task : finished) {
			task.runFinish();
		}
	}

	private record WrappedTask(AITask task, long order) implements Comparable<WrappedTask> {
		@Override
		public int compareTo(final SingleThreadedAITaskExecutor.WrappedTask o) {
			final int i = Integer.compare(task.priority(), o.task.priority());
			if (i != 0) {
				return i;
			}
			return Long.compare(order, o.order);
		}
	}
}
