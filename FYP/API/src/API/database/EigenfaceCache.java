package API.database;

import java.io.Serializable;

import API.Image;
import API.helpers.DeepCopy;

public class EigenfaceCache implements Serializable, Cloneable {

	private static final long serialVersionUID = 8193454671002418168L;

	private int numFaces;
	private int numEigenFaces;
	private int imageDimensions;
	private double[][] weights;
	private double[] averageFaces;
	private double[][] eigenFaces;
	private double[] eigenValues;
	private Image[] images;

	@Override
	public EigenfaceCache clone() {
		return new EigenfaceCache(numFaces, numEigenFaces, imageDimensions,
				weights, averageFaces, eigenFaces, eigenValues, images);
	}

	public EigenfaceCache(int numFaces, int numEigenFaces, int imageDimensions,
			double[][] weights, double[] averageFace, double[][] eigenFaces,
			double[] eigenValues, Image[] images) {
		this.numFaces = numFaces;
		this.numEigenFaces = numEigenFaces;
		this.imageDimensions = imageDimensions;
		// Deep copy weights
		this.weights = DeepCopy.copy(weights);
		// Deep copy average face
		this.averageFaces = averageFace.clone();
		// Deep copy eigen face
		this.eigenFaces = DeepCopy.copy(eigenFaces);
		// Deep copy eigen values
		this.eigenValues = eigenValues.clone();
		// Just pass by ref
		this.images = images;
	}

	public int getNumEigenFaces() {
		return numEigenFaces;
	}

	public int getImageDimensions() {
		return imageDimensions;
	}

	public double[][] getWeights() {
		return weights;
	}

	public double[] getAverageFaces() {
		return averageFaces;
	}

	public int getNumFaces() {
		return numFaces;
	}

	public double[][] getEigenFaces() {
		return eigenFaces;
	}

	public double[] getEigenValues() {
		return eigenValues;
	}

	public Image[] getImages() {
		return images;
	}
}
