package org.wso2.carbon.apimgt.rest.integration.tests.microgateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
//import javax.net.ssl.SSLSocketFactory;
//import java.util.ArrayList;
//import java.util.List;
//import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
//import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
//import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

public class MockedApiClient {

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
          //  HttpGet request = new HttpGet("https://www.mocky.io/v2/59a96c49100000300d3e0afa")
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

        }
        return output;
    }

    public void post() {
//        HttpClient client = HttpClientBuilder.create().build();
//        HttpPost post = new HttpPost("http://www.technicalkeeda.com/post-request");
//        try {
//            List <NameValuePair> nameValuePairs = new ArrayList <NameValuePair> ();
//            nameValuePairs.add(new BasicNameValuePair("name", "Yashwant"));
//            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//
//            HttpResponse response = client.execute(post);
//
//            int responseCode = response.getStatusLine().getStatusCode();
//            System.out.println("**POST** request Url: " + post.getURI());
//            System.out.println("Parameters : " + nameValuePairs);
//            System.out.println("Response Code: " + responseCode);
//            System.out.println("Content:-\n");
//            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            String line = "";
//            while ((line = rd.readLine()) != null) {
//                System.out.println(line);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
