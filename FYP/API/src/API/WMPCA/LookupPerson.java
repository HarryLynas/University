package API.WMPCA;

import java.util.LinkedList;

import API.Image;
import API.LookupResult;
import API.database.EigenfaceCache;
import API.helpers.AlgorithmHelper;
import API.helpers.TrainingThread;
import API.logger.Log;
import Jama.Matrix;

public class LookupPerson extends AlgorithmHelper {
	private EigenfaceCache[] caches = null;
	private String lookupImage = null;

	public LookupPerson(EigenfaceCache[] caches, String lookupImage) {
		this.caches = caches;
		this.lookupImage = lookupImage;
	}
	
	public LookupResult[] lookupInCache(int numRegions) {
		// First preprocess the data
		Image image = new Image(lookupImage);
		try {
			TrainingThread.addWMPCATrainData(image, numRegions);
		} catch (Exception e) {
			e.printStackTrace();
			return new LookupResult[0];
		}
		
		// Get the WMPCA resulting data
		double[][][] data = image.getWMPCAData();
		
		// Normalise between 0..1
		for (int f = 0; f < data.length; ++f) { // For each face in the image
			for (int r = 0; r < data[f].length; ++r) { // For each region of the face
				double[] pixels = data[f][r]; // pixels for this region
				// Normalise the pixels
				double min = getMinValue(pixels);
				double max = getMaxValue(pixels);
				for (int p = 0; p < pixels.length; ++p)
					pixels[p] = 0 + (1 - 0)
							* (((pixels[p]) - min) / (max - min));
			}
		}
		
		LinkedList<LookupResult> returnResults = new LinkedList<LookupResult>();
		
		// For each face found
		for (int f = 0; f < data.length; ++f) {
			
			// Arrays to store results in
			int[] results = new int[data[0].length];
			double[] minDistances = new double[data[0].length];
			
			// For each region
			for (int r = 0; r < data[f].length; ++r) {
				// Retrieve the pixels
				double[] pixels = data[f][r];
				
				// Get the cache for this region
				EigenfaceCache cache = caches[r];
				
				// Turn into a matrix
				Matrix input = new Matrix(pixels, 1);
	
				// Subtract average faces from input face
				input = input.minus(new Matrix(cache.getAverageFaces(), 1));
				
				// Get the input face weights
				Matrix inputWeights = getInputWeights(cache.getNumEigenFaces(),
						input, cache);
				// Get the distances
				double[] distances = getDistances(inputWeights, cache);
				double[] minDistance = getMinimumDistance(distances);

				// Store results
				results[r] = (int) minDistance[1];
				minDistances[r] = minDistance[0];
			}
			
			// Region is processed, now see if match
			double sum = 0;
			int match = results[0];
			boolean matched = true;
			
			for (int r = 0; r < data[f].length; ++r) {
				Log.append("Region " + r + " selected result:\t" + results[r] + ",\tT: " + ((int)minDistances[r]));
				sum += minDistances[r];
				if (results[r] != match)
					matched = false;
			}
			sum /= data[f].length;
			
			Log.append("Mean threshold = \t" + ((int)sum));
			
			int result = -1;
			double distance = 0;
			//if (matched && sum <= threshold) {
			//	Log.append("Match found.");
				result = match;
				distance = minDistances[0];
			/*} else {
				Log.append("No suitable match found.");
				// use the one with the lowest distance
				distance = Double.MAX_VALUE;
				for (int r = 0; r < data[f].length; ++r) {
					if (minDistances[r] < distance) {
						distance = minDistances[r];
						result = results[r];
					}
				}
			}*/
			
			returnResults.add(new LookupResult(result, distance));
		}
		
		return returnResults.toArray(new LookupResult[returnResults.size()]);
	}

	private double[] getMinimumDistance(double[] distances) {
		double minimumDistance = Double.MAX_VALUE;
		int selected = 0;
		for (int i = 0; i < distances.length; ++i) {
			if (distances[i] < minimumDistance) {
				minimumDistance = distances[i];
				selected = i;
			}
		}
		return new double[] { minimumDistance, selected };
	}

	private Matrix getInputWeights(int numEigenFaces, Matrix inputFace, EigenfaceCache cache) {
		Matrix eigenFaces = new Matrix(cache.getEigenFaces());
		Matrix selectedEigenFaces = eigenFaces.getMatrix(
				0,
				eigenFaces.getRowDimension() <= numEigenFaces ? eigenFaces
						.getRowDimension() - 1 : numEigenFaces, 0, eigenFaces
						.getColumnDimension() - 1);
		return inputFace.times(selectedEigenFaces.transpose());
	}

	private double[] getDistances(Matrix inputWeights, EigenfaceCache cache) {
		double[] inputWeightsData = inputWeights.getArray()[0];
		double[][] tempWeights = subtractFromEachRow(cache.getWeights(),
				inputWeightsData);
		tempWeights = nonMatrixTimesSelf(tempWeights);
		double[] distances = new double[tempWeights.length];
		for (int i = 0; i < tempWeights.length; ++i) {
			double sum = 0;
			for (int j = 0; j < tempWeights[i].length; ++j) {
				sum += tempWeights[i][j];
			}
			distances[i] = sum;
		}
		return distances;
	}
}
