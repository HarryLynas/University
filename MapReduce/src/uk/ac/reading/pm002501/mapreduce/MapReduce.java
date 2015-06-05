package uk.ac.reading.pm002501.mapreduce;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class acts as a worker node that maps and reduces data.
 * 
 * @param <K> The key data type.
 * @param <V> The value data type.
 */
public class MapReduce<K, V> implements Runnable {
	
	private K key = null;
	@SuppressWarnings("rawtypes")
	private volatile MapJob job = null;
	private volatile boolean running = true;
	private volatile boolean finishedJob = false;
	private STATE state;
	private Object lockObject = new Object();
	
	public enum STATE {
		READ_FILE_1,
		READ_FILE_2,
		GET_NUM_DEPARTURES_1,
		GET_NUM_DEPARTURES_2,
		GET_NUM_PASSENGERS_PER_FLIGHT,
		CONVERT_TIME_1,
		CONVERT_TIME_2,
		CONVERT_AIRPORT_TO_LOCATION,
		GET_TRAVELLED_MILES,
		GET_TRAVELLED_MILES_PAS
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		while (running) {
			synchronized(lockObject) {
				if (job != null && !finishedJob) {
					if (state == STATE.READ_FILE_1) {
						Accumulator line = new Accumulator(6);
						for (String word : ((String)key).split(","))
							job.map(word, line);
					} else if (state == STATE.READ_FILE_2) {
						Accumulator line = new Accumulator(4);
						for (String word : ((String)key).split(","))
							job.map(word, line);
					} else if (state == STATE.GET_NUM_DEPARTURES_1)
						job.map(key, 1);
					else if (state == STATE.GET_NUM_DEPARTURES_2)
						job.map(key, 0);
					else if (state == STATE.GET_NUM_PASSENGERS_PER_FLIGHT)
						job.map(key, 1);
					else if (state == STATE.CONVERT_TIME_1) {
						Accumulator a = (Accumulator) key;
						// fields[5] is in minutes and must be converted to seconds by * 60
						// the Date(long) constructor takes ms not seconds to * 1000
						if (a.fields[4].length() == 0 || a.fields[5].length() == 0)
							job.map(key, "");
						else {
							long time = (Long.parseLong(a.fields[4]) - (Long.parseLong(a.fields[5]) * 60)) * 1000;
							job.map(key, new Date(time).toString());
						}
					} else if (state == STATE.CONVERT_TIME_2) {
						Accumulator a = (Accumulator) key;
						if (a.fields[4].length() == 0)
							job.map(key, "");
						else {
							// the Date(long) constructor takes ms not seconds to * 1000
							long time = Long.parseLong(a.fields[4]) * 1000;
							job.map(key, new Date(time).toString());
						}
					} else if (state == STATE.CONVERT_AIRPORT_TO_LOCATION) {
						Accumulator a = (Accumulator) key;
						// Get location
						Location loc = new Location(a.fields[2], a.fields[3]);
						job.map(a.fields[1], loc);
					} else if (state == STATE.GET_TRAVELLED_MILES) {
						Accumulator a = (Accumulator) key;
						// Distance in miles
						double _d = 0.0;
						try {
							double lat1 = Double.parseDouble(a.fields[a.fields.length - 4]);
							double lon1 = Double.parseDouble(a.fields[a.fields.length - 3]);
							double lat2 = Double.parseDouble(a.fields[a.fields.length - 2]);
							double lon2 = Double.parseDouble(a.fields[a.fields.length - 1]);
							_d = distance(lat1, lon1, lat2, lon2);
						} catch (NumberFormatException e) {
						}
						job.map(a, String.valueOf(_d));
					} else if (state == STATE.GET_TRAVELLED_MILES_PAS) {
						Accumulator a = (Accumulator) key;
						job.map(a.fields[0], Double.parseDouble(a.fields[a.fields.length - 1]));
					}
					job.reduce();
					finishedJob = true;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This function calculates the miles between two lat long locations.
	 * 
	 * @param lat1 Latitude 1
	 * @param lon1 Longitude 1
	 * @param lat2 Latitude 2
	 * @param lon2 Longitude 2
	 * @return The approximate miles between the two input locations.
	 * 
	 * @author Function taken from: http://www.geodatasource.com/developers/java
	 */
	private double distance(double lat1, double lon1, double lat2, double lon2) {
		// Handle same location
		if (lat1 == lat2 && lon1 == lon2)
			return 0;
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return (dist);
	}

	private double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
	
	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}
	
	/**
	 * Add a job for this worker to process. The worker will
	 * begin processing this job almost immediately and another
	 * job should not be added while a job is already in process.
	 * 
	 * You can check if a job is already running with the
	 * isWorking() and hasFinishedJob() functions.
	 * 
	 * @param key The key value to map.
	 * @param state The state of the job.
	 */
	public void addJob(K key, STATE state) {
		synchronized(lockObject) {
			finishedJob = false;
			this.key = key;
			this.state = state;
			if (state == STATE.READ_FILE_1 || state == STATE.READ_FILE_2)
				job = new MapJob<String, Accumulator>();
			else if (state == STATE.GET_NUM_DEPARTURES_1 || state == STATE.GET_NUM_DEPARTURES_2)
				job = new MapJob<String, Integer>();
			else if (state == STATE.GET_NUM_PASSENGERS_PER_FLIGHT)
				job = new MapJob<String, Integer>();
			else if (state == STATE.CONVERT_TIME_1 || state == STATE.CONVERT_TIME_2)
				job = new MapJob<Accumulator, String>();
			else if (state == STATE.CONVERT_AIRPORT_TO_LOCATION)
				job = new MapJob<String, Location>();
			else if (state == STATE.GET_TRAVELLED_MILES)
				job = new MapJob<Accumulator, String>();
			else if (state == STATE.GET_TRAVELLED_MILES_PAS)
				job = new MapJob<String, Double>();
		}
	}
	
	/**
	 * Call this to stop the thread from running.
	 */
	public void stop() {
		running = false;
	}
	
	/**
	 * Returns true if the worker currently has a job.
	 * @return True if the worker currently has a job.
	 */
	public boolean isWorking() {
		return job != null;
	}
	
	/**
	 * Returns true if the worker has finished the job it has.
	 * @return True if the worker has finished the job it has.
	 */
	public boolean hasFinishedJob() {
		return finishedJob;
	}
	
	/**
	 * Return results where the value data type is only used.
	 * @return Returns results where the value data type is only used.
	 */
	@SuppressWarnings("unchecked")
	public Set<Entry<V, V>> getVResults() {
		if (job == null)
			return null;
		return job.getVResults();
	}
	
	/**
	 * Return the normal key value results after reduce has been called.
	 * @return The normal key value results.
	 */
	@SuppressWarnings("unchecked")
	public Set<Entry<K, V>> getResults() {
		if (job == null)
			return null;
		return job.getResults();
	}
}
