package uk.ac.reading.pm002501;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;

public class RunLength implements Runnable {
	// File being read
	private File file;
	// Compressed image size
	private long compressedBytes = 0;
	// The compressed image bytes
	byte[] compressed = new byte[0];

	/**
	 * Set the file to be used.
	 * 
	 * @param file
	 */
	public RunLength(File file) {
		this.file = file;
	}

	@Override
	/**
	 * Called when thread is started.
	 */
	public void run() {
		try {
			// Read Image
			BufferedImage image = ImageIO.read(file);
			// Get the pixels
			double[] pixels = getPixels(image);
			// Extract all the R, G, B from the pixels
			double[] r = new double[pixels.length / 3];
			double[] g = new double[pixels.length / 3];
			double[] b = new double[pixels.length / 3];
			int c = 0;
			for (int i = 0; i < pixels.length; i += 3) {
				r[c] = pixels[i];
				g[c] = pixels[i + 1];
				b[c] = pixels[i + 2];
				++c;
			}
			// Compress the RGB
			compressed = compressRGB(image.getWidth(), image.getHeight(), r, g,
					b);
			// Set the new file size
			compressedBytes = compressed.length;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compress RGB using RunLength into a byte form.
	 * 
	 * @param width
	 *            Width of the image.
	 * @param height
	 *            Height of the image.
	 * @param r
	 *            Red channel.
	 * @param g
	 *            Green channel.
	 * @param b
	 *            Blue channel.
	 * @return Compressed bytes.
	 */
	private byte[] compressRGB(int width, int height, double[] r, double[] g,
			double[] b) {
		// The byte buffer
		ArrayList<Byte> byteBuffer = new ArrayList<Byte>();
		// File header
		byte[] header = "HL".getBytes();
		for (byte by : header)
			byteBuffer.add(by);
		// Write file dimensions
		short nWidth = (short) width;
		short nHeight = (short) height;
		byteBuffer.add((byte) ((nWidth >> 8) & 0xff));
		byteBuffer.add((byte) nWidth);
		byteBuffer.add((byte) ((nHeight >> 8) & 0xff));
		byteBuffer.add((byte) nHeight);
		// Encode R, G, and B
		encode(byteBuffer, r);
		encode(byteBuffer, g);
		encode(byteBuffer, b);
		// Return the buffer, convert to byte primitive data type
		byte[] returnVal = new byte[byteBuffer.size()];
		for (int i = 0; i < returnVal.length; ++i)
			returnVal[i] = byteBuffer.get(i);
		return returnVal;
	}

	/**
	 * Encode the buffer with pixel data
	 * 
	 * @param byteBuffer
	 * @param values
	 */
	private void encode(ArrayList<Byte> byteBuffer, double[] values) {
		short currentVal = (short) values[0];
		short count = 0;
		for (int i = 0; i < values.length; ++i) {
			if (currentVal == values[i]) {
				if (++count < Short.MAX_VALUE)
					continue;
			}
			// Write count and value
			byteBuffer.add((byte) ((count >> 8) & 0xff));
			byteBuffer.add((byte) (count & 0xff));
			byteBuffer.add((byte) ((currentVal >> 8) & 0xff));
			byteBuffer.add((byte) (currentVal & 0xff));
			// Set for next write
			count = 1;
			currentVal = (short) values[i];
		}
		// Write count and value
		byteBuffer.add((byte) ((count >> 8) & 0xff));
		byteBuffer.add((byte) (count & 0xff));
		byteBuffer.add((byte) ((currentVal >> 8) & 0xff));
		byteBuffer.add((byte) (currentVal & 0xff));
	}

	/**
	 * Return all the pixels in the image in a single array with r, g, b stores
	 * as seperate values.
	 * 
	 * @param image
	 * @return The pixel array.
	 */
	private double[] getPixels(BufferedImage image) {
		double[] array = null;
		return image.getRaster().getPixels(0, 0, image.getWidth(),
				image.getHeight(), array);
	}

	/**
	 * Returns the compressed image size.
	 * 
	 * @return
	 */
	public long getSize() {
		return compressedBytes;
	}

	/**
	 * Return the bytes of the compressed image.
	 * 
	 * @return
	 */
	public byte[] getBytes() {
		return compressed;
	}

	/**
	 * Decode the bytes of a compressed image.
	 * 
	 * @param bytes
	 *            The bytes.
	 * @return The decompressed image.
	 * @throws Exception
	 */
	public Image decode(byte[] bytes) throws Exception {
		// Wrap the bytes for reading
		ByteBuffer wrapped = ByteBuffer.wrap(bytes);
		// Check the header
		if ((char) (wrapped.get()) != 'H' || (char) (wrapped.get()) != 'L')
			throw new Exception(
					"Header is not present - this is not a LYNAS file.");
		// Get the dimensions
		int width = wrapped.getShort();
		int height = wrapped.getShort();
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		int size = width * height;
		int[] r = new int[size];
		int[] g = new int[size];
		int[] b = new int[size];
		// Decode RGB channels
		decodeRGB(wrapped, r, g, b);
		// Set pixels in image
		int pos = 0;
		for (int j = 0; j < height; ++j) {
			for (int i = 0; i < width; ++i) {
				int rgb = ((r[pos] & 0x0ff) << 16) | ((g[pos] & 0x0ff) << 8)
						| (b[pos] & 0x0ff);
				image.setRGB(i, j, rgb);
				++pos;
			}
		}
		// Cast to JavaFX Image
		return SwingFXUtils.toFXImage(image, null);
	}

	/**
	 * Decode the RGB channels from a compressed image.
	 * 
	 * @param wrapped
	 *            The byte wrapper.
	 * @param r
	 *            Red channel.
	 * @param g
	 *            Green channel.
	 * @param b
	 *            Blue channel.
	 */
	private void decodeRGB(ByteBuffer wrapped, int[] r, int[] g, int[] b) {
		for (int i = 0; i < r.length; ++i) {
			int count = wrapped.getShort();
			int val = wrapped.getShort();
			while (count > 0) {
				--count;
				r[i] = val;
				++i;
			}
			--i;
		}
		for (int i = 0; i < r.length; ++i) {
			int count = wrapped.getShort();
			int val = wrapped.getShort();
			while (count > 0) {
				--count;
				g[i] = val;
				++i;
			}
			--i;
		}
		for (int i = 0; i < r.length; ++i) {
			int count = wrapped.getShort();
			int val = wrapped.getShort();
			while (count > 0) {
				--count;
				b[i] = val;
				++i;
			}
			--i;
		}
	}
}
