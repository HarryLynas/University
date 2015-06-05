package API.recognition;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

public class FaceRecognition {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static final int standardSize = 50;

	/**
	 * This function returns each face region detected as a new buffered image
	 * that is resized to a standard size and greyscaled.
	 * 
	 * @param file The image to process.
	 * @return Each face region greyscaled and resized.
	 */
	public synchronized static BufferedImage[] getProcessedFaceRegions(
			String file) {
		try {
			String path = FaceRecognition.class
					.getResource("lbpcascade_frontalface.xml").getPath()
					.substring(1);
			path = path.replace("/ndrive/pm002501/My%20Documents/", "//ndrive/pm002501/My Documents/");
			// Set the classifier
			CascadeClassifier faceDetector = new CascadeClassifier(path);
			// Read the image into a OpenCV format
			Mat image = Highgui.imread(file);
			MatOfRect faceDetections = new MatOfRect();
			// Attempt to determine all face regions
			try {
				faceDetector.detectMultiScale(image, faceDetections);
			} catch (Exception e) {
			}

			// Get the pixels for a buffered image for each face
			int numRegions = faceDetections.toArray().length;
			BufferedImage[] images = new BufferedImage[numRegions];
			int count = 0;

			for (Rect rect : faceDetections.toArray()) {
				images[count] = new BufferedImage(rect.width, rect.height,
						BufferedImage.TYPE_INT_RGB);
				// Obtain the pixels
				for (int i = rect.x; i < rect.x + rect.width; ++i) {
					for (int j = rect.y; j < rect.y + rect.height; ++j) {
						// OpenCV has opposite axis
						double[] data = image.get(j, i);
						// Greyscale the pixel value by using the mean value of RGB
						int pixelVal = (int) ((data[0] + data[1] + data[2]) / 3);
						images[count].setRGB(i - rect.x, j - rect.y,
								pixelVal << 16 | pixelVal << 8 | pixelVal);
					}
				}
				// Resize image to the standard size
				images[count] = Scalr.resize(images[count], Scalr.Method.SPEED,
						Scalr.Mode.FIT_EXACT, standardSize, standardSize,
						Scalr.OP_ANTIALIAS);
				++count;
			}

			return images;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public synchronized static BufferedImage[] getProcessedFaceRegions(
			BufferedImage file) {
		try {
			// Get the image pixels
			String path = FaceRecognition.class
					.getResource("lbpcascade_frontalface.xml").getPath()
					.substring(1);
			path = path.replace("/ndrive/pm002501/My%20Documents/", "//ndrive/pm002501/My Documents/");
			CascadeClassifier faceDetector = new CascadeClassifier(path);

			File output = new File("temp.jpg");
			while (!output.canRead())
				Thread.sleep(50);
			Mat image = Highgui.imread(output.getPath());
			
			MatOfRect faceDetections = new MatOfRect();
			faceDetector.detectMultiScale(image, faceDetections);

			// Okay, now get the pixels for a buffered image
			int numRegions = faceDetections.toArray().length;
			BufferedImage[] images = new BufferedImage[numRegions];
			int count = 0;

			for (Rect rect : faceDetections.toArray()) {
				images[count] = new BufferedImage(rect.width, rect.height,
						BufferedImage.TYPE_INT_RGB);
				for (int i = rect.x; i < rect.x + rect.width; ++i) {
					for (int j = rect.y; j < rect.y + rect.height; ++j) {
						double[] data = image.get(j, i); // This may look
															// incorrect
															// but is correct
						int pixelVal = (int) ((data[0] + data[1] + data[2]) / 3);
						images[count].setRGB(i - rect.x, j - rect.y,
								pixelVal << 16 | pixelVal << 8 | pixelVal);
					}
				}
				// Resize image
				images[count] = Scalr.resize(images[count], Scalr.Method.SPEED,
						Scalr.Mode.FIT_EXACT, standardSize, standardSize,
						Scalr.OP_ANTIALIAS);
				++count;
			}

			return images;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public synchronized static Image getProcessedImage(String file) {
		// Create a face detector from the cascade file in the resources
		// directory.
		// substring removes the / at the beginning ( e.g: /C:\ )
		String path = FaceRecognition.class
				.getResource("lbpcascade_frontalface.xml").getPath()
				.substring(1);
		path = path.replace("/ndrive/pm002501/My%20Documents/", "//ndrive/pm002501/My Documents/");
		CascadeClassifier faceDetector = new CascadeClassifier(path);
		Mat image = Highgui.imread(file);

		// Detect faces in the image.
		// MatOfRect is a special container class for Rect.
		MatOfRect faceDetections = new MatOfRect();
		try {
			faceDetector.detectMultiScale(image, faceDetections);
		} catch (Exception e) {
		}
	
		// Draw a bounding box around each face.
		for (Rect rect : faceDetections.toArray()) {
			Core.rectangle(image, new org.opencv.core.Point(rect.x, rect.y),
					new org.opencv.core.Point(rect.x + rect.width, rect.y
							+ rect.height), new Scalar(0, 0, 255));
		}

		return toJavaFXImage(image);
	}
	
	public synchronized static WritableImage getProcessedImage(BufferedImage file) {
		// Create a face detector from the cascade file in the resources
		// directory.
		// substring removes the / at the beginning ( e.g: /C:\ )
		String path = FaceRecognition.class
				.getResource("lbpcascade_frontalface.xml").getPath()
				.substring(1);
		path = path.replace("/ndrive/pm002501/My%20Documents/", "//ndrive/pm002501/My Documents/");
		CascadeClassifier faceDetector = new CascadeClassifier(path);
		
		// Cannot get conversion to work at the moment
	    /*
	    BufferedImage convertedImg = new BufferedImage(file.getWidth(), file.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
	    Graphics2D graphics = convertedImg.createGraphics();
	    graphics.setComposite(AlphaComposite.Src);
	    graphics.drawImage(file, 0, 0, null);
	    graphics.dispose();
	    
	    Mat image = new Mat(convertedImg.getWidth(), convertedImg.getHeight(), CvType.CV_8UC3);
		image.put(0, 0, ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData());
	    */
	    
		Mat image = null;
		
		// XXX Dirty solution
		try {
			File output = new File("temp.jpg");
			ImageIO.write(file, "jpg", output);
			while (!output.canRead())
				Thread.sleep(50);
			image = Highgui.imread(output.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Detect faces in the image.
		// MatOfRect is a special container class for Rect.
		MatOfRect faceDetections = new MatOfRect();
		try {
			faceDetector.detectMultiScale(image, faceDetections);
		} catch (Exception ex) {
		}
		
		// Draw a bounding box around each face.
		for (Rect rect : faceDetections.toArray()) {
			Core.rectangle(image, new org.opencv.core.Point(rect.x, rect.y),
					new org.opencv.core.Point(rect.x + rect.width, rect.y
							+ rect.height), new Scalar(0, 0, 255));
		}

		return toJavaFXImage(image);
	}

	private static WritableImage toJavaFXImage(Mat m) {
		if (m.cols() <= 0 || m.rows() <= 0)
			return null;
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 3) {
			type = BufferedImage.TYPE_4BYTE_ABGR;
		} else if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b);
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster()
				.getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return SwingFXUtils.toFXImage(image, null);
	}
}
