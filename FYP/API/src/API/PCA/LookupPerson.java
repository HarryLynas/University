package API.PCA;

import java.awt.image.BufferedImage;
import java.util.LinkedList;

import API.LookupResult;
import API.database.EigenfaceCache;
import API.helpers.AlgorithmHelper;
import API.recognition.FaceRecognition;
import Jama.Matrix;

public class LookupPerson extends AlgorithmHelper {

	private double threshold = 250;
	private EigenfaceCache cache = null;
	private String lookupImage = null;

	public LookupPerson(EigenfaceCache cache, String lookupImage, double threshold) {
		this.cache = cache;
		this.lookupImage = lookupImage;
		this.threshold = threshold;
	}

	public LookupResult[] lookupInCache() {
		// Greyscale and resize input image
		BufferedImage[] faces = FaceRecognition
				.getProcessedFaceRegions(lookupImage);
		
		LinkedList<LookupResult> returnResults = new LinkedList<LookupResult>();
		
		// For each face found
		for (int i = 0; i < faces.length; ++i) {
			// get the image pixels
			double[] pixels = new double[faces[i].getWidth()
					* faces[i].getHeight()];
			int c = 0;
			for (int k = 0; k < faces[i].getWidth(); ++k) {
				for (int j = 0; j < faces[i].getHeight(); ++j) {
					pixels[c++] = faces[i].getRGB(k, j);
				}
			}

			// Normalise between 0..1
			double min = getMinValue(pixels);
			double max = getMaxValue(pixels);
			for (int j = 0; j < pixels.length; ++j)
				pixels[j] = 0 + (1 - 0) * (((pixels[j]) - min) / (max - min));

			// Turn into matrix
			Matrix input = new Matrix(pixels, 1);
			
			// Subtract average faces from input face
			input = input.minus(new Matrix(cache.getAverageFaces(), 1));
			// Get the input face weights
			Matrix inputWeights = getInputWeights(cache.getNumEigenFaces(),
					input);
			// Get the distances
			double[] distances = getDistances(inputWeights);
			double[] minDistance = getMinimumDistance(distances);
			int result = -1;
			double distance = 0;
			if (minDistance[0] <= threshold) {
				result = (int) minDistance[1];
				distance = minDistance[0];
			} else {
				result = -1;
			}
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

	private Matrix getInputWeights(int numEigenFaces, Matrix inputFace) {
		Matrix eigenFaces = new Matrix(cache.getEigenFaces());
		Matrix selectedEigenFaces = eigenFaces.getMatrix(
				0,
				eigenFaces.getRowDimension() <= numEigenFaces ? eigenFaces
						.getRowDimension() - 1 : numEigenFaces, 0, eigenFaces
						.getColumnDimension() - 1);
		return inputFace.times(selectedEigenFaces.transpose());
	}

	private double[] getDistances(Matrix inputWeights) {
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
