package API;

import java.io.Serializable;

import API.helpers.DeepCopy;

public class Image implements Serializable, Cloneable {

	private static final long serialVersionUID = -5711238792962689562L;

	private String path;
	private double[][] pca_data;
	private double[][][] wmpca_data;
	
	@Override
	public Image clone() {
		return new Image(path, pca_data, wmpca_data);
	}

	public Image(String path) {
		this.path = path;
		this.pca_data = new double[0][0];
		this.wmpca_data = new double[0][0][0];
	}
	
	private Image(String path, double[][] data, double[][][] wmpca_data) {
		this.path = path;
		this.pca_data = DeepCopy.copy(data);
		this.wmpca_data = DeepCopy.copy(wmpca_data);
	}
	
	public void setData(double[][] data) {
		this.pca_data = data;
	}
	
	public void setData(double[][][] data) {
		this.wmpca_data = data;
	}

	public String getPath() {
		return path;
	}

	public double[][] getPCAData() {
		return pca_data;
	}
	
	public double[][][] getWMPCAData() {
		return wmpca_data;
	}
}
