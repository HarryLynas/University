package API.WMPCA;

import API.Image;
import API.PCA.PCA;

public class WMPCA_Worker extends PCA implements Runnable {

	public WMPCA_Worker(Image[] images, int numFaces, int numEigenFacesToUse, double[][] data) {
		super.setPCAData(images, numFaces, numEigenFacesToUse, data);
	}
	
	@Override
	public void run() {
		super.run();
	}
}
