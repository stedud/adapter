package fr.kw.adapter.parser.type.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.IParserConfiguration;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.utils.configuration.ConfigurationException;
import fr.utils.configuration.FileConfiguration;

public class XmlParserConfiguration implements IParserConfiguration {

	private String id = "";
	protected ParseProcessConfiguration rootConfiguration;

	private Map<String, String> metadata = new HashMap<String, String>();
	String charset = null;
	/**
	 * Contains the patterns used to identify events. map key = event name map value
	 * = possible patterns (xpath syntax)
	 */
	private LinkedHashMap<String, String[]> patterns = new LinkedHashMap<String, String[]>();
	/**
	 * Contains the patterns that will decide the split of the original XML.
	 * Patterns follow this syntax : .
	 */
	private List<XmlPattern> splitters = new ArrayList<XmlPattern>();

	public XmlParserConfiguration() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ParseProcessConfiguration getRootConfiguration() {

		return rootConfiguration;
	}

	@Override
	public void load(File descriptionFile, ParseProcessConfiguration rootConfig) throws IOException {
		/***
		 * XMLIN=xmlin MODE=SAX Invoice=\\Invoice;\\Facture
		 * SPLITTERS=\\newDocument;\\document METADATA=jobId;inputFile,...
		 * 
		 */
		try {
			this.rootConfiguration = rootConfig;
			FileConfiguration config = FileConfiguration.get(descriptionFile);
			this.id = config.get("XMLIN");
			this.charset = config.get("ENCODING", StandardCharsets.UTF_8.name());

			List<String> metadataList = config.getList("METADATA");
			for (String key : metadataList) {
				this.metadata.put(key, String.valueOf(key));
			}

			List<String> splittersStr = config.getList("SPLITTERS");
			for (String splitterObj : splittersStr) {
				if (StringUtils.isBlank(splitterObj))
					continue;

				
				XmlPattern xmlPattern = new XmlPattern();
				this.splitters.add(xmlPattern);
				String splitterStr = String.valueOf(splitterObj);
				String[] tokens = StringUtils.split(splitterStr, ',');
				for (String pattern : tokens) {
					String[] args = StringUtils.splitByWholeSeparator(pattern, "->");
					switch (args.length) {
					case 1:
						if (pattern.startsWith("@")) {
							Pattern patternObj = new Pattern(StringUtils.removeStart(args[0], "@"));
							xmlPattern.setPatternOnAttribute(patternObj);
						} else {
							Pattern patternObj = new Pattern(args[0]);
							xmlPattern.setPatternOnNode(patternObj);
						}
						break;

					case 2:
						if (pattern.startsWith("@")) {
							Pattern patternObj = new Pattern(StringUtils.removeStart(args[0], "@"), args[1]);
							xmlPattern.setPatternOnAttribute(patternObj);
						} else {
							Pattern patternObj = new Pattern(args[0], args[1]);
							xmlPattern.setPatternOnNode(patternObj);
						}
						break;
					default:

					}

				}
			}

			
			FileReader fr = new FileReader(descriptionFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				if (StringUtils.isBlank(line))
					continue;
				if (StringUtils.startsWith(line, "//"))
					continue;

				if (StringUtils.startsWith(line, "#"))
					continue;
				if (! StringUtils.contains(line, "="))
					continue;
				if (StringUtils.startsWith(line.trim().toLowerCase(), "xmlin="))
					continue;
				
				
				
				String key = StringUtils.substringBefore(line, "=");
				String patterns = StringUtils.substringAfter(line, "=");
				String[] patternsArr = StringUtils.split(patterns, ';');
				this.patterns.put(key, patternsArr);
			}
			IOUtils.closeQuietly(br);
			IOUtils.closeQuietly(fr);//devrait être inutile...
			
			
//			Iterator<String> iterator = config.getKeys().iterator();//Ne respecte pas l'ordre du fichier : KO
//			while (iterator.hasNext()) {
//				String key = iterator.next();
//				if (!StringUtils.containsAny(key, "XMLIN", "SPLITTERS")) {
//					List<String> docPatterns = config.getList(key);
//					String[] docPatternsArr = new String[docPatterns.size()];
//					for (int i = 0; i < docPatterns.size(); i++) {
//						docPatternsArr[i] = docPatterns.get(i);
//					}
//					this.patterns.put(key, docPatternsArr);
//				}
//			}

		} catch (ConfigurationException e) {
			throw new IOException(e);
		}

	}

	@Override
	public String getId() {

		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;

	}

	public LinkedHashMap<String, String[]> getPatterns() {
		return patterns;
	}

	public List<XmlPattern> getSplitters() {
		return splitters;
	}

	public String getCharset() {
		return this.charset;
	}

	public Map<String, String> getMetadata() {
		return metadata;

	}

}
