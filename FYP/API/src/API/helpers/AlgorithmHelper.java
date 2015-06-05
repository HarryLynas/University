package API.helpers;

import java.awt.image.BufferedImage;

import API.recognition.FaceRecognition;
import Jama.Matrix;

public abstract class AlgorithmHelper {
	
	protected BufferedImage getNormalisedImage(int width, int height,
			float goalMin, float goalMax, double[] data) {
		BufferedImage returnVal = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		double min = getMinValue(data);
		double max = getMaxValue(data);
		int row = -1;
		for (int i = 0; i < data.length; ++i) {
			// Normalise
			double fgrey = goalMin + (goalMax - goalMin)
					* (((data[i]) - min) / (max - min));
			int grey = (int) fgrey;
			if (grey > 255)
				grey = 255;
			else if (grey < 0)
				grey = 0;
			grey = 0xff000000 | (grey << 16) | (grey << 8) | grey;
			if (i % FaceRecognition.standardSize == 0)
				++row;
			returnVal.setRGB(row, i % FaceRecognition.standardSize, grey);
		}
		return returnVal;
	}

	protected double[] matrixSubtract(double[] m1, double[] m2) {
		double[] result = new double[m1.length];
		for (int i = 0; i < result.length; ++i)
			result[i] = m1[i] - m2[i];
		return result;
	}

	protected double[] multiplyOneDimensionalMatrix(double[] weights,
			Matrix subMatrix) {
		double[] returnVal = new double[subMatrix.getColumnDimension()];
		for (int i = 0; i < returnVal.length; ++i) {
			double[] column = getColumn(subMatrix, i);
			double sum = 0;
			for (int j = 0; j < column.length; ++j)
				sum += weights[j] * column[j];
			returnVal[i] = sum;
		}
		return returnVal;
	}

	protected double[] getColumn(Matrix m, int column) {
		double[] col = new double[m.getRowDimension()];
		for (int i = 0; i < m.getRowDimension(); ++i)
			col[i] = m.get(i, column);
		return col;
	}

	protected double[] addOneDimensionalMatrix(double[] m1, double[] m2) {
		double[] returnVal = new double[m1.length];
		for (int i = 0; i < returnVal.length; ++i)
			returnVal[i] = m1[i] + m2[i];
		return returnVal;
	}

	protected double getMinValue(double[] data) {
		double minVal = Double.MAX_VALUE;
		for (int i = 0; i < data.length; ++i)
			minVal = Math.min(minVal, data[i]);
		return minVal;
	}

	protected double getMaxValue(double[] data) {
		double maxVal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < data.length; ++i)
			maxVal = Math.max(maxVal, data[i]);
		return maxVal;
	}

	protected double[][] subtractFromEachRow(double[][] data, double[] subtract) {
		double[][] returnVal = new double[data.length][subtract.length];
		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < subtract.length; ++j)
				returnVal[i][j] = data[i][j];
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < subtract.length; ++j) {
				returnVal[i][j] -= subtract[j];
			}
		}
		return returnVal;
	}

	protected double[][] nonMatrixTimesSelf(double[][] data) {
		double[][] returnVal = new double[data.length][data[0].length];
		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < data[i].length; ++j)
				returnVal[i][j] = data[i][j];
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < data[i].length; ++j) {
				returnVal[i][j] = returnVal[i][j] * returnVal[i][j];
			}
		}
		return returnVal;
	}
}
