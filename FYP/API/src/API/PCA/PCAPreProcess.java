package API.PCA;

import API.Image;
import API.helpers.TrainingThread;
import API.logger.Log;

public class PCAPreProcess extends PCA implements Runnable {

	private final static int numTrainingThreads = 4;
	
	private Image[] images;
	private int numEigenFacesToUse = 150;
	
	public PCAPreProcess(Image[] paths, int num) {
		this.images = paths;
		numEigenFacesToUse = num;
	}
	
	@Override
	public void run() {
		if (images.length == 0)
			return;
		try {
			Log.append("Getting training data...");
			// Get data in correct format
			calculateTrainingData();

			// Normalise data between 0 and 1
			for (int k = 0; k < images.length; ++k) {
				double[][] currData = images[k].getPCAData();
				for (int faceInd = 0; faceInd < currData.length; ++faceInd) {
					double[] data2 = currData[faceInd];
					double min = getMinValue(data2);
					double max = getMaxValue(data2);
					for (int j = 0; j < data2.length; ++j)
						data2[j] = 0 + (1 - 0)
								* (((data2[j]) - min) / (max - min));
				}
			}

			// Get the number of faces
			int numFaces = 0;
			int pixelNum = 0;
			for (Image i : images) {
				numFaces += i.getPCAData().length;
				if (i.getPCAData().length > 0)
					pixelNum = i.getPCAData()[0].length;
			}
			// Create store for all face data
			double[][] data = new double[numFaces][pixelNum];
			// Load each face into the store
			int c = 0;
			for (Image i : images) {
				for (double[] f : i.getPCAData()) {
					data[c++] = f;// .clone();
				}
			}
			
			super.setPCAData(images, numFaces, numEigenFacesToUse, data);
			super.run();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void calculateTrainingData() throws Exception {
		int size = images.length;
		if (size > 100) {
			int steps = size / numTrainingThreads;
			Thread[] threads = new Thread[numTrainingThreads];
			for (int i = 0; i < numTrainingThreads; ++i)
				threads[i] = new Thread(new TrainingThread(images, steps * i, steps * (i + 1), 0));
			for (int i = (steps * numTrainingThreads) + 1; i < size; ++i)
				TrainingThread.addPCATrainData(images[i]);
			for (Thread t : threads) {
				t.setDaemon(true);
				t.start();
			}
			boolean running = true;
			while (running) {
				Thread.sleep(100);
				running = false;
				for (Thread t : threads) {
					if (t.isAlive())
						running = true;
				}
			}
		} else {
			for (int i = 0; i < size; ++i)
				TrainingThread.addPCATrainData(images[i]);
		}
	}
}
