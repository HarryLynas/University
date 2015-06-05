package uk.ac.reading.pm002501.misc;

public class Random {
	/**
	 * The random number generator seeded with the roughly startup time of the
	 * application.
	 */
	private static java.util.Random random = new java.util.Random(
			System.currentTimeMillis());

	/**
	 * Return a random number in a given range.
	 * 
	 * @param min
	 *            The minimum value.
	 * @param max
	 *            The maximum value.
	 * @return The resulting pseudorandom value.
	 */
	public static int getRandom(int min, int max) {
		return random.nextInt(max - min + 1) + min;
	}

	/**
	 * Returns 5 random colours, RGB.
	 * 
	 * @return A int array, each 3 positions is R, G, B. Contains 5 colours.
	 */
	public static int[] getRandomColours() {
		return new int[] { getRandom(0, 255), getRandom(0, 255),
				getRandom(0, 255), getRandom(0, 255), getRandom(0, 255),
				getRandom(0, 255), getRandom(0, 255), getRandom(0, 255),
				getRandom(0, 255), getRandom(0, 255), getRandom(0, 255),
				getRandom(0, 255), getRandom(0, 255), getRandom(0, 255),
				getRandom(0, 255) };
	}
}
