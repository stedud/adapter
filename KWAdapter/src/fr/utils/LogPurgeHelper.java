package fr.utils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import fr.kw.adapter.Version;

public class LogPurgeHelper {

	public LogPurgeHelper() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * The main logger for this class.
	 */
	protected static final Logger LOGGER = Logger.getLogger("PURGE");
	static {
		Handler[] handlers = LOGGER.getHandlers();
		for (Handler h : handlers)
			LOGGER.removeHandler(h);
		LOGGER.setUseParentHandlers(false);

		try {
			LOGGER.setLevel(Level.INFO);
			File logFolder = new File("log");
			logFolder.mkdirs();
			FileHandler fh = new FileHandler("log" + File.separator + "KWAdapterPurge.%u.%g.txt", 1024 * 1024 * 50, 2,true);

//			fh = new FileHandler("log" + File.separator + "KWAdapter.%u.%g.txt", 1024*1024*50, 2);
			fh.setLevel(Level.INFO);
			fh.setFormatter(getLogFormatter());
			LOGGER.addHandler(fh);

			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.INFO);
			ch.setFormatter(getLogFormatter());

			LOGGER.addHandler(ch);
			LOGGER.info("logger started");
			LOGGER.info("Adapter version : " + Version.version);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void setLevel(Level level) {
		LOGGER.setLevel(level);
		for (Handler h : LOGGER.getHandlers()) {
			h.setLevel(level);
		}

	}

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	public static void info(String msg, Throwable t) {
		LOGGER.info(msg);
		LOGGER.info(getStackTrace(t));
	}

	public static void debug(String msg) {
		LOGGER.finest(msg);
	}

	public static void debug(String msg, Throwable t) {
		LOGGER.finest(msg);
		LOGGER.finest(getStackTrace(t));
	}

	public static void warn(String msg) {
		LOGGER.warning(msg);
	}

	public static void warn(String msg, Throwable t) {
		LOGGER.warning(msg);
		LOGGER.warning(getStackTrace(t));
	}

	public static void error(String msg) {
		LOGGER.severe(msg);
	}

	public static void error(String msg, Throwable t) {
		LOGGER.severe(msg);
		LOGGER.severe(getStackTrace(t));
	}

	public static String getStackTrace(Throwable t) {
		StringBuffer sb = new StringBuffer(t.getMessage() != null ? t.getMessage() : "No message");
		Throwable cause = t.getCause();
		while (cause != null) {
			if (sb.length() > 0)
				sb.append("\n");

			sb.append(cause.getMessage());
			cause = cause.getCause();
		}
		for (StackTraceElement stack : t.getStackTrace()) {
			if (sb.length() > 0)
				sb.append("\n");
			sb.append(stack);
		}

		return sb.toString();
	}

	protected static Formatter getLogFormatter() {
		return new SimpleFormatter() {

			@Override
			public String format(LogRecord lr) {
				if (lr != null) {

					StringBuffer sb = new StringBuffer();
					sb.append("[");
					sb.append(displayTimeStamp(lr.getMillis()));
					sb.append("]-[");
					sb.append(lr.getLevel().getName());
					sb.append("          ".substring(0, 7 - lr.getLevel().getName().length()));
					sb.append("]- ");
					sb.append(lr.getMessage());
					sb.append("\n");
					sb.toString();
					return sb.toString();
				} else {
					return "";
				}
			}
		};
	}

	public static String displayTimeStamp(long timestamp) {
		LocalDateTime date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
		return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

}
