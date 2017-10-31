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

public class ApiClient {
//    protected Logger log = Logger.getLogger(this.getClass());
    public static void main(String[] args) throws Exception {
        ApiClient client = new ApiClient();
        System.out.println("Http GET Request Example\n");
        client.get();
        System.out.println("\nHttp POST Request Example\n");
        client.post();
    }

    public String get() throws NoSuchAlgorithmException, KeyManagementException {
        String line = "sabeena";
        try {
            SSLSocketFactory sf = null;
            SSLContext sslContext = SSLContext.getInstance("SSL");

// set up a TrustManager that trusts everything
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    System.out.println("getAcceptedIssuers =============");
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {
                    System.out.println("checkClientTrusted =============");
                }

                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {
                    System.out.println("checkServerTrusted =============");
                }
            } }, new SecureRandom());

            //try {

            sf = new SSLSocketFactory(sslContext);
           // } catch (NoSuchAlgorithmException e) {
                //System.out.println("Failed to initialize SSL handling.", e);
            //} catch (KeyManagementException e) {
                //System.out.println("Failed to initialize SSL handling.", e);
           // }
            Scheme httpsScheme = new Scheme("https", 443, sf);
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(httpsScheme);

// apache HttpClient version >4.2 should use BasicClientConnectionManager
            BasicClientConnectionManager cm = new BasicClientConnectionManager(schemeRegistry);
           // HttpClient client = new DefaultHttpClient(cm);


            HttpClient client = new DefaultHttpClient(cm);
            //9292
            //9443

            HttpGet request = new HttpGet("https://localhost:9092/api");
          //  HttpGet request = new HttpGet("https://www.mocky.io/v2/59a96c49100000300d3e0afa");
            request.addHeader("apikey","122456");
            HttpResponse response = client.execute(request);

            int responseCode = response.getStatusLine().getStatusCode();

            System.out.println("**GET** request Url: " + request.getURI());
            System.out.println("Response Code: " + responseCode);
            System.out.println("Content:-\n");
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            line = rd.readLine();
            System.out.println(line);
//            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//            //URL url = new URL("https://localhost:9092/api");
//            HttpClient client = HttpClientBuilder.create().build();
//            HttpGet request = new HttpGet("https://localhost:9092/api");
//            request.addHeader("apikey","122456");
//            request.setSSLSocketFacory(sslsocketfactory);
//            HttpResponse response = client.execute(request);
//
//            int responseCode = response.getStatusLine().getStatusCode();
//
//            System.out.println("**GET** request Url: " + request.getURI());
//            System.out.println("Response Code: " + responseCode);
//            System.out.println("Content:-\n");
//            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            String string = null;
//            while ((string = rd.readLine()) != null) {
//                System.out.println("Received " + string);
//            }



        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return line;
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
