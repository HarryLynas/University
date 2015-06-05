package API.WMPCA;

import java.io.Serializable;

import API.Image;
import API.database.EigenfaceCache;
import API.helpers.AlgorithmHelper;
import API.helpers.TrainingThread;
import API.logger.Log;

public class WMPCA extends AlgorithmHelper implements Runnable, Serializable {

	private static final long serialVersionUID = -7314060653953690902L;

	private final static int numTrainingThreads = 4;

	private Image[] images;
	private int numEigenFacesToUse = 150;
	private int numRegions = 3;

	private EigenfaceCache[] caches = null;

	public WMPCA(Image[] paths, int num, int regions) {
		this.images = paths;
		numEigenFacesToUse = num;
		numRegions = regions;
	}

	public EigenfaceCache[] getResults() {
		return caches;
	}

	@Override
	public void run() {
		if (images.length == 0)
			return;

		long currTime = System.currentTimeMillis();
		try {
			Log.append("Getting training data...");
			// Get data in correct format
			calculateTrainingData();

			// [faces.length][numRegions][regionHeight * faces[0].getWidth()]
			
			// Normalise the data between 0..1
			for (int i = 0; i < images.length; ++i) { // For each image
				double[][][] data = images[i].getWMPCAData();
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
			}
			
			// Get the number of faces and pixels
			int numFaces = 0;
			int numPixels = 0;
			for (Image i : images) {
				numFaces += i.getWMPCAData().length;
				if (i.getWMPCAData().length > 0)
					numPixels = i.getWMPCAData()[0][0].length;
			}
			
			double[][][] toPerformPCAOn = new double[numRegions][numFaces][numPixels];
			// Get [faces][region] for each region, perform PCA on this data
			for (int r = 0; r < numRegions; ++r) {
				double[][] data = new double[numFaces][numPixels];
				int face = 0;
				for (Image i  : images) {
					double[][][] wmpca = i.getWMPCAData();
					for (int f = 0; f < wmpca.length; ++f) { // each face
						data[face++] = wmpca[f][r];
					}
				}
				toPerformPCAOn[r] = data;
			}
			
			// perform PCA on each region set
			Thread[] threads = new Thread[toPerformPCAOn.length];
			WMPCA_Worker[] workers = new WMPCA_Worker[toPerformPCAOn.length];
			for (int r = 0; r < toPerformPCAOn.length; ++r) {
				workers[r] = new WMPCA_Worker(images, numFaces, numEigenFacesToUse, toPerformPCAOn[r]);
				threads[r] = new Thread(workers[r]);
				threads[r].setDaemon(true);
				threads[r].setName("WMPCA-Worker-" + r);
				threads[r].start();
			}
			boolean running = true;
			while (running) {
				running = false;
				for (Thread t : threads) {
					if (t.isAlive())
						running = true;
				}
				Thread.sleep(500);
			}
			
			// Get results into array
			EigenfaceCache[] cache = new EigenfaceCache[workers.length];
			for (int i = 0; i < workers.length; ++i)
				cache[i] = workers[i].getResults();
			
			this.caches = cache;
			Log.append("Got all cache's.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Log.append("Done. Took: "
				+ String.valueOf(System.currentTimeMillis() - currTime)
				+ " ms.");
	}

	private void calculateTrainingData() throws Exception {
		int size = images.length;
		if (size > 100) {
			int steps = size / numTrainingThreads;
			Thread[] threads = new Thread[numTrainingThreads];
			for (int i = 0; i < numTrainingThreads; ++i)
				threads[i] = new Thread(new TrainingThread(images, steps * i, steps * (i + 1), numRegions));
			for (int i = (steps * numTrainingThreads) + 1; i < size; ++i)
				TrainingThread.addWMPCATrainData(images[i], numRegions);
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
				TrainingThread.addWMPCATrainData(images[i], numRegions);
		}
	}
}
