package org.wso2.carbon.apimgt.rest.integration.tests.microgateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedApiClient {

    Logger logger = LoggerFactory.getLogger(MockedApiClient.class);

    public static HttpClient createClient() {
        HttpClient client = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } }, new SecureRandom());
            SSLSocketFactory sf = new SSLSocketFactory(sslContext);
            Scheme httpsScheme = new Scheme("https", 443, sf);
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(httpsScheme);
            BasicClientConnectionManager cm = new BasicClientConnectionManager(schemeRegistry);
            client = new DefaultHttpClient(cm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return client;
    }

    public String get(String apiKey, String context) throws NoSuchAlgorithmException, KeyManagementException {
        String output = null;
            HttpClient client = createClient();
            HttpGet request = new HttpGet("https://localhost:9092/"+context);
        try {
            request.addHeader("apikey",apiKey);
            HttpResponse response = client.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            System.out.println("Response Code: " + responseCode);
            System.out.println("Mocked API Content:-");
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            output = rd.readLine();
            System.out.println(output);
        } catch (IOException ioe) {
            logger.info("Error occured", ioe);
        }
        return output;
    }

}
