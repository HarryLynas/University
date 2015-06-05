package uk.ac.reading.pm002501;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageCompression {

	// Factor can be 2 or 3
	private final static int factor = 2;

	public ImageCompression(String path) throws Exception {
		BufferedImage image = ImageIO.read(new File(path));

		if (image.getType() != BufferedImage.TYPE_3BYTE_BGR)
			throw new Exception(
					"Image is not BGR - only BGR currently supported.");

		// Get the pixels
		int[] pixels = image.getRaster().getPixels(0, 0, image.getWidth(),
				image.getHeight(),
				new int[(image.getWidth() * image.getHeight()) * 3]);

		// Convert image to YCbCr colour space
		int ycbcr[][] = new int[pixels.length / 3][3];
		int c = 0;
		for (int i = 0; i < pixels.length; i += 3) {
			Color colour = new Color(pixels[i + 2], pixels[i + 1], pixels[i]); // BGR
			ycbcr[c++] = RGBToYCbCr(colour.getRed(), colour.getGreen(),
					colour.getBlue());
		}

		// Reduce Cb and Cr by a factor
		for (int i = 0; i < ycbcr.length; ++i) {
			ycbcr[i][1] = (ycbcr[i][1] / factor) * factor;
			ycbcr[i][2] = (ycbcr[i][2] / factor) * factor;
		}

		// Perform DCT on 8x8 blocks
		for (int x = 7; x < image.getWidth(); x += 8) {
			for (int y = 7; y < image.getHeight(); y += 8) {
				int[][] block = new int[64][3];
				c = 64;
				for (int i = x; i > (x - 8); --i) {
					for (int j = y; j > (y - 8); --j) {
						block[--c] = ycbcr[i + j * image.getWidth()];
					}
				}
				block = performDCTOnBlock(block);
				c = 64;
				for (int i = x; i > (x - 8); --i) {
					for (int j = y; j > (y - 8); --j) {
						ycbcr[i + j * image.getWidth()] = block[--c];
					}
				}
			}
		}

		// Convert back to RGB
		c = 0;
		for (int i = 0; i < ycbcr.length; ++i) {
			int[] rgb = YCbCrToRGB(ycbcr[i]);
			Color colour = new Color(rgb[0], rgb[1], rgb[2]);
			pixels[c++] = colour.getBlue();
			pixels[c++] = colour.getGreen();
			pixels[c++] = colour.getRed();
		}

		// Set image pixels
		image.getRaster().setPixels(0, 0, image.getWidth(), image.getHeight(),
				pixels);

		// Save to file
		ImageIO.write(image, "bmp", new File(path + "_DCT.bmp"));

		// Perform IDCT on 8x8 blocks
		for (int x = 7; x < image.getWidth(); x += 8) {
			for (int y = 7; y < image.getHeight(); y += 8) {
				int[][] block = new int[64][3];
				c = 64;
				for (int i = x; i > (x - 8); --i) {
					for (int j = y; j > (y - 8); --j) {
						block[--c] = ycbcr[i + j * image.getWidth()];
					}
				}
				block = performIDCTOnBlock(block);
				c = 64;
				for (int i = x; i > (x - 8); --i) {
					for (int j = y; j > (y - 8); --j) {
						ycbcr[i + j * image.getWidth()] = block[--c];
					}
				}
			}
		}

		// Convert back to RGB
		c = 0;
		for (int i = 0; i < ycbcr.length; ++i) {
			int[] rgb = YCbCrToRGB(ycbcr[i]);
			Color colour = new Color(rgb[0], rgb[1], rgb[2]);
			pixels[c++] = colour.getBlue();
			pixels[c++] = colour.getGreen();
			pixels[c++] = colour.getRed();
		}

		// Set image pixels
		image.getRaster().setPixels(0, 0, image.getWidth(), image.getHeight(),
				pixels);

		// Save to file
		ImageIO.write(image, "bmp", new File(path + "_IDCT.bmp"));
	}

	// JPEG Quantization Matrix
	// http://www.dei.unipd.it/~neviani/did/cit/aa06-07/docpro07aut.pdf
	private static double[][] quantisationMatrix = {
			{ 3, 5, 7, 9, 11, 13, 15, 17 }, { 5, 7, 9, 11, 13, 15, 17, 19 },
			{ 7, 9, 11, 13, 15, 17, 19, 21 },
			{ 9, 11, 13, 15, 17, 19, 21, 23 },
			{ 11, 13, 15, 17, 19, 21, 23, 25 },
			{ 13, 15, 17, 19, 21, 23, 25, 27 },
			{ 15, 17, 19, 21, 23, 25, 27, 39 },
			{ 17, 19, 21, 23, 25, 27, 29, 31 } };

	private int[][] performDCTOnBlock(int[][] block) {
		for (int channel = 0; channel < 3; ++channel) {
			// Get the 2d image for that channel
			double[][] dBlock = new double[8][8];
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					dBlock[i][j] = block[i + j * 8][channel];
				}
			}
			// Apply DCT
			DCT dct = new DCT();
			dBlock = dct.applyDCT(dBlock);
			// Quantitise
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					dBlock[i][j] = (int) dBlock[i][j]
							/ quantisationMatrix[i][j];
				}
			}

			// Convert back to 1d
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					block[i + j * 8][channel] = (int) dBlock[i][j];
				}
			}
		}

		return block;
	}

	private int[][] performIDCTOnBlock(int[][] block) {
		for (int channel = 0; channel < 3; ++channel) {
			// Get the 2d image for that channel
			double[][] dBlock = new double[8][8];
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					dBlock[i][j] = block[i + j * 8][channel];
				}
			}

			// Dequantitise
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					dBlock[i][j] = (int) dBlock[i][j]
							* quantisationMatrix[i][j];
				}
			}
			// Inverse
			DCT dct = new DCT();
			dBlock = dct.applyIDCT(dBlock);

			// Convert back to 1d
			for (int i = 0; i < 8; ++i) {
				for (int j = 0; j < 8; ++j) {
					block[i + j * 8][channel] = (int) dBlock[i][j];
				}
			}
		}

		return block;
	}

	private int[] RGBToYCbCr(int r, int g, int b) {
		int ret[] = new int[3];
		/*
		 * From MatLab
		 * 
		 * Y = 0.257R´ + 0.504G´ + 0.098B´ + 16
		 * 
		 * Cb = -0.148R´ - 0.291G´ + 0.439B´ + 128
		 * 
		 * Cr = 0.439R´ - 0.368G´ - 0.071B´ + 128
		 */
		ret[0] = (int) ((0.257 * r) + (0.504 * g) + (0.098 * b) + 16);
		ret[1] = (int) ((-0.148 * r) - (0.291 * g) + (0.439 * b) + 128);
		ret[2] = (int) ((0.439 * r) - (0.368 * g) - (0.071 * b) + 128);
		return ret;
	}

	private int[] YCbCrToRGB(int[] YCbCr) {
		double Y = YCbCr[0];
		double Cb = YCbCr[1];
		double Cr = YCbCr[2];

		/*
		 * From MatLab
		 * 
		 * R´ = 1.164(Y - 16) + 1.596(Cr - 128)
		 * 
		 * G´ = 1.164(Y - 16) - 0.813(Cr - 128) - 0.392(Cb - 128)
		 * 
		 * B´ = 1.164(Y - 16) + 2.017(Cb - 128)
		 */
		int r = (int) ((1.164 * (Y - 16)) + (1.596 * (Cr - 128)));
		int g = (int) ((1.164 * (Y - 16)) - (0.813 * (Cr - 128)) - (0.392 * (Cb - 128)));
		int b = (int) ((1.164 * (Y - 16)) + (2.017 * (Cb - 128)));

		// Normalise results
		r = Math.max(0, Math.min(255, r));
		g = Math.max(0, Math.min(255, g));
		b = Math.max(0, Math.min(255, b));

		return new int[] { r, g, b };
	}
}
