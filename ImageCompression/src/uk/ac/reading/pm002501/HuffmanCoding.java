package uk.ac.reading.pm002501;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

public class HuffmanCoding implements Runnable {
	// The file being processed
	private File file;
	// The string dictionary for mapping bits to huffman codes
	private String[] table = null;
	// The compressed buffered image decompressed
	private BufferedImage image = null;
	// The size of the compressed image
	private long size = -1;

	/**
	 * Constructor - takes a file.
	 * 
	 * @param file
	 *            The file to be processed.
	 */
	public HuffmanCoding(File file) {
		this.file = file;
	}

	/**
	 * Get the decompressed image.
	 * 
	 * @return The decompressed image.
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Gets the size of the compresed image.
	 * 
	 * @return The size of the compresesd image.
	 */
	public long getSize() {
		return size;
	}

	@Override
	/**
	 * Called when the thread is started.
	 */
	public void run() {
		try {
			{
				// Read Image
				BufferedImage image = ImageIO.read(file);
				// Get the pixels
				double[] pixels = getPixels(image);
				// Extract all the R, G, B from the pixels
				int[] r = new int[pixels.length / 3];
				int[] g = new int[pixels.length / 3];
				int[] b = new int[pixels.length / 3];
				int c = 0;
				for (int i = 0; i < pixels.length; i += 3) {
					r[c] = (int) pixels[i];
					g[c] = (int) pixels[i + 1];
					b[c] = (int) pixels[i + 2];
					++c;
				}
				// encode the pixels
				byte[] br = encode(r);
				byte[] bg = encode(g);
				byte[] bb = encode(b);
				// Write everything
				FileOutputStream fs = new FileOutputStream(new File(
						file.getPath() + "_new.lynas"));
				// Write dimensions
				fs.write((byte) (image.getWidth() >>> 24));
				fs.write((byte) (image.getWidth() >>> 17));
				fs.write((byte) (image.getWidth() >>> 8));
				fs.write((byte) (image.getWidth()));
				fs.write((byte) (image.getHeight() >>> 24));
				fs.write((byte) (image.getHeight() >>> 17));
				fs.write((byte) (image.getHeight() >>> 8));
				fs.write((byte) (image.getHeight()));
				// Write data
				fs.write(br);
				fs.write(bg);
				fs.write(bb);
				fs.close();
			}
			// Read the compressed file back in
			FileInputStream fs = new FileInputStream(new File(file.getPath()
					+ "_new.lynas"));
			// Get how many bytes are available
			int avail = fs.available();
			// Where to store the bytes
			ArrayList<Byte> bytes = new ArrayList<Byte>();
			// While there is more bytes available
			while (avail != 0) {
				// Store what is read
				byte[] reading = new byte[avail];
				// Try to read, get how much is actually read
				int numRead = fs.read(reading);
				// Get how much left is availble
				avail = fs.available();
				// Add what was read
				for (int i = 0; i < numRead; ++i)
					bytes.add(reading[i]);
			}
			// The size of the compresed image
			this.size = bytes.size();
			// Close the input stream
			fs.close();
			// Convert to native data type
			byte[] toWrap = new byte[bytes.size()];
			int count = 0;
			for (Byte b : bytes)
				toWrap[count++] = b;
			// Use a wrapper to
			ByteBuffer buffer = ByteBuffer.wrap(toWrap);

			// Image width / height
			int width = buffer.getInt();
			int height = buffer.getInt();
			int[][] rgb = new int[3][width * height];

			// For each channel in the image (R, G, B)
			for (int channel = 0; channel < 3; ++channel) {
				// Get the frequency pairs
				ArrayList<FrequencyVal> sortedMap = new ArrayList<FrequencyVal>();
				int numPairs = buffer.getInt();
				for (int i = 0; i < numPairs; ++i) {
					int freq = buffer.getInt();
					short val = buffer.getShort();
					sortedMap.add(new FrequencyVal(val, freq));
				}

				// While the map still has > 1 element, keep tree'ing
				while (sortedMap.size() > 1) {
					// Always sort the list first
					sortList(sortedMap);
					FrequencyVal a = sortedMap.get(sortedMap.size() - 1);
					sortedMap.remove(a);
					FrequencyVal b = sortedMap.get(sortedMap.size() - 1);
					sortedMap.remove(b);
					FrequencyVal c = new FrequencyVal((short) -1, a.count
							+ b.count);
					if (a.count < b.count) {
						c.left = a;
						c.right = b;
					} else {
						c.left = b;
						c.right = a;
					}
					sortedMap.add(c);
				}

				// Complete map is now done
				FrequencyVal root = sortedMap.get(0);
				sortedMap = null;

				// Get all pixels
				FrequencyVal current = root;
				int size = width * height;
				byte b = buffer.get();
				int p = 7;
				for (int i = 0; i < size; ++i) {
					if (current.isLeaf()) {
						rgb[channel][i] = current.val;
						current = root;
						continue;
					}
					if (p < 0) {
						p = 7;
						b = buffer.get();
					}
					if (isBitSet(b, p)) { // right is 1
						current = current.right;
					} else {
						current = current.left;
					}
					--p;
					--i;
				}
			}
			// Create the uncompresed image
			BufferedImage image = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			int pos = 0;
			// Set each pixel
			for (int j = 0; j < height; ++j) {
				for (int i = 0; i < width; ++i) {
					Color color = new Color(rgb[0][pos], rgb[1][pos],
							rgb[2][pos]);
					image.setRGB(i, j, color.getRGB());
					pos++;
				}
			}
			this.image = image;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return whether a bit is a 1 or a 0 at a position in a byte.
	 * 
	 * @param b
	 *            The byte.
	 * @param bit
	 *            The bit position (0-indexed).
	 * @return True if bit is a 1.
	 */
	private boolean isBitSet(byte b, int bit) {
		return (b & (1 << bit)) != 0;
	}

	/**
	 * Encode the pixel data.
	 * 
	 * @param data
	 *            The pixel values for a channel.
	 * @return The encoded data.
	 */
	private byte[] encode(int[] data) {
		// Create the frequency map
		HashMap<Short, Integer> map = new HashMap<Short, Integer>();
		for (int d : data) {
			Integer count = map.get((short) d);
			if (count == null)
				map.put((short) d, 1);
			else
				map.put((short) d, count + 1);
		}
		// Sort the frequency map
		ArrayList<FrequencyVal> sortedList = new ArrayList<FrequencyVal>();
		for (Entry<Short, Integer> entry : map.entrySet())
			sortedList.add(new FrequencyVal(entry.getKey(), entry.getValue()));
		map = null;

		// The bit data
		ArrayList<Byte> bytes = new ArrayList<Byte>();

		// Write out the tree
		// num leafs
		// Number of leafs
		int size = sortedList.size();
		bytes.add((byte) (size >>> 24));
		bytes.add((byte) (size >>> 17));
		bytes.add((byte) (size >>> 8));
		bytes.add((byte) (size));
		// tree
		writeOutLeafs(bytes, sortedList);

		// While the list still has > 1 element, keep tree'ing
		while (sortedList.size() > 1) {
			// Always sort the list first
			sortList(sortedList);
			FrequencyVal a = sortedList.get(sortedList.size() - 1);
			sortedList.remove(a);
			FrequencyVal b = sortedList.get(sortedList.size() - 1);
			sortedList.remove(b);
			FrequencyVal c = new FrequencyVal((short) -1, a.count + b.count);
			if (a.count < b.count) {
				c.left = a;
				c.right = b;
			} else {
				c.left = b;
				c.right = a;
			}
			sortedList.add(c);
		}

		// Complete map is now done
		FrequencyVal root = sortedList.get(0);
		sortedList = null;

		// Debug
		// printBinaryTree(root, 0);
		// System.out.println();

		// Build string table
		table = new String[256]; // 255 is max val of colour
		for (int i = 0; i < 256; ++i)
			table[i] = "";
		buildLookupTable(table, root, "");

		// Get bit data for all the pixels
		for (int pixel : data) {
			String huf = table[pixel];
			for (char x : huf.toCharArray())
				writeBit(x == '1', bytes);
		}
		clearBuffer(bytes);

		// Cast and return
		byte[] returnVal = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); ++i)
			returnVal[i] = bytes.get(i);
		return returnVal;
	}

