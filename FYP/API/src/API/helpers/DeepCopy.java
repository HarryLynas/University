package API.helpers;

public class DeepCopy {
	/**
	 * Class cannot be instantiated.
	 */
	private DeepCopy() {
		throw new AssertionError();
	}
	
	/**
	 * Deep copy a two dimensional double array.
	 * @param data The 2d array to copy.
	 * @return A new array with the same data as the input array.
	 */
	public static double[][] copy(double[][] data) {
		if (data.length == 0)
			return new double[0][0];
		double[][] returnData = new double[data.length][data[0].length];
		for (int i = 0; i < data.length; ++i)
			returnData[i] = data[i].clone();
		return returnData;
	}
	
	/**
	 * Deep copy a three dimensional double array.
	 * @param data The 3d array to copy.
	 * @return A new array with the same data as the input array.
	 */
	public static double[][][] copy(double[][][] data) {
		if (data.length == 0)
			return new double[0][0][0];
		double[][][] returnData = new double[data.length][data[0].length][data[0][0].length];
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < data[i].length; ++j)
				returnData[i][j] = data[i][j].clone();
		}
		return returnData;
	}
}
