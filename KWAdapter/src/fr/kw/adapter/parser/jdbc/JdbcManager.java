package fr.kw.adapter.parser.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.Record;
import fr.utils.LogHelper;

public class JdbcManager {

	private static Map<String, Connection> connections = new HashMap<String, Connection>();
	private JdbcSettings settings;

	public JdbcManager(JdbcSettings settings) {
		this.settings = settings;
	}

	public void runQuery(Record record, JdbcQuery query, Map<String, Object> initialContext)
			throws fr.kw.adapter.parser.jdbc.JdbcException {

		JdbcDataSource ds = settings.datasources.get(query.dataSourceId);
		Connection connection = null;
		try {

			connection = connections.get(query.name + "_" + query.dataSourceId);
			if (connection == null) {
				JdbcManager.loadDrivers(ds.driver);
				connection = DriverManager.getConnection(ds.url, ds.user, ds.password);
				connection.setAutoCommit(false);
				connections.put(query.name + "_" + query.dataSourceId, connection);
			}

			HashMap<String, Object> context = new HashMap<String, Object>();
			context.putAll(initialContext);

			Record root = record.getRootParent();

			context.put(root.getName(), getDataModel(root, true));

			Map<String, Object> currentDataModel = getDataModel(record, false);
			context.put(record.getName(), currentDataModel);
			context.put("current", currentDataModel);
			context.put("record", currentDataModel);

			Record currentRecord = record;
			while (currentRecord.getParent() != null) {
				Record parent = currentRecord.getParent();

				Map<String, Object> dataModel = getDataModel(currentRecord, false);
				currentDataModel.put("parent", dataModel);
				dataModel.put(currentRecord.getName(), currentDataModel);
				context.put(parent.getName(), dataModel);

				currentDataModel = dataModel;
				currentRecord = parent;
			}

			String querySelect = query.select;
			querySelect = FreeMarkerHelper.parseExpression(querySelect, querySelect, context);

			ResultSet resultSet = executeQuery(connection, querySelect, 100, Integer.MAX_VALUE, 30);

			// *******************************************************************************************

			int currRowNumber = 0;
			String[] columnsName = null;

			// Store the columns name
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			columnsName = new String[resultSetMetaData.getColumnCount()];
			for (int i = 0; i < columnsName.length; i++) {
				columnsName[i] = resultSetMetaData.getColumnName(i + 1);
			}
			resultSetMetaData = null;

			while (resultSet.next()) {
				currRowNumber++;
				Record r = new Record(query.name);
				r.setParent(record);

				for (int i = 0; i < columnsName.length; i++) {
					r.getFields().add(new Field(columnsName[i], resultSet.getString(i + 1)));
				}
				record.getRecords().add(r);

				// TODO : call subqueries

				for (JdbcQuery subQuery : query.children) {
					runQuery(r, subQuery, context);
				}

				String updateQuery = null;
				if (query.getUpdates(UpdateWhen.row) != null)
					for (JdbcUpdate update : query.getUpdates(UpdateWhen.row)) {
						updateQuery = update.update;
						updateQuery = FreeMarkerHelper.parseExpression(updateQuery, updateQuery, context);
						executeUpdate(connection, updateQuery, 30);
					}

			}
			resultSet.close();

			String updateQuery = null;
			if (query.getUpdates(UpdateWhen.end) != null)
				for (JdbcUpdate update : query.getUpdates(UpdateWhen.end)) {
					updateQuery = update.update;
					updateQuery = FreeMarkerHelper.parseExpression(updateQuery, updateQuery, context);
					executeUpdate(connection, updateQuery, 30);
				}

			// *******************************************************************************************

		} catch (SQLException e) {
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					connection.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			connections.remove(query.name + "_" + query.dataSourceId);
			throw new JdbcException("Could not run " + query + " : " + e.getMessage(), e);
		} finally {

		}

	}

	public static void loadDrivers(String driverName) throws JdbcException {

		try {

			Class.forName(driverName).getDeclaredConstructor().newInstance();

		} catch (IllegalAccessException e) {
			throw new JdbcException("Can't load driver '" + driverName + "', IllegalAccessException.", e);
		} catch (ClassNotFoundException e) {
			throw new JdbcException("Can't load driver '" + driverName + "', ClassNotFoundException.", e);
		} catch (InstantiationException e) {
			throw new JdbcException("Can't load driver '" + driverName + "', InstantiationException.", e);
		} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new JdbcException("Can't load driver '" + driverName + "', " + e.getClass().getSimpleName() + ".", e);
		}

	}

	public static ResultSet executeQuery(Connection connection, String query, int fetchSize, int maxRows,
			int queryTimeOut) throws JdbcException {

		Statement statement = null;
		ResultSet rs = null;
		try {
			LogHelper.debug("Executing query : " + query);
			statement = connection.createStatement();

			int initFetchSize = statement.getFetchSize();
			int initMaxRows = statement.getMaxRows();
			int initQueryTimeOut = statement.getQueryTimeout();

			if (fetchSize != 0)
				statement.setFetchSize(fetchSize);
			if (maxRows != 0)
				statement.setMaxRows(maxRows);
			if (queryTimeOut != 0)
				statement.setQueryTimeout(queryTimeOut);

			rs = statement.executeQuery(query);

			statement.setFetchSize(initFetchSize);
			statement.setMaxRows(initMaxRows);
			statement.setQueryTimeout(initQueryTimeOut);

			return rs;

		} catch (SQLException e) {
			throw new JdbcException("Can't execute query '" + query + "', SQLException : " + e.getMessage(), e);
		} finally {

		}
	}

	public static void executeUpdate(Connection connection, String update, int queryTimeOut) throws JdbcException {

		try {

			Statement statement = connection.createStatement();

			int initQueryTimeOut = statement.getQueryTimeout();

			if (queryTimeOut != 0)
				statement.setQueryTimeout(queryTimeOut);

			statement.executeUpdate(update);

			statement.setQueryTimeout(initQueryTimeOut);

		} catch (SQLException e) {
			throw new JdbcException("Can't execute update '" + update + "', SQLException.", e);
		}
	}

	public static Map<String, Object> getDataModel(Record record, boolean recursive) {
		Map<String, Object> recordDataModel = new HashMap<String, Object>();
		for (Field f : record.getFields()) {
			recordDataModel.put(f.getName(), f.getValue());
		}
		if (recursive)
			for (Record r : record.getRecords()) {

				recordDataModel.put(r.getName(), getDataModel(r, recursive));
			}
		return recordDataModel;
	}

}
