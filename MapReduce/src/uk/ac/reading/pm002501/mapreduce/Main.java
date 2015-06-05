package uk.ac.reading.pm002501.mapreduce;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import uk.ac.reading.pm002501.mapreduce.MapReduce.STATE;

public class Main extends FunctionHelper {
	/**
	 * The number of threads to use, each thread is a single worker node.
	 */
	private final static int numWorkers = 50;

	/**
	 * Called when the program is started up.
	 * @param args The arguments must contain the two files necessary.
	 * @throws Exception A serious exception will be passed all the way
	 * to main and terminate the program.
	 */
	public static void main(String[] args) throws Exception {
		// Get paths from arguments
		if (args.length != 2) {
			Log.log("ERROR: You must specify the AComp_Passenger_data_no_error.csv and the "
					+ "Top30_airports_LatLong files.csv files.");
			return;
		}

		// Read files into memory
		String path = args[0];
		String path2 = args[1];
		Log.log("Reading files: \n" + path + ",\n" + path2 + "\n");

		List<String> lines = Files.readAllLines(Paths.get(path),
				Charset.defaultCharset());
		List<String> lines2 = Files.readAllLines(Paths.get(path2),
				Charset.defaultCharset());

		Log.log("Processing input files...");
		// Map every input
		Accumulator[] data = getInputData(lines, 6);
		Accumulator[] data2 = getInputData(lines2, 4);

		Log.log("");
		Log.log("Getting number of flights from each airport...");
		// Get number of flights from each airport
		Set<Entry<String, Integer>> numFlights = getAirportDeparturesNum(data,
				data2);
		for (Entry<String, Integer> entry : numFlights)
			Log.log("[" + entry.getKey() + "] = " + entry.getValue());

		Log.log("");
		Log.log("Getting number of passengers on each flight...");
		// Get number of passengers on each flight
		Set<Entry<String, Integer>> numPassengers = getPassengersPerFlightNum(data);
		for (Entry<String, Integer> entry : numPassengers)
			Log.log("[" + entry.getKey() + "] = " + entry.getValue());
		
		Log.log("");
		Log.log("Converting times into standard string format...");
		data = convertInputTimes(data);
		for (Accumulator a : data)
			Log.log(a.toString());
		
		Log.log("");
		Log.log("Getting airport location data...");
		HashMap<String, Location> airportLocations = getAirportLocationMap(data2);
		Log.log("Getting miles travelled per flight...");
		Set<Entry<Accumulator, String>> milesTravelledPerFlight = getMilesTravelledPerFlight(data, airportLocations);
		for (Entry<Accumulator, String> entry : milesTravelledPerFlight)
			Log.log(entry.getKey().toString() + " = " + entry.getValue());
		
		Log.log("");
		Log.log("Getting miles travelled per passenger...");
		Set<Entry<String, Double>> milesTravelledPerPassenger = getMilesTravelledPerPassenger(data, milesTravelledPerFlight);
		for (Entry<String, Double> entry : milesTravelledPerPassenger)
			Log.log("[" + entry.getKey().toString() + "] = " + entry.getValue());		
		
		// Close the logger
		Log.close();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Set<Entry<String, Double>> getMilesTravelledPerPassenger(
			Accumulator[] data,
			Set<Entry<Accumulator, String>> milesTravelledPerFlight) throws InterruptedException {
		// The root node
		MapJob rootNode = new MapJob<String, Double>();
	
		// Generate some workers
		LinkedList<MapReduce<Object, Double>> mappers = createWorkers(numWorkers);
	
		int mapperIndex = 0;
		int i = 0;
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + data.length);
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<Object, Double> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Object, Double> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac.addDistance(milesTravelledPerFlight),
					STATE.GET_TRAVELLED_MILES_PAS);
		}
	
		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);
	
		// Stop threads
		for (MapReduce<Object, Double> m : mappers)
			m.stop();
	
		// Get results
		return rootNode.getResults();
	}

	private static Set<Entry<Accumulator, String>> getMilesTravelledPerFlight(Accumulator[] data,
			HashMap<String, Location> airportLocations) throws InterruptedException {
		// The root node
		MapJob<Accumulator, String> rootNode = new MapJob<Accumulator, String>();
	
		// Generate some workers
		LinkedList<MapReduce<Accumulator, String>> mappers = createWorkers(numWorkers);
	
		int mapperIndex = 0;
		int i = 0;
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + data.length);
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<Accumulator, String> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Accumulator, String> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac.mergeLocation(
					airportLocations.get(ac.fields[2]),
					airportLocations.get(ac.fields[3])),
					STATE.GET_TRAVELLED_MILES);
		}
	
		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);
	
		// Stop threads
		for (MapReduce<Accumulator, String> m : mappers)
			m.stop();
	
		// Get results
		return rootNode.getResults();
	}

	private static HashMap<String, Location> getAirportLocationMap(
			Accumulator[] data2) throws InterruptedException {
		// The root node
		MapJob<Accumulator, Location> rootNode = new MapJob<Accumulator, Location>();
	
		// Generate some workers
		LinkedList<MapReduce<Accumulator, Location>> mappers = createWorkers(numWorkers);
	
		int mapperIndex = 0;
		int i = 0;
		for (Accumulator ac : data2) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + data2.length);
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<Accumulator, Location> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Accumulator, Location> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac, STATE.CONVERT_AIRPORT_TO_LOCATION);
		}
	
		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);
	
		// Stop threads
		for (MapReduce<Accumulator, Location> m : mappers)
			m.stop();
	
		// Get results
		Set<Entry<Accumulator, Location>> results = rootNode.getResults();
		HashMap<String, Location> airportMap = new HashMap<String, Location>();
		for (Entry<Accumulator, Location> entry : results) {
			String s = ((Object)entry.getKey()).toString();
			airportMap.put(s, entry.getValue());
		}
		return airportMap;
	}

	private static Accumulator[] convertInputTimes(Accumulator[] data) throws InterruptedException {
		// The root node
		MapJob<Accumulator, String> rootNode = new MapJob<Accumulator, String>();

		// Generate some workers
		LinkedList<MapReduce<Accumulator, String>> mappers = createWorkers(numWorkers);

		int mapperIndex = 0;
		int i = 0;
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + (data.length * 2));
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<Accumulator, String> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Accumulator, String> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac, STATE.CONVERT_TIME_1);
		}

		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);

		// Stop threads
		for (MapReduce<Accumulator, String> m : mappers)
			m.stop();

		// Get results
		Set<Entry<Accumulator, String>> entrySet = rootNode.getResults();
		data = new Accumulator[entrySet.size()];
		i = 0;
		for (Entry<Accumulator, String> entry : entrySet)
			data[i++] = entry.getKey();
		
		// Generate some workers
		mappers = createWorkers(numWorkers);

		mapperIndex = 0;
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + (data.length * 2));
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<Accumulator, String> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Accumulator, String> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac, STATE.CONVERT_TIME_2);
		}

		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);
		
		// Stop threads
		for (MapReduce<Accumulator, String> m : mappers)
			m.stop();
		
		// Get results
		entrySet = rootNode.getResults();
		data = new Accumulator[entrySet.size()];
		i = 0;
		for (Entry<Accumulator, String> entry : entrySet)
			data[i++] = entry.getKey();
		return data;
	}

	private static Set<Entry<String, Integer>> getPassengersPerFlightNum(
			Accumulator[] data) throws InterruptedException {
		// The root node
		MapJob<String, Integer> rootNode = new MapJob<String, Integer>();
	
		// Generate some workers
		LinkedList<MapReduce<String, Integer>> mappers = createWorkers(numWorkers);
	
		int mapperIndex = 0;
		int i = 0;
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + data.length);
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<String, Integer> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<String, Integer> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac.fields[1], STATE.GET_NUM_PASSENGERS_PER_FLIGHT);
		}
	
		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);
	
		// Stop threads
		for (MapReduce<String, Integer> m : mappers)
			m.stop();
	
		// Get results
		return rootNode.getResults();
	}

	private static Set<Entry<String, Integer>> getAirportDeparturesNum(
			Accumulator[] data, Accumulator[] data2)
			throws InterruptedException {
		// The root node
		MapJob<String, Integer> rootNode = new MapJob<String, Integer>();

		// Generate some workers
		LinkedList<MapReduce<String, Integer>> mappers = createWorkers(numWorkers);

		int mapperIndex = 0;
		int i = 0;
		// First file
		for (Accumulator ac : data) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / "
						+ (data.length + data2.length));
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<String, Integer> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<String, Integer> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac.fields[2], STATE.GET_NUM_DEPARTURES_1);
		}
		// Second file
		for (Accumulator ac : data2) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / "
						+ (data.length + data2.length));
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<String, Integer> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<String, Integer> entry : m.getResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			m.addJob(ac.fields[1], STATE.GET_NUM_DEPARTURES_2);
		}

		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);

		// Stop threads
		for (MapReduce<String, Integer> m : mappers)
			m.stop();

		// Get results
		return rootNode.getResults();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Accumulator[] getInputData(List<String> lines, int numFields)
			throws InterruptedException {
		// The root node
		MapJob rootNode = new MapJob();

		// Generate some workers
		LinkedList<MapReduce<String, Accumulator>> mappers = createWorkers(numWorkers);

		// For each record
		int mapperIndex = -1;
		int i = 0;
		for (String line : lines) {
			if (++i % 100 == 0)
				Log.log("Processed: " + i + " / " + lines.size());
			if (++mapperIndex >= numWorkers)
				mapperIndex = 0;
			MapReduce<String, Accumulator> m = mappers.get(mapperIndex);
			while (m.isWorking() && !m.hasFinishedJob()) {
				if (++mapperIndex >= numWorkers)
					mapperIndex = 0;
				m = mappers.get(mapperIndex);
			}
			if (m.hasFinishedJob()) {
				for (Entry<Accumulator, Accumulator> entry : m.getVResults())
					rootNode.map(entry.getKey(), entry.getValue());
			}
			if (numFields == 4)
				m.addJob(line, STATE.READ_FILE_2);
			else
				m.addJob(line, STATE.READ_FILE_1);
		}

		// Wait for mapping jobs to complete
		waitForJobsToComplete(mappers, rootNode);

		// Stop threads
		for (MapReduce<String, Accumulator> m : mappers)
			m.stop();

		// Get results
		Set<Entry<Object, Object>> entrySet = rootNode.getVResults();
		Accumulator[] results = new Accumulator[entrySet.size()];
		i = 0;
		for (Entry<Object, Object> entry : entrySet)
			results[i++] = (Accumulator) entry.getKey();
		
		// Apply Levenshtein algorithm for error correction to first set of data
		if (numFields > 4)
			results = Levenshtein.applyFixes(results);
		
		// Merge results that are the same
		HashMap<String, Accumulator> mergeSet = new HashMap<String, Accumulator>();
		for (Accumulator acc : results) {
			String uId = acc.getUniqueId();
			if (uId.length() > 0)
				mergeSet.put(uId, acc);
		}
		return mergeSet.values().toArray(
				new Accumulator[mergeSet.values().size()]);
	}
}
