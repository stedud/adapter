package fr.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.coreoz.wisp.schedule.FixedDelaySchedule;
import com.coreoz.wisp.schedule.Schedule;
import com.coreoz.wisp.schedule.cron.CronSchedule;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import fr.freemarker.FreeMarkerException;
import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DocumentActions;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.adapter.document.configuration.DocumentConfigurationException;
import fr.kw.adapter.parser.process.ParseProcess;
import fr.utils.configuration.ConfigurationException;
import freemarker.ext.dom.NodeModel;

public class Utils {
	public static final String FFRegHex = "\\x0D\\x0C|\\x0C";
	public static final String LFRegHex = "\\x0D\\x0A|\\x0A";
	public Utils() {
		// TODO Auto-generated constructor stub
	}

	public static String removeSurrounding(String text, String toRemove) {

		return StringUtils.removeEnd(StringUtils.removeStart(text, toRemove), toRemove);

	}

	public static long getFileSize(FileObject f) {

		FileContent content = null;
		try {
			f.refresh();
			content = f.getContent();
			long fileSize = content.getSize();
			return fileSize;
		} catch (FileSystemException e) {
			try {
				File standardFile = new File(f.getURL().toString());
				return standardFile.length();
			} catch (FileSystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} finally {
			if (content != null)
				try {
					content.close();
				} catch (FileSystemException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		return -1;
	}

	public static int getInt(String intValue) {
		int value;

		try {
			if (intValue == null) {
				value = 0;
			} else if (intValue.compareToIgnoreCase("max") == 0) //$NON-NLS-1$
				value = Integer.MAX_VALUE;
			else if (intValue.compareTo("") == 0) { //$NON-NLS-1$
				value = 0;
			} else {
				value = Integer.parseInt(intValue);
			}

		} // end try
		catch (Exception e) {
			value = 0;
		} // end catch

		return value;
	} // end getInt()

	public static int getPercentMemoryUse() {
		long maxM = java.lang.Runtime.getRuntime().maxMemory();
		long totalM = java.lang.Runtime.getRuntime().totalMemory();
		long freeM = java.lang.Runtime.getRuntime().freeMemory();
		// long freeMMb = freeM/(1024*1024);
		double percentUse = (((double) (totalM - freeM) / (double) maxM) * 100.0);
		Double percentUseObj = new Double(percentUse);
		int percentUseInt = percentUseObj.intValue();
		return percentUseInt;
	}

	public static String getMimeType(String fileName) {
		FileTypeMap fileTypeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();
		String mimeType = fileTypeMap.getContentType(fileName);
		return mimeType;
	}

	public static void saveXML(Source source, File destination) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();
		transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transf.setOutputProperty(OutputKeys.INDENT, "yes");
		transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StreamResult file = new StreamResult(destination);
		transf.transform(source, file);
	}

	public static void saveXML(Source source, OutputStream destination) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();
		transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transf.setOutputProperty(OutputKeys.INDENT, "yes");
		transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StreamResult file = new StreamResult(destination);
		transf.transform(source, file);
		try {
			file.getOutputStream().flush();
			file.getOutputStream().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getStringFromXmlDocument(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static String parseSingleArgKeyWord(String line, String keyWord) {// labelStartEvent "BEGIN";
		String value = line.substring(keyWord.length()).trim();
		value = StringUtils.removeStart(StringUtils.removeEnd(value, "\""), "\"");
		return value;
	}

	/**
	 * Returns a Scanner to read the file page by page (separator 0D0A or 0A). The
	 * scanner must be closed when the file processing is finished.
	 * 
	 * @param fileToScan
	 * @param charset
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Scanner getPageScanner(File fileToScan, String charset) throws FileNotFoundException {
	
		Scanner scanner = new Scanner(fileToScan, charset).useDelimiter(FFRegHex);
		return scanner;
	
	}

	/**
	 * Returns a Scanner to read the file line by line (separator 0D0C or 0C). The
	 * scanner must be closed when the file processing is finished.
	 * 
	 * @param fileToScan
	 * @param charset
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Scanner getLineScanner(File fileToScan, String charset) throws FileNotFoundException {
	
		Scanner scanner = new Scanner(fileToScan, charset).useDelimiter(LFRegHex);
	
		return scanner;
	}

	public static void pipeStream(InputStream input, OutputStream output) throws IOException {
		if (!(input instanceof BufferedInputStream))
			input = new BufferedInputStream(input);
		if (!(output instanceof BufferedOutputStream))
			output = new BufferedOutputStream(output);

		byte buffer[] = new byte[1024];
		int numRead = 0;

		do {
			numRead = input.read(buffer);
			output.write(buffer, 0, numRead);
		} while (input.available() > 0);

		output.flush();
	}

	public static void saveXML(Document xmlDoc, File destination) throws TransformerException {
		DOMSource source = new DOMSource(xmlDoc);
		saveXML(source, destination);
	}

	public static void copyInputStreamToFile(InputStream is, File destination) throws IOException {

		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(is);
			bos = new BufferedOutputStream(new FileOutputStream(destination));
			byte[] buffer = new byte[4096];
			int len = -1;
			while ((len = bis.read(buffer)) >= 0) {
				bos.write(buffer, 0, len);
				bos.flush();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			try {
				if (bos != null)
					bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static String normalizeXmlName(String value) {
		if (value == null)
			return "NoName";

		if (StringUtils.isBlank(value))
			return "NoName";

		value = StringUtils.stripAccents(value.trim());

		value = value.replaceAll("[^A-Za-z0-9\\-]", "_");
		if (Character.isDigit(value.charAt(0)))
			value = "F" + value;
		return value;
	}

	public static boolean isAbsolute(String path) {
		boolean absolute = false;
		int pos = path.indexOf(':');

		absolute = (pos > -1 && pos < 8) || path.startsWith("/") || path.startsWith("\\");
		return absolute;

	}

	public static File getFile(File parent, String filePath) {
		if (isAbsolute(filePath))
			return new File(filePath);
		else
			return new File(parent, filePath);
	}

	public static File getFile(String parent, String filePath) {
		if (isAbsolute(filePath))
			return new File(filePath);
		else
			return new File(parent, filePath);
	}

	public static String getXMLFileEncoding(File file) {
		XMLStreamReader xmlStreamReader = null;
		FileReader fr = null;
		try {
			fr = new FileReader(file);
			xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(fr);
			// running on MS Windows fileEncoding is "CP1251"
			// String fileEncoding = xmlStreamReader.getEncoding();

			// the XML declares UTF-8 so encodingFromXMLDeclaration is "UTF-8"
			String encodingFromXMLDeclaration = xmlStreamReader.getCharacterEncodingScheme();
			if (encodingFromXMLDeclaration == null)
				encodingFromXMLDeclaration = "UTF-8";
			xmlStreamReader.close();
			fr.close();
			return encodingFromXMLDeclaration;
		} catch (FileNotFoundException | XMLStreamException | FactoryConfigurationError e) {

		} catch (IOException e) {
		} finally {
			try {
				if (fr != null)
					fr.close();
				if (xmlStreamReader != null)
					xmlStreamReader.close();
			} catch (IOException | XMLStreamException e) {
				;
			}
		}

		return "UTF-8";// default

	}

	public static String getStackTrace(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter printWriter = new PrintWriter(baos);
		t.printStackTrace(printWriter);
		printWriter.flush();
		printWriter.close();
		return baos.toString();
	}

	public static Schedule parseSchedule(String scheduleString) throws Exception {
		boolean cronType = false;
		Schedule schedule = null;
		CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);

		// create a parser based on provided definition
		CronParser parser = new CronParser(cronDefinition);
		try {
			Cron cronParsed = parser.parse(scheduleString);
			cronParsed.validate();
			cronType = true;
		} catch (Exception e) {
			cronType = false;
		}

		if (cronType) {// CRON

			Cron quartzCron = parser.parse("0 * * 1-3 * ? *");
			schedule = CronSchedule.parseQuartzCron(scheduleString);
		} else {// period
			try {
				Duration d = parseDuration(scheduleString);
				
				schedule = new FixedDelaySchedule(d);
			}
			catch (Exception e)
			{
					throw new Exception(
							"Unable to parse schedule '" + scheduleString + "'. Allowed values : cron expressions, value with unit (ms,s,mn,h,d), Duration.parse expression (see java api java/time/Duration.html, ex : P1D, PT10H, PT10M,...) ");
			}
			
		}

		return schedule;
	}

	public static Duration parseDuration(String durationString) throws Exception {
	
		Duration duration = null;
		
			if (durationString.toLowerCase().startsWith("p"))
			{
				duration = Duration.parse(durationString);

			}
			else
			{
				if (StringUtils.endsWithIgnoreCase(durationString, "ms")) {
					String value = StringUtils.removeEnd(durationString.toLowerCase(), "ms");
					int interval = Integer.parseInt(value);
					duration = Duration.ofMillis(interval);
				} else if (StringUtils.endsWithIgnoreCase(durationString, "s")) {
					String value = StringUtils.removeEnd(durationString.toLowerCase(), "s");
					int interval = Integer.parseInt(value);
					duration = Duration.ofSeconds(interval);
				} else if (StringUtils.endsWithIgnoreCase(durationString.toLowerCase(), "mn")) {
					String value = StringUtils.removeEnd(durationString, "mn");
					int interval = Integer.parseInt(value);
					duration = Duration.ofMinutes(interval);
				} else if (StringUtils.endsWithIgnoreCase(durationString, "h")) {
					String value = StringUtils.removeEnd(durationString.toLowerCase(), "h");
					int interval = Integer.parseInt(value);
					duration = Duration.ofHours(interval);
				} else if (StringUtils.endsWithIgnoreCase(durationString, "d")) {
					String value = StringUtils.removeEnd(durationString.toLowerCase(), "d");
					int interval = Integer.parseInt(value);
					duration = Duration.ofDays(interval);
				}  
				else {
					throw new Exception(
							"Unable to parse duration '" + durationString + "'. Allowed values : value with unit (ms,s,mn,h,d), Duration.parse expression (see java api java/time/Duration.html, ex : P1D, PT10H, PT10M,...) ");
				}
			}
		

		return duration;
	}
	
	protected static void appendContext(org.w3c.dom.Document xmlDoc, Element currentLevel, Entry<?, ?> currentEntry) {
		Object object = currentEntry.getValue();
		String name = String.valueOf(currentEntry.getKey());
		Element current = xmlDoc.createElement(Utils.normalizeXmlName(name));
		currentLevel.appendChild(current);

		if (object instanceof NodeModel) {
			NodeModel nodeModel = (NodeModel) object;
			Node node = nodeModel.getNode();

			// node = node.cloneNode(true);

			node = xmlDoc.importNode(node.getFirstChild(), true);

			current.appendChild(node);
		} else if (object instanceof Map<?, ?>) {

			for (Entry entry : ((Map<?, ?>) object).entrySet()) {
				appendContext(xmlDoc, current, entry);
			}
		} else {
			current.setTextContent(String.valueOf(object));
		}
	}
	
	public static File getTmpFolder()
	{
		
		 String tmpdir = System.getProperty("java.io.tmpdir");
		 if (StringUtils.isBlank(tmpdir)) {
			 tmpdir = "tmp";
			}
		 return new File(tmpdir);
	}

	public static void saveDocument(fr.kw.adapter.document.Document doc, DocumentConfiguration docConfig,
			File destination) throws FreeMarkerException, DocumentConfigurationException, ConfigurationException,
			IOException, ParserConfigurationException, TransformerException, DataSourceException {

		DocumentActions actions = doc.getDocumentActions();
		if (actions == null) {
			actions = docConfig.createDocumentActions(doc);
			doc.setDocumentActions(actions);
		}

		File docFolder = destination;
		LogHelper.info("Saving " + doc.getId() + " to " + docFolder.getPath());
		docFolder.mkdirs();
		File documentObject = new File(docFolder, "Document.xml");
		FileUtils.writeStringToFile(documentObject, doc.toString());

		// File actionsFile = new File(docFolder, "actions.xml");

		if (doc.getMainDatasource() != null) {
			File mainDataFile = new File(docFolder,
					(doc.getMainDatasource().getName() != null ? doc.getMainDatasource().getName()
							: normalizeXmlName(doc.getName())) + "."
							+ doc.getMainDatasource().getType().name().toLowerCase());
			FileOutputStream fos = null;
			InputStream is = null;

			try {
				fos = new FileOutputStream(mainDataFile);
				is = doc.getMainDatasource().getDataInputStreamToClose();
				IOUtils.copy(is, fos);

			} catch (IOException e) {
				LogHelper.error("Could not copy '" + doc.getMainDatasource().getName() + "' to '" + mainDataFile
						+ "' : " + e.getMessage());
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(fos);

			}
		}
		for (Datasource ds : doc.getAdditionalDatasources().values()) {
			File dataFile = new File(docFolder, ds.getName() + "." + ds.getType().name().toLowerCase());
			FileOutputStream fos = null;
			InputStream is = null;
			try {

				fos = new FileOutputStream(dataFile);
				is = ds.getDataInputStreamToClose();//.getDataInputStreamToClose();
				IOUtils.copy(is, fos);

			} catch (IOException e) {
				LogHelper.error("Could not copy '" + ds.getName() + "' to '" + dataFile + "' : " + e.getMessage());
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(fos);

			}
		}

		Map<String, Object> docContext = FreeMarkerHelper.getContextFromDocument(doc);
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		org.w3c.dom.Document xmlDoc = docBuilder.newDocument();
		Element root = xmlDoc.createElement("context");
		xmlDoc.appendChild(root);
		Element currentLevel = root;
		for (Entry<String, Object> entry : docContext.entrySet()) {
			appendContext(xmlDoc, currentLevel, entry);
		}
		Utils.saveXML(xmlDoc, new File(docFolder, "context.xml"));
	}

	public static boolean isFileInProgess(File f) {
		long firstSize = f.length();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return firstSize != f.length();
	}

	private static final String datePattern = "yyyy-MM-dd";
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
	private static final String timePattern = "HH-mm-ss";
	private static final SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);

	public static String formatDate(Date date) {
		return simpleDateFormat.format(date);
	}

	public static String formatTime(Date date) {
		return simpleTimeFormat.format(date);
	}

}
