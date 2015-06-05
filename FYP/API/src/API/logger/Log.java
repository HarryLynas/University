package API.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public final class Log {
	
	/**
	 * This class cannot be instantiated.
	 */
	private Log() {
		throw new AssertionError();
	}
	
	/**
	 * Store for all messages waiting to be printed.
	 */
	private static final ArrayList<String> messages = new ArrayList<String>();
	/**
	 * The formatter for adding timestamps to messages.
	 */
	private static final SimpleDateFormat formater = new SimpleDateFormat("HH.mm.ss");
	
	/**
	 * Add a new message to the list of messages waiting to be printed.
	 * @param text The string to log.
	 */
	public static final void append(String text) {
		synchronized (messages) {
			messages.add("[" + formater.format(new Date())
					+ " - " + Thread.currentThread().getName() + "] " + text);
		}
	}
	
	/**
	 * Get all the messages currently waiting to be printed.
	 * Calling this method clears the list of waiting messages.
	 * @return All messages to be printed.
	 */
	public static final String[] getMessages() {
		synchronized (messages) {
			String[] ret = messages.toArray(new String[messages.size()]);
			messages.clear();
			return ret;
		}
	}
}
