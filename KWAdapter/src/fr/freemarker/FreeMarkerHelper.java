package fr.freemarker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.Record;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.Values;
import freemarker.cache.StringTemplateLoader;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class FreeMarkerHelper {

	public static final String XF_FOLDER = "xf";

	private Configuration freemarkerConfig;
	private static FreeMarkerHelper instance = null;
	static private Configuration stringFreemarkerConfig;
	static StringTemplateLoader stringLoader;

	static {
		stringFreemarkerConfig = new Configuration(Configuration.VERSION_2_3_26);
		stringLoader = new StringTemplateLoader();
		stringFreemarkerConfig.setTemplateLoader(stringLoader);

	}

	public static void clearCache() {
		if (stringFreemarkerConfig != null)
			stringFreemarkerConfig.clearTemplateCache();
		if (getInstance().freemarkerConfig != null)
			getInstance().freemarkerConfig.clearTemplateCache();
	}

	private FreeMarkerHelper() {
		freemarkerConfig = new Configuration(Configuration.VERSION_2_3_26);
		try {
			File xf = new File(XF_FOLDER);
			if (!xf.exists())
				xf.mkdirs();
			freemarkerConfig.setDirectoryForTemplateLoading(xf);
		} catch (IOException e) {
			LogHelper.error(
					"Error in freemarker init, template folder '" + XF_FOLDER + "' not acessible : " + e.getMessage(),
					e);
		}
		freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freemarkerConfig.setLogTemplateExceptions(false);
		// freemarkerConfig.setWrapUncheckedExceptions(true);
		freemarkerConfig.setNumberFormat("###0.###########");
	}
	
	public static Map<String, Object> getFilteredContextFromXMLInputStream(InputStream xmlIS, String xpathPattern) throws FreeMarkerException {
		try {
			InputSource is = new InputSource(xmlIS);
			Map<String, Object> context = new HashMap<String, Object>();
			
				XPathExpression xPathExpr;
				NodeList result = null;
				try {
					
					XPath xPath = XPathFactory.newInstance().newXPath();
					xPathExpr = xPath.compile(xpathPattern);
					result = (NodeList) xPathExpr.evaluate(is, XPathConstants.NODESET);
					for (int i=0;i<result.getLength();i++)
					{
					
						Node node = result.item(i);
						//TODO : reconstruire l'arboresence du node dans le contexte
					}
					
				} catch (XPathExpressionException e) {
					LogHelper.error("Error on XML pattern '" + xpathPattern + "' : " + e.getMessage(), e);
					
				}
			
			
		
			return context;

		} catch (Exception e) {
			throw new FreeMarkerException("Could not create context from xml Input Stream", e);
		} finally {
			try {
				
				xmlIS.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static NodeModel getContextFromXMLInputStream(InputStream xmlIS) throws FreeMarkerException {
		try {
			
			InputSource is = new InputSource(xmlIS);
	
			return freemarker.ext.dom.NodeModel.parse(is);

		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new FreeMarkerException("Could not create context from xml Input Stream", e);
		} finally {
			try {
				xmlIS.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static NodeModel getContextFromXML(File xml) throws FreeMarkerException {
		try {
			return freemarker.ext.dom.NodeModel.parse(xml);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new FreeMarkerException("Could not create context from xml '" + xml.getPath() + "'", e);
		}
	}

	public static Map<String, Object> getContextFromRecord(Record record) {
		Map<String, Object> context = new HashMap<String, Object>();
		for (Field f : record.getFields()) {
			context.put(Utils.normalizeXmlName(f.getName()), f.getValue());
		}
		for (Record child : record.getRecords()) {
			Map<String, Object> childContext = getContextFromRecord(child);
			if (context.get(Utils.normalizeXmlName(child.getName())) != null) {
				String name = Utils.normalizeXmlName(child.getName());
				if (context.get(name) instanceof List) {
					((List) context.get(name)).add(childContext);
				} else if (context.get(name) instanceof Map) {
					List<Object> newContext = new ArrayList<Object>(2);
					newContext.add(context.get(name));
					newContext.add(childContext);
					context.replace(name, newContext);
				}
			}
		}
		return context;

	}

	public static Map<String, Object> getContextFromDocument(fr.kw.adapter.document.Document doc)
			throws FreeMarkerException {

		Map<String, Object> context = new HashMap<String, Object>();

		context.put("jobId", doc.getOrigin());
		context.put("origin", doc.getOrigin());
		context.put("originFileName", FilenameUtils.getName(doc.getOrigin()));
		context.put("originBaseName", FilenameUtils.getBaseName(doc.getOrigin()));
		context.put("originExtension", FilenameUtils.getExtension(doc.getOrigin()));
		context.put("docIdx", String.valueOf(doc.getNum()));
		context.put("_docNum", String.valueOf(doc.getNum()));
		context.put("environment", String.valueOf(doc.getEnvironment()));

		context.put("docId", doc.getId());

		context.put("name", doc.getName());
		context.put("docType", doc.getName());
		context.put("_docType", doc.getName());

		context.put("date", doc.getDate());
		context.put("_date", doc.getDate());
		context.put("time", doc.getTime());
		context.put("_time", doc.getTime());
		context.put("timestamp", String.valueOf(System.currentTimeMillis()));

		for (Entry<String, String> field : doc.getMetadata().entrySet()) {
			context.put(field.getKey(), field.getValue());
		}

		if (doc.getPageInfos() != null) {
			context.put("pages", doc.getPageInfos().size());
		}

		InputStream mainInputStream = null;
		List<InputStream> inputStreams = new ArrayList<InputStream>();
		try {
			mainInputStream = doc.getMainDatasource().getDataInputStreamToClose();

			if (doc.getMainDatasource().getType() == DatasourceType.XML) {
				LogHelper.debug("Main datasource context : " + doc.getMainDatasource().getName() + ", data="
						+ doc.getMainDatasource().getName());
				if (doc.getMainDatasource() != null)
					if (doc.getMainDatasource().isNotEmpty()) {
						NodeModel mainDataContext = getContextFromXMLInputStream(mainInputStream);
						context.put(doc.getMainDatasource().getName(), mainDataContext);
					}
			}
			if (doc.getMainDatasource() != null)
				if (doc.getMainDatasource().isNotEmpty())
					context.put(doc.getMainDatasource() + "Name", doc.getMainDatasource().getName());

			for (Datasource ds : doc.getAdditionalDatasources().values()) {
				LogHelper.debug("Context datasource " + ds.getName());
				if (ds.getType() == DatasourceType.XML && ds.isNotEmpty()) {
					InputStream is = ds.getDataInputStreamToClose();
					inputStreams.add(is);
					NodeModel dsContext = getContextFromXMLInputStream(is);
					context.put(ds.getName(), dsContext);
				}
				if (ds.isNotEmpty())
					context.put(ds.getName() + "Name", ds.getName());

			}

			for (Datasource ds : doc.getAdditionalContexts().values()) {
				LogHelper.debug("Context datasource " + ds.getName());
				if (ds.getType() == DatasourceType.XML && ds.isNotEmpty()) {
					InputStream is = ds.getDataInputStreamToClose();
					inputStreams.add(is);
					NodeModel dsContext = getContextFromXMLInputStream(is);
					context.put(ds.getName(), dsContext);
				}
				if (ds.isNotEmpty())
					context.put(ds.getName() + "Name", ds.getName());
			}
		} catch (DataSourceException e) {
			throw new FreeMarkerException("Could not read data source for FreeMarker context : " + e.getMessage(), e);
		} finally {

			IOUtils.closeQuietly(mainInputStream);

			for (InputStream is : inputStreams) {
				IOUtils.closeQuietly(is);
			}

		}

		return context;
	}

	public File createXmlXf(String templateName, Map<String, Object> context) {
		try {
			Template template = freemarkerConfig.getTemplate(templateName);

			File target = File.createTempFile(templateName, ".tmp");
			FileOutputStream fos = new FileOutputStream(target);
			OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

			template.process(context, osw);
			osw.flush();
			osw.close();

			return target;

		} catch (IOException | TemplateException e) {
			// TODO Auto-generated catch block
			LogHelper.error("Error while processing freemarker template " + templateName, e);
		}

		return null;

	}

	public synchronized static String parseExpression(String expressionAsString, String expressionName,
			Map<String, Object> context) {
		stringLoader.putTemplate(expressionName, expressionAsString);
		try {
			Template template = stringFreemarkerConfig.getTemplate(expressionName);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
			template.process(context, osw);
			osw.flush();
			osw.close();
			return Values.getValue(baos.toString(StandardCharsets.UTF_8.name()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogHelper.warn("Could not parse " + expressionAsString + " : " + e.getMessage());
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogHelper.warn("Could not parse " + expressionAsString + " : " + e.getMessage());
		}
		return "";

	}

	public static Configuration getStringFreemarkerConfig() {

		return stringFreemarkerConfig;
	}

	public static synchronized FreeMarkerHelper getInstance() {
		if (instance == null) {
			instance = new FreeMarkerHelper();
		}
		return instance;
	}

}