	/**
	 * Bubble sort a list of frequency values by frequency.
	 * 
	 * @param sortedList
	 *            The list to be sorted.
	 */
	private void sortList(ArrayList<FrequencyVal> sortedList) {
		int size = sortedList.size();
		boolean sorted = false;
		while (!sorted) {
			sorted = true;
			for (int i = 0; i < size - 1; ++i) {
				FrequencyVal a = sortedList.get(i);
				FrequencyVal b = sortedList.get(i + 1);
				if (a.count < b.count) {
					sortedList.set(i, b);
					sortedList.set(i + 1, a);
					sorted = false;
					break;
				}
			}
		}
	}

	/**
	 * Write out each frequency value.
	 * 
	 * @param bytes
	 *            The bytes to write it to.
	 * @param nodes
	 *            The frequency values to write.
	 */
	private void writeOutLeafs(ArrayList<Byte> bytes,
			ArrayList<FrequencyVal> nodes) {
		for (FrequencyVal f : nodes) {
			int c = f.count;
			bytes.add((byte) (c >>> 24));
			bytes.add((byte) (c >>> 17));
			bytes.add((byte) (c >>> 8));
			bytes.add((byte) (c));
			bytes.add((byte) ((f.val >> 8) & 0xff));
			bytes.add((byte) (f.val & 0xff));
		}
	}

