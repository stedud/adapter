package fr.kw.api.submit.xml.splitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;

import de.kwsoft.mtext.api.InsufficientPermissionsException;
import de.kwsoft.mtext.api.LoginFailedException;
import de.kwsoft.mtext.api.MTextException;
import de.kwsoft.mtext.api.databinding.DataSource;
import de.kwsoft.mtext.api.databinding.SplittingCallback;
import de.kwsoft.mtext.api.databinding.XMLDataSource;
import de.kwsoft.mtext.api.server.MTextFactory;
import de.kwsoft.mtext.api.server.MTextServer;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.api.submit.SubmitConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class XMLSplitter {

	private SubmitConfiguration configuration;
	protected int i = 0;

	public XMLSplitter(SubmitConfiguration configuration) {
		this.configuration = configuration;
	}

	protected synchronized int incrementCounter() {
		i++;
		return i;
	}

	public List<Datasource> split(Datasource xmlData, String splitter) throws IllegalArgumentException,
			LoginFailedException, InsufficientPermissionsException, MTextException, DataSourceException {

		List<Datasource> result = new ArrayList<Datasource>();

		Properties connectParams = new Properties();
		connectParams.put("kwsoft.env.mtextclient.mtext.communication.EJB.ProviderUrl",
				configuration.getUrlForJavaAPI());

		
		MTextServer mMText;
		try {
			mMText = MTextFactory.connect(configuration.getKWUser(), configuration.getKWPlainPassword(),
					connectParams);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (LoginFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (InsufficientPermissionsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (MTextException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		InputStream is = null;
		try {
			is = xmlData.getDataInputStreamToClose();
			DataSource ds = new XMLDataSource(is);
			mMText.splitDataSource(ds, splitter, new SplittingCallback() {

				@Override
				public void runWithDataSource(DataSource arg0) {

					try {
						int idx = incrementCounter();

//					File tmpData = File.createTempFile("split_", ".xml");
//					Utils.saveXML(arg0.getSource(), tmpData);
//					LogHelper.info(xmlData.getPath() + ", new split  " + tmpData.getPath());
//					result.add(tmpData);

						Datasource splitDatasource = new Datasource(DatasourceType.XML, xmlData.getName() + "_" + idx);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						Utils.saveXML(arg0.getSource(), baos);
						arg0 = null;
						splitDatasource.writeData(baos.toByteArray(), baos.size());
						baos = null;
						result.add(splitDatasource);
						LogHelper.info(xmlData.getName() + ", new split  " + splitDatasource.getName());

					} catch (IOException | TransformerException | DataSourceException e) {
						LogHelper.error("Error when saving split document : " + e.getMessage());
					}

				}
			});
		} finally {

			IOUtils.closeQuietly(is);
		}
		return result;
	}

	public SubmitConfiguration getConfiguration() {
		return configuration;
	}

}
