package uk.ac.reading.pm002501.mapreduce;

import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is used to store many fields within a single object that are populated over time.
 */
public class Accumulator {
	/**
	 * The fields of the accumulator.
	 */
	public volatile String[] fields;

	/**
	 * Initialise an accumulator with a number of fields.
	 * @param numFields The number of fields for this record.
	 */
	public Accumulator(int numFields) {
		fields = new String[numFields];
		// Initialise values
		for (int i = 0; i < numFields; ++i)
			fields[i] = "";
	}

	/**
	 * Merge two accumulators into a single object.
	 * @param val The accumulator to merge with.
	 * @return The merged accumulator.
	 */
	public Accumulator merge(Accumulator val) {
		for (int i = 0; i < fields.length; ++i)
			fields[i] = val.fields[i].length() > 0 ? val.fields[i] : fields[i];
		return this;
	}

	/**
	 * Sets the first empty field to a piece of data.
	 * @param val The value to set.
	 * @throws Exception An exception is thrown if all
	 * the fields have already been filled.
	 */
	public void setVal(String val) throws Exception {
		for (int i = 0; i < fields.length; ++i) {
			if (fields[i].length() == 0) {
				fields[i] = val;
				return;
			}
		}
		throw new Exception("All fields already have data.");
	}

	/**
	 * Returns a unique ID by merging all the fields into
	 * a single string. This is used during reduce as the
	 * key value.
	 * @return The unique ID for this accumulator.
	 */
	public String getUniqueId() {
		StringBuilder builder = new StringBuilder();
		for (String field : fields)
			builder.append(field);
		return builder.toString();
	}

	/**
	 * Format the fields into a pretty string detailing the contents.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		for (String field : fields)
			builder.append(field + ", ");
		builder.delete(builder.length() - 2, builder.length());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Merge two locations into the fields of the accumulator.
	 * @param departed The departed location.
	 * @param arrived The arrived location.
	 * @return The accumulator with the locations inserted into the fields.
	 */
	public Accumulator mergeLocation(Location departed, Location arrived) {
		synchronized (fields) {
			if (arrived == null) {
				if (departed == null)
					departed = new Location("", "");
				arrived = departed;
			} else if (departed == null)
				departed = arrived;
			String[] oldData = fields.clone();
			fields = new String[fields.length + 4];
			for (int i = 0; i < oldData.length; ++i)
				fields[i] = oldData[i];
			fields[oldData.length] = departed.latitude;
			fields[oldData.length + 1] = departed.longitude;
			fields[oldData.length + 2] = arrived.latitude;
			fields[oldData.length + 3] = arrived.longitude;
			return this;
		}
	}

	/**
	 * Inserts a new field into the accumulator that will contain the number
	 * of miles travelled by this flight if it is possible to calculate from
	 * the known data.
	 * 
	 * @param milesTravelledPerFlight A list of all flights and the distance
	 * they have travelled. The data to insert into this accumulator is looked
	 * up in this data set.
	 * @return The accumulator with the miles flown inserted.
	 */
	public Accumulator addDistance(
			Set<Entry<Accumulator, String>> milesTravelledPerFlight) {
		synchronized (fields) {
			String[] oldData = fields.clone();
			fields = new String[fields.length + 1];
			for (int i = 0; i < oldData.length; ++i)
				fields[i] = oldData[i];
			
			fields[oldData.length] = "";
			for (Entry<Accumulator, String> entry : milesTravelledPerFlight) {
				if (entry.getKey().equals(this)) {
					fields[oldData.length] = entry.getValue();
					break;
				}
			}
			
			return this;
		}
	}
}