	private int buffer = 0; // 8-bit buffer of bits to write out
	private int N = 0; // number of bits remaining in buffer

	private void writeBit(boolean bit, ArrayList<Byte> stream) {
		// add bit to buffer
		buffer <<= 1;
		if (bit)
			buffer |= 1;

		// if buffer is full (8 bits), write out as a single byte
		++N;
		if (N == 8)
			clearBuffer(stream);
	}

	// write out any remaining bits in buffer to standard output, padding with
	// 0s
	private void clearBuffer(ArrayList<Byte> stream) {
		if (N == 0)
			return;
		if (N > 0)
			buffer <<= (8 - N);
		stream.add((byte) buffer);
		N = 0;
		buffer = 0;
	}

	/**
	 * Build the dictionary recursively given a root node.
	 * 
	 * @param table
	 *            The dictionary to be populated.
	 * @param node
	 *            The root node.
	 * @param str
	 *            An empty string.
	 */
	private void buildLookupTable(String[] table, FrequencyVal node, String str) {
		if (node.isLeaf()) {
			table[node.val] = str;
		} else {
			if (node.left != null)
				buildLookupTable(table, node.left, str + "0");
			if (node.right != null)
				buildLookupTable(table, node.right, str + "1");
		}
	}

	/**
	 * Store a pixel value and how many times it occurs.
	 */
	protected class FrequencyVal implements Comparable<FrequencyVal> {
		public short val;
		public int count;
		public FrequencyVal left;
		public FrequencyVal right;

		public FrequencyVal(short val, int count) {
			this.val = val;
			this.count = count;
			left = null;
			right = null;
		}

		public boolean isLeaf() {
			return left == null && right == null;
		}

		@Override
		public int compareTo(FrequencyVal o) {
			if (count > o.count)
				return -1;
			else if (count < o.count)
				return 1;
			else if (val > o.val)
				return -1;
			else if (val < o.val)
				return 1;
			return -1;
		}
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
}
