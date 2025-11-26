package fr.kw.adapter.document;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import fr.utils.Utils;

public class Datasource {

	/**
	 * For Tonic documents : The name of the datasource as indicated in the Tonic
	 * template data configuration. Can be prefixed with the data type
	 * (xml:,csv:,json:) By default, "xml:" is assumed. ex: xml:DATA
	 * 
	 * For MOMS documents (converted with InputX module) : the data will contain a
	 * MFD, in this case the name is ignored.
	 */
	protected String name;

	/**
	 * The file containig the data (xml or json or csv, or MFD if the document has
	 * to be sent to MOMS directly)
	 * 
	 */

	protected File storage;

	protected boolean onDisk = false;

	protected boolean empty = true;

	protected static int MAX_MEMORY_SIZE = 5 * 1024 * 1024;
	protected ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
	/**
	 * Default value is {@link DatasourceType}.XML
	 */
	protected DatasourceType type = DatasourceType.XML;// par défaut

	public Datasource(DatasourceType type) {
		this.type = type;
	}

	public Datasource(DatasourceType type, File data) throws DataSourceException {

		this(type, null, data);
	}

	public Datasource(DatasourceType type, String name, File data) throws DataSourceException {
		super();
		this.type = type;
		this.name = name;
		if (data.length() > MAX_MEMORY_SIZE) {
			onDisk = true;
			storage = data;
			empty = false;

		} else {
			this.writeData(data);
		}

	}

	public Datasource(DatasourceType type, String name) {
		super();
		this.type = type;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DatasourceType getType() {
		return type;
	}

	public void setType(DatasourceType type) {
		this.type = type;
	}

	public void clean() {

		if (onDisk) {
			if (storage != null)
				storage.delete();
			storage = null;
		} else {
			dataBytes = null;
		}
		empty = true;
		onDisk = false;

	}

	public InputStream getDataInputStreamToClose() throws DataSourceException {
		if (onDisk) {
			try {
				return new FileInputStream(storage);
			} catch (FileNotFoundException e) {
				throw new DataSourceException("Could not read data storage on disk : " + e.getMessage(), e);
			}
		} else {
			return new ByteArrayInputStream(dataBytes.toByteArray());
		}
	}

	public synchronized void writeData(String str) throws DataSourceException {
		byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
		writeData(strBytes, strBytes.length);
	}

	public synchronized void writeData(File file) throws DataSourceException {
		BufferedInputStream bis = null;
		try {
			if (file.length() > MAX_MEMORY_SIZE) {
				onDisk = true;
				storage = file;
				empty = false;
			} else {
				bis = new BufferedInputStream(new FileInputStream(file));
				byte[] buffer = new byte[MAX_MEMORY_SIZE];
				int len = 0;
				while ((len = bis.read(buffer)) >= 0) {
					writeData(buffer, len);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (bis != null)
				try {
					bis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	public synchronized void writeData(byte[] bytes, int length) throws DataSourceException {
		if (!onDisk) {
			if (bytes.length + dataBytes.size() > MAX_MEMORY_SIZE || Utils.getPercentMemoryUse() > 90) {

				onDisk = true;
				try {
					storage = File.createTempFile("datasource", ".tmp");
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storage, true));
					bos.write(dataBytes.toByteArray());
					bos.flush();
					bos.write(bytes, 0, length);
					bos.flush();
					bos.close();
					dataBytes = null;
				} catch (IOException e) {
					throw new DataSourceException("Could not add data to file data source : " + e.getMessage(), e);
				}
			} else {

				if (dataBytes == null)
					dataBytes = new ByteArrayOutputStream();
				dataBytes.write(bytes, 0, length);

			}

		} else if (onDisk) {
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storage, true));

				bos.write(bytes, 0, length);
				bos.flush();
				bos.close();
			} catch (IOException e) {
				throw new DataSourceException("Could not add data to file data source : " + e.getMessage(), e);
			}
		}

		if (length > 0)
			empty = false;

	}

	public boolean isEmpty() {
		return empty;
	}

	public boolean isNotEmpty() {
		return !empty;
	}

	public boolean isSameDataFile(File data) {
		if (onDisk) {
			return storage.compareTo(data) == 0;
		}
		return false;
	}

}
