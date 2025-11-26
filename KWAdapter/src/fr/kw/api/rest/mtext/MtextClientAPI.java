package fr.kw.api.rest.mtext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.json.JSONArray;

import fr.utils.LogHelper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MtextClientAPI {

	protected String[] urlBases;
	protected URL[] urlBasesRest;
	protected Random random;

	public MtextClientAPI(String[] urlBases) throws MalformedURLException {

		if (urlBases == null || urlBases.length == 0)
			throw new MalformedURLException("No URL defined");
		this.urlBases = urlBases;

		this.urlBasesRest = new URL[urlBases.length];
		for (int i = 0; i < this.urlBasesRest.length; i++) {
			this.urlBasesRest[i] = new URL(this.urlBases[i] + "/mtext-integration-adapter");

		}
		random = new Random();
	}

	public synchronized URL getUrlBase() throws IOException {

		int i = random.nextInt(this.urlBasesRest.length - 1 + 1);
		URL url = this.urlBasesRest[i];
		List<Integer> tested = new ArrayList<Integer>(this.urlBasesRest.length);

		while (!pingURL(url.toString(), 1000 * 60 * 2)) {
			tested.add(i);
			if (tested.size() == this.urlBasesRest.length)
				throw new IOException("No response from server(s)");
			do {
				i = random.nextInt(this.urlBasesRest.length - 1 + 1);
			} while (tested.contains(i));
			url = this.urlBasesRest[i];
		}

		return url;

	}

	/**
	 * Pings a HTTP URL. This effectively sends a HEAD request and returns
	 * <code>true</code> if the response code is in the 200-399 range.
	 * 
	 * @param url     The HTTP URL to be pinged.
	 * @param timeout The timeout in millis for both the connection timeout and the
	 *                response read timeout. Note that the total timeout is
	 *                effectively two times the given timeout.
	 * @return <code>true</code> if the given HTTP URL has returned response code
	 *         200-399 on a HEAD request within the given timeout, otherwise
	 *         <code>false</code>.
	 */
	public static boolean pingURL(String url, int timeout) {
		// url = url.replaceFirst("^https", "http"); // Otherwise an exception may be
		// thrown on invalid SSL certificates.

		try {
			LogHelper.debug("Ping url " + url);
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			LogHelper.debug("response ping url " + url + " : " + responseCode);
			return (200 <= responseCode && responseCode <= 404);
		} catch (IOException exception) {
			return false;
		}
	}

	public OkHttpClient getHttpClient() {
		// Add here certificate manamgenet if needed
		okhttp3.OkHttpClient.Builder builder = new OkHttpClient().newBuilder().readTimeout(10, TimeUnit.MINUTES)
				.writeTimeout(10, TimeUnit.MINUTES).connectTimeout(2, TimeUnit.MINUTES);
		builder.setHostnameVerifier$okhttp(new HostnameVerifier() {

			@Override
			public boolean verify(String hostname, SSLSession session) {

				return session.getPeerHost().toLowerCase().contains(hostname.toLowerCase());
			}
		});

		OkHttpClient client = builder.build();

		return client;
	}

	public fr.kw.api.rest.mtext.Response documentDelete(String documentPath, JobExecutionStrategy strategy,
			Boolean ignoreLock, String user, String password, String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		MediaType mediaType = MediaType.parse("text/plain");
		// RequestBody body = RequestBody..create(mediaType, "");
		RequestBody body = RequestBody.create("".getBytes(), mediaType);
		Request request = new Request.Builder().url(getUrlBase() + "/document/" + documentPath + "/delete?" + "user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: "")
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (ignoreLock != null ? "&mtext.deleteTextDocumentConfiguration.ignoreLock=" + ignoreLock : ""))
				.method("DELETE", body).build();
		LogHelper.debug("Query delete document " + documentPath);
		Response response = client.newCall(request).execute();
		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response documentPrint(String documentPath, String printer,
			Boolean executeDocumentModels, Boolean saveAfterOperation, JobExecutionStrategy strategy,
			Integer documentVersion, String user, String password, String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		Request request = new Request.Builder().url(getUrlBase() + "/document/" + documentPath + "/print?"
				+ "destination=" + (printer != null ? printer : "OMS")
				+ (executeDocumentModels != null ? "&execute-document-models=" + executeDocumentModels : "")
				+ (saveAfterOperation != null ? "&save-after-operation=" + saveAfterOperation : "")
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "") + "&user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("GET", null).build();

		LogHelper.debug("Query document print " + documentPath + " to " + printer);
		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response documentExport(String documentPath, String mimeType,
			JobExecutionStrategy strategy, String documentPassword, String styleSheet, String user, String password,
			String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		Request request = new Request.Builder().url(getUrlBase() + "/document/" + documentPath + "/export?"
				+ "mime-type=" + (mimeType != null ? mimeType : "application/pdf") + "&download=true&stream=false"
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (documentPassword != null
						? "&mtext.openTextDocumentConfiguration.DocumentPassword="
								+ URLEncoder.encode(documentPassword, StandardCharsets.UTF_8.name())
						: "")
				+ (styleSheet != null ? "&mtext.exportDocumentConfiguration.stylesheet=" + styleSheet : "") + "&user="
				+ user + (password != null ? "&password=" + password : "")
				+ (passwordPlain != null ? "&passwordplain=" + passwordPlain : "")).method("GET", null).build();
		LogHelper.debug("Query document export " + documentPath + " to " + mimeType);
		
		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(bodyResponse);
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response documentEmbedData(String documentPath, Map<String, String> data,
			JobExecutionStrategy strategy, String documentPassword, Boolean transferContent, String user,
			String password, String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		MediaType mediaType = MediaType.parse("multipart/form-data");

		Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

		if (data != null) {
			for (Entry<String, String> entry : data.entrySet()) {
				String key = entry.getKey();
				bodyBuilder = bodyBuilder.addFormDataPart(key, entry.getValue());
			}
		}
		RequestBody body = bodyBuilder.build();

		Request request = new Request.Builder()
				.url(getUrlBase() + "/document/" + documentPath + "/embedded-objects?"
						+ "mtext.openTextDocumentConfiguration.transferContent="
						+ (transferContent != null ? transferContent : false)
						+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
						+ (documentPassword != null
								? "&mtext.openTextDocumentConfiguration.DocumentPassword="
										+ URLEncoder.encode(documentPassword, StandardCharsets.UTF_8.name())
								: "")
						+ "&user=" + user + (password != null ? "&password=" + password : "")
						+ (passwordPlain != null ? "&passwordplain=" + passwordPlain : ""))
				.method("POST", body).addHeader("Content-Type", "multipart/form-data").build();

		Response response = client.newCall(request).execute();
		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;

	}

	public fr.kw.api.rest.mtext.Response documentListEmbedData(String documentPath, JobExecutionStrategy strategy,
			Boolean transferContent, String documentPassword, String user, String password, String passwordPlain)
			throws IOException {
		OkHttpClient client = getHttpClient();

		Request request = new Request.Builder().url(getUrlBase() + "/document/" + documentPath + "/embedded-objects?"
				+ "mtext.openTextDocumentConfiguration.transferContent="
				+ (transferContent != null ? transferContent : false)
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (documentPassword != null
						? "&mtext.openTextDocumentConfiguration.DocumentPassword="
								+ URLEncoder.encode(documentPassword, StandardCharsets.UTF_8.name())
						: "")
				+ "&user=" + URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("GET", null).build();
		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);

		String strResponse = new String(bodyResponse);
		if (strResponse.startsWith("[")) {
			JSONArray jsonArray = new JSONArray(strResponse);
			String[] array = new String[jsonArray.length()];
			for (int i = 0; i < array.length; i++) {
				array[i] = jsonArray.getString(i);
			}
			myResponse.setResponse(array);
		} else {
			myResponse.setResponse(new String[] {});
			myResponse.setSuccess(false);
			myResponse.setMessage("Failed to list embeded objets in document. " + response.message());
		}
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response templateCreateDocument(String template, Map<String, String> dataBindings,
			String documentPath, Boolean createMissingFolders, JobExecutionStrategy strategy,
			String holdLockOnSessionEnd, Boolean executeDocumentModels,
			Map<String, String> documentDataBindingParameters, Map<String, String> documentMetaDataParameters,
			Map<String, String> csvDataSourceNameSeparator, Map<String, String> csvDataSourceNameEncoding,
			Map<String, String> csvDataSourceNameColumns, String user, String password, String passwordPlain)
			throws IOException {
		OkHttpClient client = getHttpClient();

		MediaType mediaType = MediaType.parse("multipart/form-data");

		Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

		if (dataBindings != null) {
			for (Entry<String, String> entry : dataBindings.entrySet()) {
				String key = entry.getKey();
				if (!(key.startsWith("xml:") || key.startsWith("json:") || key.startsWith("csv:")))
					key = "xml:" + key;// xml par défaut
				bodyBuilder = bodyBuilder.addFormDataPart(key, entry.getValue());
			}
		}
		RequestBody body = bodyBuilder.build();

		StringBuffer urlDocumentDataBindingParameters = new StringBuffer();
		;
		if (documentDataBindingParameters != null && documentDataBindingParameters.size() > 0) {
			urlDocumentDataBindingParameters.append("&");
			for (Entry<String, String> entry : documentDataBindingParameters.entrySet()) {
				String key = "mtext.documentDataBindingParameter." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentDataBindingParameters.length() > 1)
					urlDocumentDataBindingParameters.append("&");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentDataBindingParameters.append("=");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlDocumentMetaDataParameters = new StringBuffer();
		if (documentMetaDataParameters != null && documentMetaDataParameters.size() > 0) {
			urlDocumentMetaDataParameters.append("&");
			for (Entry<String, String> entry : documentMetaDataParameters.entrySet()) {
				String key = "mtext.documentMetaDataParameter.Metadata." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentMetaDataParameters.length() > 1)
					urlDocumentMetaDataParameters.append("&");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentMetaDataParameters.append("=");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameSeparator = new StringBuffer();

		if (csvDataSourceNameSeparator != null && csvDataSourceNameSeparator.size() > 0) {
			urlCsvDataSourceNameSeparator.append("&");
			for (Entry<String, String> entry : csvDataSourceNameSeparator.entrySet()) {
				String key = "csv." + entry.getKey() + ".separator";
				String value = entry.getValue();
				if (urlCsvDataSourceNameSeparator.length() > 1)
					urlCsvDataSourceNameSeparator.append("&");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameSeparator.append("=");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameEncoding = new StringBuffer();
		if (csvDataSourceNameEncoding != null && csvDataSourceNameEncoding.size() > 0) {
			urlCsvDataSourceNameEncoding.append("&");
			for (Entry<String, String> entry : csvDataSourceNameEncoding.entrySet()) {
				String key = "csv." + entry.getKey() + ".encoding";
				String value = entry.getValue();
				if (urlCsvDataSourceNameEncoding.length() > 1)
					urlCsvDataSourceNameEncoding.append("&");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameEncoding.append("=");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameColumns = new StringBuffer();
		;
		if (csvDataSourceNameColumns != null && csvDataSourceNameColumns.size() > 0) {
			urlCsvDataSourceNameColumns.append("&");
			for (Entry<String, String> entry : csvDataSourceNameColumns.entrySet()) {
				String key = "csv." + entry.getKey() + ".columnNames";
				String value = entry.getValue();
				if (urlCsvDataSourceNameColumns.length() > 1)
					urlCsvDataSourceNameColumns.append("&");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameColumns.append("=");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}
		LogHelper.debug("Query template create document " + template + " to " + documentPath);
		
		Request request = new Request.Builder().url(getUrlBase() + "/template/" + template + "/create?document-name="
				+ documentPath
				+ (createMissingFolders != null ? "&create-missing-folders=f" + createMissingFolders : "")
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (holdLockOnSessionEnd != null
						? "&mtext.createTextDocumentConfiguration.holdLockOnSessionEnd=" + holdLockOnSessionEnd
						: "")
				+ (executeDocumentModels != null ? "&execute-document-models=" + executeDocumentModels : "")
				+ urlDocumentDataBindingParameters.toString() + urlDocumentMetaDataParameters.toString()
				+ urlCsvDataSourceNameSeparator.toString() + urlCsvDataSourceNameEncoding.toString()
				+ urlCsvDataSourceNameColumns.toString() + "&user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", body).addHeader("Content-Type", "multipart/form-data").build();
		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response templateExport(String template, Map<String, String> dataBindings,
			String documentPath, String mimeType, JobExecutionStrategy strategy, String holdLockOnSessionEnd,
			Boolean executeDocumentModels, Map<String, String> documentDataBindingParameters,
			Map<String, String> documentMetaDataParameters, Map<String, String> csvDataSourceNameSeparator,
			Map<String, String> csvDataSourceNameEncoding, Map<String, String> csvDataSourceNameColumns, String user,
			String password, String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		MediaType mediaType = MediaType.parse("multipart/form-data");

		Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

		if (dataBindings != null) {
			for (Entry<String, String> entry : dataBindings.entrySet()) {
				String key = entry.getKey();
				if (!(key.startsWith("xml:") || key.startsWith("json:") || key.startsWith("csv:")))
					key = "xml:" + key;// xml par défaut
				bodyBuilder = bodyBuilder.addFormDataPart(key, entry.getValue());
			}
		}
		RequestBody body = bodyBuilder.build();

		StringBuffer urlDocumentDataBindingParameters = new StringBuffer();
		;
		if (documentDataBindingParameters != null && documentDataBindingParameters.size() > 0) {
			urlDocumentDataBindingParameters.append("&");
			for (Entry<String, String> entry : documentDataBindingParameters.entrySet()) {
				String key = "mtext.documentDataBindingParameter." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentDataBindingParameters.length() > 1)
					urlDocumentDataBindingParameters.append("&");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentDataBindingParameters.append("=");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlDocumentMetaDataParameters = new StringBuffer();
		if (documentMetaDataParameters != null && documentMetaDataParameters.size() > 0) {
			urlDocumentMetaDataParameters.append("&");
			for (Entry<String, String> entry : documentMetaDataParameters.entrySet()) {
				String key = "mtext.documentMetaDataParameter.Metadata." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentMetaDataParameters.length() > 1)
					urlDocumentMetaDataParameters.append("&");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentMetaDataParameters.append("=");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameSeparator = new StringBuffer();

		if (csvDataSourceNameSeparator != null && csvDataSourceNameSeparator.size() > 0) {
			urlCsvDataSourceNameSeparator.append("&");
			for (Entry<String, String> entry : csvDataSourceNameSeparator.entrySet()) {
				String key = "csv." + entry.getKey() + ".separator";
				String value = entry.getValue();
				if (urlCsvDataSourceNameSeparator.length() > 1)
					urlCsvDataSourceNameSeparator.append("&");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameSeparator.append("=");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameEncoding = new StringBuffer();
		if (csvDataSourceNameEncoding != null && csvDataSourceNameEncoding.size() > 0) {
			urlCsvDataSourceNameEncoding.append("&");
			for (Entry<String, String> entry : csvDataSourceNameEncoding.entrySet()) {
				String key = "csv." + entry.getKey() + ".encoding";
				String value = entry.getValue();
				if (urlCsvDataSourceNameEncoding.length() > 1)
					urlCsvDataSourceNameEncoding.append("&");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameEncoding.append("=");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameColumns = new StringBuffer();
		;
		if (csvDataSourceNameColumns != null && csvDataSourceNameColumns.size() > 0) {
			urlCsvDataSourceNameColumns.append("&");
			for (Entry<String, String> entry : csvDataSourceNameColumns.entrySet()) {
				String key = "csv." + entry.getKey() + ".columnNames";
				String value = entry.getValue();
				if (urlCsvDataSourceNameColumns.length() > 1)
					urlCsvDataSourceNameColumns.append("&");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameColumns.append("=");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}
		LogHelper.debug("Query template export document " + template + " to " + mimeType);
		Request request = new Request.Builder().url(getUrlBase() + "/template/" + template + "/export?document-name="
				+ documentPath + "&mime-type=" + (mimeType != null ? mimeType : "application/pdf")
				+ "&download=true&stream=false"
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (holdLockOnSessionEnd != null
						? "&mtext.createTextDocumentConfiguration.holdLockOnSessionEnd=" + holdLockOnSessionEnd
						: "")
				+ (executeDocumentModels != null ? "&execute-document-models=" + executeDocumentModels : "")
				+ urlDocumentDataBindingParameters.toString() + urlDocumentMetaDataParameters.toString()
				+ urlCsvDataSourceNameSeparator.toString() + urlCsvDataSourceNameEncoding.toString()
				+ urlCsvDataSourceNameColumns.toString() + "&user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", body).addHeader("Content-Type", "multipart/form-data").build();
		Response response = client.newCall(request).execute();
		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(bodyResponse);
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response templatePrint(String template, Map<String, String> dataBindings,
			String documentPath, String destination, JobExecutionStrategy strategy, String holdLockOnSessionEnd,
			Boolean executeDocumentModels, Map<String, String> documentDataBindingParameters,
			Map<String, String> documentMetaDataParameters, Map<String, String> csvDataSourceNameSeparator,
			Map<String, String> csvDataSourceNameEncoding, Map<String, String> csvDataSourceNameColumns, String user,
			String password, String passwordPlain) throws IOException {
		OkHttpClient client = getHttpClient();

		MediaType mediaType = MediaType.parse("multipart/form-data");

		Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

		if (dataBindings != null) {
			for (Entry<String, String> entry : dataBindings.entrySet()) {
				String key = entry.getKey();
				if (!(key.startsWith("xml:") || key.startsWith("json:") || key.startsWith("csv:")))
					key = "xml:" + key;// xml par défaut
				bodyBuilder = bodyBuilder.addFormDataPart(key, entry.getValue());
			}
		}
		RequestBody body = bodyBuilder.build();

		StringBuffer urlDocumentDataBindingParameters = new StringBuffer();
		;
		if (documentDataBindingParameters != null && documentDataBindingParameters.size() > 0) {
			urlDocumentDataBindingParameters.append("&");
			for (Entry<String, String> entry : documentDataBindingParameters.entrySet()) {
				String key = "mtext.documentDataBindingParameter." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentDataBindingParameters.length() > 1)
					urlDocumentDataBindingParameters.append("&");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentDataBindingParameters.append("=");
				urlDocumentDataBindingParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlDocumentMetaDataParameters = new StringBuffer();
		if (documentMetaDataParameters != null && documentMetaDataParameters.size() > 0) {
			urlDocumentMetaDataParameters.append("&");
			for (Entry<String, String> entry : documentMetaDataParameters.entrySet()) {
				String key = "mtext.documentMetaDataParameter.Metadata." + entry.getKey();
				String value = entry.getValue();
				if (urlDocumentMetaDataParameters.length() > 1)
					urlDocumentMetaDataParameters.append("&");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlDocumentMetaDataParameters.append("=");
				urlDocumentMetaDataParameters.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameSeparator = new StringBuffer();

		if (csvDataSourceNameSeparator != null && csvDataSourceNameSeparator.size() > 0) {
			urlCsvDataSourceNameSeparator.append("&");
			for (Entry<String, String> entry : csvDataSourceNameSeparator.entrySet()) {
				String key = "csv." + entry.getKey() + ".separator";
				String value = entry.getValue();
				if (urlCsvDataSourceNameSeparator.length() > 1)
					urlCsvDataSourceNameSeparator.append("&");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameSeparator.append("=");
				urlCsvDataSourceNameSeparator.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameEncoding = new StringBuffer();
		if (csvDataSourceNameEncoding != null && csvDataSourceNameEncoding.size() > 0) {
			urlCsvDataSourceNameEncoding.append("&");
			for (Entry<String, String> entry : csvDataSourceNameEncoding.entrySet()) {
				String key = "csv." + entry.getKey() + ".encoding";
				String value = entry.getValue();
				if (urlCsvDataSourceNameEncoding.length() > 1)
					urlCsvDataSourceNameEncoding.append("&");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameEncoding.append("=");
				urlCsvDataSourceNameEncoding.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}

		StringBuffer urlCsvDataSourceNameColumns = new StringBuffer();
		;
		if (csvDataSourceNameColumns != null && csvDataSourceNameColumns.size() > 0) {
			urlCsvDataSourceNameColumns.append("&");
			for (Entry<String, String> entry : csvDataSourceNameColumns.entrySet()) {
				String key = "csv." + entry.getKey() + ".columnNames";
				String value = entry.getValue();
				if (urlCsvDataSourceNameColumns.length() > 1)
					urlCsvDataSourceNameColumns.append("&");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
				urlCsvDataSourceNameColumns.append("=");
				urlCsvDataSourceNameColumns.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			}
		}
		LogHelper.debug("Query template export document " + template + " to " + (destination != null ? destination : "OMS"));
		Request request = new Request.Builder().url(getUrlBase() + "/template/" + template + "/print?document-name="
				+ documentPath + "&destination=" + (destination != null ? destination : "OMS")
				+ (strategy != null ? "&mtext.jobConfiguration.executionStrategy=" + strategy : "")
				+ (holdLockOnSessionEnd != null
						? "&mtext.createTextDocumentConfiguration.holdLockOnSessionEnd=" + holdLockOnSessionEnd
						: "")
				+ (executeDocumentModels != null ? "&execute-document-models=" + executeDocumentModels : "")
				+ urlDocumentDataBindingParameters.toString() + urlDocumentMetaDataParameters.toString()
				+ urlCsvDataSourceNameSeparator.toString() + urlCsvDataSourceNameEncoding.toString()
				+ urlCsvDataSourceNameColumns.toString() + "&user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", body).addHeader("Content-Type", "multipart/form-data").build();
		Response response = client.newCall(request).execute();
		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;
	}

}
