package uk.ac.reading.pm002501.mapreduce;

import java.io.PrintWriter;
import java.util.Date;

public final class Log {
	/**
	 * This class cannot be instantiated.
	 */
	private Log() {
		throw new AssertionError("This class cannot be instantiated.");
	}

	/**
	 * The writer that outputs to a file.
	 */
	private static PrintWriter writer;

	// Called on program start up
	static {
		try {
			writer = new PrintWriter("log.txt", "UTF-8");
			writer.println("Program started at: " + new Date().toString()
					+ "\n");
		} catch (Exception e) {
			e.printStackTrace();
			writer = null;
		}
	}

	/**
	 * Log a message to the logger. The log will write the message to a file and
	 * to the default system output. A new line is attached to the end of the
	 * message.
	 * 
	 * @param message The string message to be logged.
	 */
	public synchronized static void log(String message) {
		System.out.println(message);
		writer.println(message);
		writer.flush();
	}

	/**
	 * This must be called at the end of the program to properly dispose of file
	 * resources.
	 */
	public static void close() {
		if (writer != null)
			writer.close();
	}
}
