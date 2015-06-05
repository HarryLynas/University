package uk.ac.reading.pm002501.mapreduce;

/**
 * This class represents a key and value pair.
 *
 * @param <K> The key data type.
 * @param <V> The value data type.
 */
public class KeyValuePair<K, V> {

	public K key = null;
	public V value = null;

	public KeyValuePair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Add two key value pairs together. This function handles many different data
	 * types and adds them differently depending on what data type the key and value
	 * is. If the data types are not handled in this function, null will be returned.
	 * 
	 * @param val The value to add to this key pair value.
	 * @return The result of adding the key value pairs value together. Can return null.
	 */
	@SuppressWarnings("unchecked")
	public V add(V val) {
		try {
			if (value instanceof Integer && val instanceof Integer) {
				int temp = (int) value + (int) val;
				return (V) ((Object) temp);
			} else if (value instanceof Double && val instanceof Double) {
				double temp = (double) value + (double) val;
				if (Double.isNaN(temp)) {
					temp = 0;
					if (!Double.isNaN((double) value))
						temp += (double) value;
					else if (!Double.isNaN((double) val))
						temp += (double) val;
				}
				return (V) ((Object) temp);
			} else if (value instanceof String && val instanceof String) {
				String temp = (String) value + (String) val;
				return (V) (temp);
			} else if (value instanceof Accumulator
					&& val instanceof Accumulator) {
				Accumulator temp = ((Accumulator) value)
						.merge((Accumulator) val);
				return (V) (temp);
			}
		} catch (ClassCastException e) {
		}
		return null;
	}
}
