package API.helpers;

import java.awt.image.BufferedImage;

import API.Image;
import API.recognition.FaceRecognition;

public class TrainingThread implements Runnable {
	private Image[] images;
	private int step;
	private int size;
	private int numRegions;
	
	public TrainingThread(Image[] images, int step, int size, int numRegions) {
		this.images = images;
		this.step = step;
		this.size = size;
		this.numRegions = numRegions;
	}
	
	@Override
	public void run() {
		try {
			if (numRegions <= 0) {
				for (int i = step; i < size; ++i)
					addPCATrainData(images[i]);
			} else {
				for (int i = step; i < size; ++i)
					addWMPCATrainData(images[i], numRegions);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void addPCATrainData(Image image) throws Exception {
		// Get each region
		BufferedImage[] faces = FaceRecognition.getProcessedFaceRegions(image
				.getPath());
		// Check some faces were found
		if (faces.length == 0)
			return;
		// Store face index and pixel data
		double[][] currentData = new double[faces.length][faces[0].getWidth()
				* faces[0].getHeight()];
		// Get the pixels into a 1d array for each face region
		for (int f = 0; f < faces.length; ++f) {
			int count = 0;
			for (int i = 0; i < faces[f].getWidth(); ++i) {
				for (int j = 0; j < faces[f].getHeight(); ++j) {
					currentData[f][count++] = faces[f].getRGB(i, j);
				}
			}
		}
		// Update the store
		image.setData(currentData);
	}
	
	//public static int static_count = 0;
	
	public static void addWMPCATrainData(Image image, int numRegions) throws Exception {
		// Get each region
		BufferedImage[] faces = FaceRecognition.getProcessedFaceRegions(image
				.getPath());
		// Check some faces were found
		if (faces.length == 0)
			return;
		// Split each face into regions
		int regionHeight = (int) Math.ceil(faces[0].getHeight() / numRegions);
		double[][][] regions = new double[faces.length][numRegions][regionHeight * faces[0].getWidth()];
		
		for (int f = 0; f < faces.length; ++f) {
			for (int r = 0; r < numRegions; ++r) {
				int count = 0;
				for (int x = 0; x < faces[f].getWidth(); ++x) {
					for (int y = regionHeight * r; y < regionHeight * (r + 1); ++ y) {
						if (y > faces[f].getHeight())
							break;
						regions[f][r][count++] = faces[f].getRGB(x, y);
					}
				}
				
				// For debug purposes
				/*
				BufferedImage out = new BufferedImage(faces[0].getWidth(), regionHeight, BufferedImage.TYPE_INT_RGB);
				//out.getRaster().setPixels(0, 0, faces[0].getWidth(), regionHeight, regions[f][r]);
				int index = 0;
				for (int i = 0; i < faces[0].getWidth(); ++i) {
					for (int j = 0; j < regionHeight; ++j) {
						int pix = (int) regions[f][r][index++];
						pix = 0xff000000 | (pix << 16) | (pix << 8) | pix;
						out.setRGB(i, j, pix);
					}
				}
				ImageIO.write(out, "jpg", new File("C:\\Users\\Harry_\\Desktop\\DUMP_2\\" + static_count++ + "_" + f + "_" + r + ".jpg"));
				*/
			}
		}

		// Update the store
		image.setData(regions);	
	}
}
