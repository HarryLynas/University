package uk.ac.reading.pm002501.mapreduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class handles the primary map, reduce, and combine functions of MapReduce.
 * 
 * This class is populated and called from the MapReduce class.
 *
 * @param <K> The key data type.
 * @param <V> The value data type.
 */
public class MapJob<K, V> {
	/**
	 * Store for all key value pairs during mapping.
	 */
	private volatile ArrayList<KeyValuePair<K, V>> mapPairs = new ArrayList<KeyValuePair<K, V>>();

	/**
	 * Call this function to map a key value pair.
	 * @param key The key value.
	 * @param value The value.
	 */
	public void map(K key, V value) {
		KeyValuePair<K, V> pair = new KeyValuePair<K, V>(key, value);
		mapPairs.add(pair);
	}

	/**
	 * Call this function to reduce the key value pairs into a result set.
	 */
	public void reduce() {
		for (KeyValuePair<K, V> pair : mapPairs)
			combine(pair);
	}

	private HashMap<V, V> reduceVMap = new HashMap<V, V>();
	private HashMap<K, V> reduceMap = new HashMap<K, V>();

	/**
	 * This function is called on each key value pair to merge results
	 * value where the key is the same into a single record.
	 * @param pair The key value pair to process.
	 */
	private void combine(KeyValuePair<K, V> pair) {
		if (pair.value instanceof Accumulator) {
			Accumulator a = (Accumulator) pair.value;
			if (pair.key instanceof String)
				try {
					a.setVal(pair.key.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			V value = reduceVMap.get(pair.key);
			if (value != null)
				reduceVMap.put(pair.add(value), pair.add(value));
			else
				reduceVMap.put(pair.value, pair.value);
			return;
		}
		if (pair.value instanceof Integer) {
			V value = reduceMap.get(pair.key);
			if (value != null)
				reduceMap.put(pair.key, pair.add(value));
			else
				reduceMap.put(pair.key, pair.value);
			return;
		}
		if (pair.value instanceof String) {
			Accumulator a = (Accumulator) pair.key;
			if (a.fields.length > 6) {
				reduceMap.put(pair.key, pair.value);
				return;
			}
			String temp = a.fields[5];
			boolean error = false;
			try {
				Long.parseLong(temp);
			} catch (NumberFormatException e) {
				error = true;
			}
			if (!error)
				a.fields[5] = pair.value.toString();
			else
				a.fields[4] = pair.value.toString();
			reduceMap.put(pair.key, null);
			return;
		} else if (pair.value instanceof Location) {
			reduceMap.put(pair.key, pair.value);
			return;
		} else if (pair.value instanceof Double) {
			V value = reduceMap.get(pair.key);
			if (value != null)
				reduceMap.put(pair.key, pair.add(value));
			else
				reduceMap.put(pair.key, pair.value);
			return;
		} else if (pair.value == null) {
			reduceMap.put(pair.key, null);
			return;
		}
		throw new ClassCastException("Class not supported!");
	}

	/**
	 * Get the results where only the value is used.
	 * @return Get results where only the value is used.
	 */
	public Set<Entry<V, V>> getVResults() {
		return reduceVMap.entrySet();
	}

	/**
	 * Get the normal results from reduce.
	 * @return The normal results from reduce.
	 */
	public Set<Entry<K, V>> getResults() {
		return reduceMap.entrySet();
	}
}
