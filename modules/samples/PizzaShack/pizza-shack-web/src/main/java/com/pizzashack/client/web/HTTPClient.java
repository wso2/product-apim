package com.pizzashack.client.web;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HTTPClient {

	private ThreadSafeClientConnManager connManager;
	private DefaultHttpClient client;

	public HTTPClient() {
		connManager = getHTTPConnectionManager();
		client = configureHTTPClient(connManager);
	}

	public HttpResponse doPost(String url, String token, final String payload, String contentType) throws IOException {
		HttpUriRequest request = new HttpPost(url);
		addSecurityHeaders(request, token);

		HttpEntityEnclosingRequest entityEncReq = (HttpEntityEnclosingRequest) request;
		EntityTemplate ent = new EntityTemplate(new ContentProducer() {
			public void writeTo(OutputStream outputStream) throws IOException {
				outputStream.write(payload.getBytes());
				outputStream.flush();
			}
		});
		ent.setContentType(contentType);
		entityEncReq.setEntity(ent);
		return client.execute(request);		
	}
	
	public HttpResponse doGet(String url, String token) throws IOException {
		HttpUriRequest request = new HttpGet(url);
		addSecurityHeaders(request, token);
		return client.execute(request);
	}
	
	public String getResponsePayload(HttpResponse response) throws IOException {
		StringBuffer buffer = new StringBuffer();
		InputStream in = null;
		try {
			if (response.getEntity() != null) {
			    in = response.getEntity().getContent();
			    int length;
			    byte[] tmp = new byte[2048];
			    while ((length = in.read(tmp)) != -1) {
			        buffer.append(new String(tmp, 0, length));
			    }
			}
		} catch (IllegalStateException e) {			
			e.printStackTrace();
		} finally {
			if (in != null) {
				in.close();
			}
		}

        return buffer.toString();
	}

	public HttpResponse doPut(String url, String token, final String payload, String contentType) throws IOException {
		HttpUriRequest request = new HttpPut(url);
		addSecurityHeaders(request, token);

		HttpEntityEnclosingRequest entityEncReq = (HttpEntityEnclosingRequest) request;
		EntityTemplate ent = new EntityTemplate(new ContentProducer() {
			public void writeTo(OutputStream outputStream) throws IOException {
				outputStream.write(payload.getBytes());
				outputStream.flush();
			}
		});
		ent.setContentType(contentType);
		entityEncReq.setEntity(ent);
		return client.execute(request);
	}

	public HttpResponse doDelete(String url, String token) throws IOException {
		HttpUriRequest request = new HttpDelete(url);
		addSecurityHeaders(request, token);
		return client.execute(request);
	}

	private DefaultHttpClient configureHTTPClient(
			ThreadSafeClientConnManager connManager) {
		connManager.setDefaultMaxPerRoute(1000);
		DefaultHttpClient client = new DefaultHttpClient(connManager);
		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 30000);
		HttpConnectionParams.setSoTimeout(params, 30000);
		client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
			public boolean retryRequest(IOException e, int i,
					HttpContext httpContext) {
				return false;
			}
		});
		return client;
	}

	private ThreadSafeClientConnManager getHTTPConnectionManager() {
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		SocketFactory sf = PlainSocketFactory.getSocketFactory();
		supportedSchemes.register(new Scheme("http", sf, 80));

		return new ThreadSafeClientConnManager(supportedSchemes);
	}
	
	private void addSecurityHeaders(HttpRequest request, String token) {
		if (token != null) {
			request.setHeader(HttpHeaders.AUTHORIZATION, token);
		}
	}
}
