package uk.ac.reading.pm002501.mapreduce;

public class Levenshtein {
	/**
	 * This class cannot be instantiated.
	 */
	private Levenshtein() {
		throw new AssertionError();
	}
	
	/**
	 * Returns the Levenshtein distance between two strings.
	 * 
	 * @param a The first string.
	 * @param b The second string.
	 * @return The integer distance between the strings.
	 * 
	 * @author Taken from: http://rosettacode.org/wiki/Levenshtein_distance#Java
	 */
    private static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
    
    /**
     * Get the nearest matching string from a given array of strings.
     * 
     * @param toMatch The string to find the nearest match to.
     * @param strings The array of strings to find the nearest match in.
     * @return The nearest matching string from the given array.
     */
    private static String getNearestMatch(String toMatch, String[] strings) {
    	int[] distances = new int[strings.length];
    	int i = 0;
    	for (String s : strings)
    		distances[i++] = distance(s, toMatch);
    	
    	String returnVal = toMatch;
    	
    	i = 0;
    	int lowest = Integer.MAX_VALUE;
    	for (int d : distances) {
    		if (d < lowest) {
    			boolean okayMatch = true;
    			for (char c : strings[i].toCharArray()) {
    				if (!Character.isAlphabetic(c) && !Character.isDigit(c)) {
    					okayMatch = false;
    					break;
    				}
    			}
    			if (okayMatch) {
    				lowest = d;
    				returnVal = strings[i];
    			}
    		}
    		++i;
    	}
    	
    	return returnVal;
    }

    /**
     * Applies the levenshtein method to a set of accumulators to fix most errors.
     * @param results The data to process
     * @return The fixed data.
     */
	public static Accumulator[] applyFixes(Accumulator[] results) {
		int records = results.length;
		if (records == 0)
			return results;
		for (int i = 0; i < 4; ++i) {
			String[] data = new String[records];
			// First pass to obtain data
			for (int j = 0; j < records; ++j)
				data[j] = results[j].fields[i];
			// Second pass fixes data
			for (int j = 0; j < records; ++j) {
				String s = results[j].fields[i];
				for (char c : s.toCharArray()) {
					if ((!Character.isAlphabetic(c) && !Character.isDigit(c)) ||
							(Character.isAlphabetic(c) && !Character.isUpperCase(c))) {
						results[j].fields[i] = getNearestMatch(s, data);
						break;
					}
				}
			}
		}
		return results;
	}
}
