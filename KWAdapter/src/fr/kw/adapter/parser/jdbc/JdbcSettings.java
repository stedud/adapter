package fr.kw.adapter.parser.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JdbcSettings {

	protected Map<String, JdbcDataSource> datasources = new HashMap();
	protected List<JdbcQuery> queries = new ArrayList<JdbcQuery>();

	public static JdbcSettings parse(String settingsXML) {
		/**
		 * <jdbc> <datasources> <datasource id="db1"> <driver></driver> <url></url>
		 * <user></user> <pwd></pwd> </datasource> </datasources> <queries>
		 * <query datasource="db1"> <select>select * from table1</select>
		 * <query name="q1"> <select>select * from table2 where
		 * champ2a='table1.champ1a'</select> </query> <update when="row/end">update
		 * table1 set champ1.a='XXX'</update> </query> </queries> </jdbc>
		 * 
		 */

		JdbcSettings settings = null;
		try {
			settings = new JdbcSettings();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			ByteArrayInputStream bais = new ByteArrayInputStream(settingsXML.getBytes());

			Document document = docBuilder.parse(bais);
			NodeList datasourceNodes = document.getElementsByTagName("datasource");
			int nbDatasources = datasourceNodes.getLength();
			for (int i = 0; i < nbDatasources; i++) {
				Node datasourceNode = datasourceNodes.item(i);
				parseDatasource(settings, (Element) datasourceNode);
			}

			NodeList queriesNodes = document.getElementsByTagName("queries");

			int nbQueriesNodes = queriesNodes.getLength();
			for (int i = 0; i < nbQueriesNodes; i++) {
				Node queriesNode = queriesNodes.item(i);
				parseQueries(settings, (Element) queriesNode);
			}

		} catch (FactoryConfigurationError | ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JdbcSettingsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return settings;

	}

	protected static void parseDatasource(JdbcSettings settings, Element datasourceNode) throws JdbcSettingsException {

		String id = datasourceNode.getAttribute("id");
		if (StringUtils.isBlank(id))
			throw new JdbcSettingsException("<datasource> element must indicate @id attribute");

		NodeList driverList = datasourceNode.getElementsByTagName("driver");
		if (driverList.getLength() != 1)
			throw new JdbcSettingsException("<datasource> element must contain 1 <driver> element");
		String driver = driverList.item(0).getTextContent();

		NodeList userList = datasourceNode.getElementsByTagName("user");
		if (userList.getLength() != 1)
			throw new JdbcSettingsException("<datasource> element must contain 1 <user> element");
		String user = userList.item(0).getTextContent();

		NodeList pwdList = datasourceNode.getElementsByTagName("pwd");
		if (pwdList.getLength() != 1)
			throw new JdbcSettingsException("<datasource> element must contain 1 <pwd> element");
		String pwd = pwdList.item(0).getTextContent();

		NodeList urlList = datasourceNode.getElementsByTagName("url");
		if (urlList.getLength() != 1)
			throw new JdbcSettingsException("<datasource> element must contain 1 <url> element");
		String url = urlList.item(0).getTextContent();

		JdbcDataSource ds = new JdbcDataSource();
		ds.id = id;
		ds.driver = driver;
		ds.url = url;
		ds.user = user;
		ds.password = pwd;
		if (settings.datasources.containsKey(ds.id))
			throw new JdbcSettingsException("<datasource> @id attribute must be unique");

		settings.datasources.put(ds.id, ds);

	}

	protected static void parseQueries(JdbcSettings settings, Element queriesNode) throws JdbcSettingsException {
		NodeList queryNodes = queriesNode.getChildNodes();

		int nbQueries = queryNodes.getLength();
		for (int i = 0; i < nbQueries; i++) {
			Node currNode = queryNodes.item(i);
			if (currNode.getNodeType() == Node.ELEMENT_NODE) {
				Element queryNode = (Element) currNode;
				if (queryNode.getNodeName().compareTo("query") != 0)
					throw new JdbcSettingsException("Only <query> elements are expected in <queries>");
				parseQuery(settings, null, queryNode);
			}

		}

	}

	protected static void parseQuery(JdbcSettings settings, JdbcQuery parentQuery, Element queryNode)
			throws JdbcSettingsException {

		String name = queryNode.getAttribute("name");

		List<Element> selectList = getChildren(queryNode, "select");

		if (selectList.size() != 1)
			throw new JdbcSettingsException("Element <query> '" + name + "' must contain 1 <select> element");

		String dsId = queryNode.getAttribute("datasource");
		if (StringUtils.isBlank(dsId))
			throw new JdbcSettingsException("<query> element must have a 'datasource' attribute");
		if (!settings.datasources.containsKey(dsId))
			throw new JdbcSettingsException("datasource '" + dsId + "' was not found");

		JdbcQuery jdbcQuery = new JdbcQuery();
		jdbcQuery.dataSourceId = dsId;
		if (StringUtils.isNotBlank(name))
			jdbcQuery.name = name;

		for (Element selectElt : selectList) {

			String select = selectElt.getTextContent();

			jdbcQuery.select = select;

			List<Element> updateList = getChildren(queryNode, "update");

			int nbUpdate = updateList.size();
			for (int i = 0; i < nbUpdate; i++) {
				Element update = (Element) updateList.get(i);
				String updateQuery = update.getTextContent();
				String whenStr = update.getAttribute("when");
				UpdateWhen when = UpdateWhen.valueOf(whenStr);
				JdbcUpdate jdbcUpdate = new JdbcUpdate();
				jdbcUpdate.update = updateQuery;
				jdbcUpdate.when = when;
				jdbcQuery.addUpdate(jdbcUpdate);
			}

			List<Element> childrenQueries = getChildren(queryNode, "query");
			int nbChildren = childrenQueries.size();
			for (int i = 0; i < nbChildren; i++) {
				parseQuery(settings, jdbcQuery, (Element) childrenQueries.get(i));
			}

			if (parentQuery != null) {
				parentQuery.children.add(jdbcQuery);
			} else {
				settings.queries.add(jdbcQuery);
			}
		}
	}

	public static List<Element> getChildren(Node element, String elementName) {
		NodeList children = element.getChildNodes();
		List<Element> selectList = new ArrayList<Element>(children.getLength());
		for (int i = 0; i < children.getLength(); i++) {
			Node current = children.item(i);
			if (current.getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) current;
				if (elt.getNodeName().compareTo(elementName) == 0) {
					selectList.add(elt);
				}
			}
		}
		return selectList;
	}

	public Map<String, JdbcDataSource> getDatasources() {
		return datasources;
	}

	public List<JdbcQuery> getQueries() {
		return queries;
	}

}
