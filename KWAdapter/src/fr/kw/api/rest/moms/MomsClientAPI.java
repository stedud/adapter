package fr.kw.api.rest.moms;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.commons.io.FileUtils;

import fr.kw.api.rest.moms.search.SearchParameter;
import fr.utils.LogHelper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MomsClientAPI {

	protected String[] urlBases;
	protected URL[] urlBasesRest;
	protected Random random;

	public static void main(String[] args) {

	}

	public MomsClientAPI(String[] urlBases) throws MalformedURLException {

		if (urlBases == null || urlBases.length == 0)
			throw new MalformedURLException("No URL defined");
		this.urlBases = urlBases;

		this.urlBasesRest = new URL[urlBases.length];
		for (int i = 0; i < this.urlBasesRest.length; i++) {
			this.urlBasesRest[i] = new URL(this.urlBases[i] + "/moms-integration-adapter");

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
			LogHelper.debug("Testing url " + url);
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			LogHelper.debug("response url " + url + " : " + responseCode);
			return (200 <= responseCode && responseCode <= 404);
		} catch (IOException exception) {
			return false;
		}
	}

	public OkHttpClient getClient() {
		// Add here certificate manamgenet if needed

		Builder builder = new OkHttpClient().newBuilder().readTimeout(10, TimeUnit.MINUTES)
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

	public fr.kw.api.rest.mtext.Response send(File mfd, File mcj, String user, String password, String passwordPlain)
			throws IOException {// TODO : implements

		OkHttpClient client = getClient();

		MediaType mediaType = MediaType.parse("application/json");
		byte[] bytes = FileUtils.readFileToByteArray(mfd);
		String b64Mfd = Base64.getEncoder().encodeToString(bytes);
		bytes = FileUtils.readFileToByteArray(mcj);
		String b64Mcj = Base64.getEncoder().encodeToString(bytes);

		byte[] requestBody = ("{\n \"mfd\": \"" + b64Mfd + "\",\n \"mcj\": \"" + b64Mcj
				+ "\",\n \"fileType\": \"MFD\",\n \"properties\": []\n}").getBytes();
		b64Mcj = null;
		b64Mfd = null;
		RequestBody body = RequestBody.create(requestBody, mediaType);

		URL urlBase = getUrlBase();
		Request request = new Request.Builder().url(urlBase + "/document/send?" + "user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", body).addHeader("Content-Type", "application/json").build();
		LogHelper.debug("MomsClientAPI.send, build url from " + urlBase + "/document/send");
		LogHelper.debug("MomsClientAPI.send, url=" + request.url());

		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;
	}

	public fr.kw.api.rest.mtext.Response listDocumentsWithExclusiveSplitDocs(List<SearchParameter> searchParams,
			String user, String password, String passwordPlain) throws IOException {/// input-doc/with-exclusive-split-docs

		OkHttpClient client = getClient();

		MediaType mediaType = MediaType.parse("application/json");

		StringBuffer body = new StringBuffer("{\"select\":[\n");
		boolean first = true;
		for (SearchParameter param : searchParams) {
			if (!first) {
				body.append(",\n");
			}

			first = false;
			body.append(param.toJSonString());
		}
		body.append("\n]}");

		// System.out.println("/input-doc/with-exclusive-split-docs, body=" +
		// body.toString());

		RequestBody requestBody = RequestBody.create(mediaType, body.toString().getBytes());
		Request request = new Request.Builder().url(getUrlBase() + "/input-doc/with-exclusive-split-docs?" + "user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", requestBody).addHeader("Content-Type", "application/json").build();

		Response response = client.newCall(request).execute();

		byte[] bodyResponse = response.body().bytes();
		fr.kw.api.rest.mtext.Response myResponse = new fr.kw.api.rest.mtext.Response();
		myResponse.setSuccess(response.isSuccessful());
		myResponse.setMessage(response.message());
		myResponse.setBody(bodyResponse);
		myResponse.setResponse(new String(bodyResponse));
		return myResponse;

	}

	public fr.kw.api.rest.mtext.Response updateMedata(List<SearchParameter> searchParams, Map<String, String> setParams,
			String user, String password, String passwordPlain) throws IOException {

		/**
		 * 
		 * OkHttpClient client = new OkHttpClient().newBuilder() .build(); MediaType
		 * mediaType = MediaType.parse("application/json"); RequestBody body =
		 * RequestBody.create(mediaType, "{\n \"select\": [\n {\n \"equal\":
		 * {\"logicalOperator\":\"AND\",
		 * \"parameterName\":\"KW_STATUS\",\"value\":\"WAIT_FOR_STACK_PROCESSING\"}\n
		 * }\n ],\n \"updateParameters\": [\n {\n \"name\": \"PDF_FILE_NAME\",\n
		 * \"value\":\"TUTU\"\n }\n ],\n \"allowUpdateMultiInputdocs\": true\n}");
		 * Request request = new Request.Builder()
		 * .url("http://localhost:8080/moms-integration-adapter/split-doc/update?user=kwsoft2&passwordplain=kwsoft2")
		 * .method("POST", body) .addHeader("Content-Type", "application/json")
		 * .build(); Response response = client.newCall(request).execute();
		 * 
		 * 
		 */
		OkHttpClient client = getClient();

		MediaType mediaType = MediaType.parse("application/json");

		StringBuffer body = new StringBuffer("{\"select\":[\n");
		boolean first = true;
		for (SearchParameter param : searchParams) {
			if (!first) {
				body.append(",\n");
			}
			body.append(param.toJSonString());
			first = false;
		}
		body.append("\n],\n");

		first = true;
		body.append("\"updateParameters\":[\n");
		for (Entry<String, String> pair : setParams.entrySet()) {

			if (!first) {
				body.append(",\n");
			}
			body.append("{\"name\":\"");
			body.append(pair.getKey());
			body.append("\",\n\"value\":\"");
			body.append(pair.getValue().replace("\\", "\\\\"));
			body.append("\"}");
			first = false;
		}
		body.append("],\n\"allowUpdateMultiInputdocs\": true\n}");

		RequestBody requestBody = RequestBody.create(mediaType, body.toString().getBytes());
		Request request = new Request.Builder().url(getUrlBase() + "/split-doc/update?" + "user="
				+ URLEncoder.encode(user, StandardCharsets.UTF_8.name())
				+ (password != null ? "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) : "")
				+ (passwordPlain != null
						? "&passwordplain=" + URLEncoder.encode(passwordPlain, StandardCharsets.UTF_8.name())
						: ""))
				.method("POST", requestBody).addHeader("Content-Type", "application/json").build();

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
