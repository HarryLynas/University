package uk.ac.reading.pm002501.mapreduce;

import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * This class provides generic functions that can be used.
 */
public abstract class FunctionHelper {
	/**
	 * This function takes a list of worker nodes and a root node. It waits
	 * until the processing of all jobs on all worker nodes is completed.
	 * 
	 * Once all workers have finished their jobs, the results of all workers are
	 * mapped onto the root node. The root node then reduces this data.
	 * 
	 * @param mappers
	 *            The worker nodes as a LinkedList.
	 * @param rootNode
	 *            The root node.
	 * @throws InterruptedException
	 *             Thrown if Thread.sleep(long); fails.
	 */
	@SuppressWarnings("unchecked")
	protected static <K, V, T extends MapReduce<K, V>, M extends MapJob<K, V>> void waitForJobsToComplete(
			LinkedList<T> mappers, M rootNode) throws InterruptedException {
		// Wait for all workers to terminate
		boolean running = true;
		while (running) {
			running = false;
			for (MapReduce<K, V> m : mappers) {
				if (m.isWorking() && !m.hasFinishedJob()) {
					running = true;
					Thread.sleep(100);
					break;
				}
			}
		}
		// Map the results, Shuffle/Sort
		for (MapReduce<K, V> mapper : mappers) {
			if (mapper.getResults() == null)
				continue;
			if (mapper.getResults().size() == 0) {
				if (mapper.getVResults() == null)
					continue;
				for (Entry<V, V> entry : mapper.getVResults())
					rootNode.map((K) entry.getKey(), entry.getValue());
			} else
				for (Entry<K, V> entry : mapper.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
		}
		// Reduce
		rootNode.reduce();
	}

	/**
	 * This function returns a LinkedList of MapReduce workers of a given type.
	 * Threads are automatically created and not kept track off. This means that
	 * to terminate the workers stop(); must be called on each one.
	 * 
	 * @param numWorkers
	 *            The number of workers to create.
	 * @return The LinkedList<MapReduce<K, V>> of workers running on individual
	 *         threads.
	 */
	protected static <K, V> LinkedList<MapReduce<K, V>> createWorkers(
			int numWorkers) {
		LinkedList<MapReduce<K, V>> mappers = new LinkedList<MapReduce<K, V>>();
		for (int i = 0; i < numWorkers; ++i) {
			MapReduce<K, V> m = new MapReduce<K, V>();
			mappers.add(m);
			new Thread(m).start();
		}
		return mappers;
	}

}
