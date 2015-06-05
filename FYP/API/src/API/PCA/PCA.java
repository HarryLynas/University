package API.PCA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import API.Image;
import API.database.EigenfaceCache;
import API.helpers.AlgorithmHelper;
import API.logger.Log;
import API.recognition.FaceRecognition;
import Jama.Matrix;

public class PCA extends AlgorithmHelper {
	
	private final static boolean debug = false;

	private Image[] images;
	private double[][] image_data;
	private int numFaces = 0;
	private int numEigenFacesToUse = 150;
	private double[] eigenValues = null;
	private double[][] vectors = null;
	private double[] dataAverageForEachRow = null;
	private double[][] dataWithAverageSubtracted = null;
	private double[][] weights = null;
	private double[][] eigenFaces = null;

	private EigenfaceCache cache = null;

	public void setPCAData(Image[] data, int numFaces, int numEigenFaces, double[][] image_data) {
		this.images = data;
		this.numFaces = numFaces;
		numEigenFacesToUse = numEigenFaces;
		this.image_data = image_data;
	}

	public EigenfaceCache getResults() {
		return cache;
	}

	public void run() {
		if (images.length == 0)
			return;
		Log.append("Starting...");
		long currTime = System.currentTimeMillis();
		try {
			double[][] data = this.image_data;
			
			// Calculate needed data for future
			calculateAverageAndDifferenceData(data);

			// Get eigenvalues and eigenvectors as variables
			calculateEigenData();

			// Compute principle components
			Log.append("Computing principle components...");
			calculatePrincipleComponents();

			// Calculate weights
			Log.append("Computing weights and eigenFaces...");
			calculateWeightsAndEigenFaces();

			if (debug) {
				Log.append("Computing reconstructed faces...");
				// Reconstruct faces
				BufferedImage[] reconstructedFaces = new BufferedImage[data.length];
				for (int i = 0; i < data.length; ++i)
					reconstructedFaces[i] = getImageByWeights(weights[i],
							eigenFaces);

				// Debug
				System.out
						.println("Saving eigenImages & reonstructed faces to files...");
				for (int i = 0; i < reconstructedFaces.length; ++i) {
					File outputfile = new File(
							"C:\\Users\\Harry_\\Desktop\\DUMP\\" + i
									+ "_Weights.jpg");
					ImageIO.write(reconstructedFaces[i], "jpg", outputfile);
				}

				// debug out eigenfaces
				for (int i = 0; i < eigenFaces.length; ++i) {
					BufferedImage f = getNormalisedImage(50, 50, 0, 255,
							eigenFaces[i]);
					ImageIO.write(f, "jpg", new File(
							"C:\\Users\\Harry_\\Desktop\\DUMP\\faces\\" + i
									+ ".jpg"));
				}
			}

			// Build the cache
			cache = new EigenfaceCache(numFaces, numEigenFacesToUse,
					data.length, weights, dataAverageForEachRow, eigenFaces,
					eigenValues, images);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Log.append("Took: "
				+ String.valueOf(System.currentTimeMillis() - currTime)
				+ " ms.");
	}

	private void calculateWeightsAndEigenFaces() {
		int pixelCount = dataWithAverageSubtracted[0].length;
		int imageCount = dataWithAverageSubtracted.length;
		int vectorsCount = vectors.length;
		// Calculate and normalise eigenFaces
		double[][] eigenFaces = new double[vectorsCount][pixelCount];
		for (int i = 0; i < vectorsCount; ++i) {
			double sumSquare = 0;
			for (int j = 0; j < pixelCount; ++j) {
				for (int k = 0; k < imageCount; ++k) {
					eigenFaces[i][j] += dataWithAverageSubtracted[k][j]
							* vectors[i][k];
				}
				sumSquare += eigenFaces[i][j] * eigenFaces[i][j];
			}
			double norm = Math.sqrt(sumSquare);
			for (int j = 0; j < pixelCount; ++j) {
				eigenFaces[i][j] /= norm;
			}
		}
		// Get only the selected amount of eigenFaces
		this.eigenFaces = new Matrix(eigenFaces).getMatrix(
				0,
				eigenFaces.length <= numEigenFacesToUse ? eigenFaces.length - 1
						: numEigenFacesToUse, 0, eigenFaces[0].length - 1)
				.getArray();
		// Calculate weights
		this.weights = new Matrix(dataWithAverageSubtracted).times(
				new Matrix(eigenFaces).transpose()).getArray();
	}

	private void calculateAverageAndDifferenceData(double[][] data) {
		int columnLength = data[0].length;
		int rowLength = data.length;
		dataAverageForEachRow = new double[columnLength];
		for (int i = 0; i < columnLength; ++i) {
			double sum = 0;
			for (int counter = 0; counter < rowLength; ++counter) {
				sum += data[counter][i];
			}
			dataAverageForEachRow[i] = sum / rowLength;
		}
		dataWithAverageSubtracted = new double[rowLength][columnLength];
		for (int i = 0; i < rowLength; ++i) {
			dataWithAverageSubtracted[i] = matrixSubtract(data[i],
					dataAverageForEachRow);
		}
	}

	private void calculateEigenData() {

		Log.append("Computing covariance matrix...");
		// Compute covariance matrix
		RealMatrix matrix = new Covariance(new BlockRealMatrix(
				dataWithAverageSubtracted).transpose()).getCovarianceMatrix();

		Log.append("Computing eigen decomposition...");
		// Get the eigenvalues and eigenvectors
		EigenDecomposition eigen = new EigenDecomposition(matrix);

		eigenValues = eigen.getRealEigenvalues();
		// Transpose because rows need to be eigenvectors not columns
		vectors = eigen.getV().transpose().getData();
	}

	private BufferedImage getImageByWeights(double[] weights,
			double[][] eigenFaces) {
		Matrix subMatrix = new Matrix(eigenFaces);
		// getMatrix(initial row index, final row index, initial column index,
		// final column index)
		subMatrix = subMatrix.getMatrix(
				0,
				subMatrix.getRowDimension() <= numEigenFacesToUse ? subMatrix
						.getRowDimension() - 1 : numEigenFacesToUse, 0,
				subMatrix.getColumnDimension() - 1);

		double[] imageData = multiplyOneDimensionalMatrix(weights, subMatrix);
		imageData = addOneDimensionalMatrix(imageData, dataAverageForEachRow);
		return getNormalisedImage(FaceRecognition.standardSize,
				FaceRecognition.standardSize, 0, 255, imageData);
	}

	private void calculatePrincipleComponents() {
		int numComps = vectors.length;
		// Get principle components
		ArrayList<PrincipleComponent> principleComponents = new ArrayList<PrincipleComponent>();
		for (int i = 0; i < numComps; ++i) {
			double[] eigenVector = new double[numComps];
			for (int j = 0; j < numComps; ++j) {
				eigenVector[j] = vectors[i][j];
			}
			principleComponents.add(new PrincipleComponent(eigenValues[i],
					eigenVector));
		}
		Collections.sort(principleComponents);
		// Update original vals
		Iterator<PrincipleComponent> it = principleComponents.iterator();
		int count = 0;
		double[][] tempStore_vectors = new double[3][vectors[0].length];
		double[] tempStore_values = new double[3];
		while (it.hasNext()) {
			PrincipleComponent p = it.next();
			if (count < 3) {
				tempStore_vectors[count] = p.eigenVector;
				tempStore_values[count] = p.eigenValue;
			} else {
				eigenValues[count - 3] = p.eigenValue;
				vectors[count - 3] = p.eigenVector;
			}
			++count;
		}
		/*
		count -= 3;
		int max = count + 3;
		int i = 0;
		for (; count < max; ++count) {
			eigenValues[count] = tempStore_values[i];
			vectors[count] = tempStore_vectors[i];
			++i;
		}*/
	}

	protected class PrincipleComponent implements
			Comparable<PrincipleComponent> {
		public double eigenValue;
		public double[] eigenVector;

		public PrincipleComponent(double eigenValue, double[] eigenVector) {
			this.eigenValue = eigenValue;
			this.eigenVector = eigenVector;
		}

		@Override
		public int compareTo(PrincipleComponent o) {
			if (eigenValue > o.eigenValue)
				return -1;
			else if (eigenValue < o.eigenValue)
				return 1;
			return 0;
		}
	}
}
