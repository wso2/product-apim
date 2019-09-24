package org.wso2.carbon.apimgt.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class ClientAuthenticator {

    public static final double JAVA_VERSION;
    public static final boolean IS_ANDROID;
    public static final int ANDROID_SDK_VERSION;
    private static TrustManager trustAll;
    private static String consumerKey = null;
    private static String consumerSecret = null;
    private static final String TLS_PROTOCOL = "TLS";

    static {
        JAVA_VERSION = Double.parseDouble(System.getProperty("java.specification.version"));
        boolean isAndroid;
        try {
            Class.forName("android.app.Activity");
            isAndroid = true;
        } catch (ClassNotFoundException e) {
            isAndroid = false;
        }
        IS_ANDROID = isAndroid;
        int sdkVersion = 0;
        if (IS_ANDROID) {
            try {
                sdkVersion = Class.forName("android.os.Build$VERSION").getField("SDK_INT").getInt(null);
            } catch (Exception e) {
                try {
                    sdkVersion = Integer.parseInt((String) Class.forName("android.os.Build$VERSION").getField("SDK").get(null));
                } catch (Exception e2) {
                }
            }
        }
        ANDROID_SDK_VERSION = sdkVersion;

        trustAll = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
    }


//    public String getAccessTokenForStore() {
//        return getAccessToken("apim:subscribe apim:signup apim:workflow_approve");
//    }
//
//    private String getAccessTokenForPublisher() {
//        return getAccessToken("apim:api_view apim:api_update apim:api_delete apim:api_create apim:api_publish apim:tier_view apim:tier_manage " +
//                "apim:subscription_view apim:subscription_block apim:apidef_update apim:workflow_approve");
//    }

    public static String getAccessToken(String scopeList, String appName, String callBackURL, String tokenScope, String appOwner,
                                        String grantType, String dcrEndpoint, String username, String password, String tenantDomain, String tokenEndpoint) {
        if (consumerKey == null) {
            makeDCRRequest(appName,  callBackURL,  tokenScope,  appOwner, grantType,  dcrEndpoint,  username,  password,  tenantDomain);
        }
        URL url;
        HttpsURLConnection urlConn = null;
        //calling token endpoint
        try {
            url = new URL(tokenEndpoint);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String clientEncoded = DatatypeConverter.printBase64Binary(
                    (consumerKey + ':' + consumerSecret).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            String postParams = "grant_type=client_credentials";
            if (!scopeList.isEmpty()) {
                postParams += "&scope=" + scopeList;
            }
            urlConn.setHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            urlConn.setSSLSocketFactory(sslContext.getSocketFactory());
            urlConn.getOutputStream().write((postParams).getBytes("UTF-8"));
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                String responseStr = getResponseString(urlConn.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(responseStr).getAsJsonObject();
                return obj.get("access_token").getAsString();
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }



    private static void makeDCRRequest(String appName, String callBackURL, String tokenScope, String appOwner,
                                       String grantType, String dcrEndpoint, String username, String password, String tenantDomain) {
        String applicationName = appName;
        URL url;
        HttpURLConnection urlConn = null;
        try {
            //Create json payload for DCR endpoint
            JsonObject json = new JsonObject();
            json.addProperty("callbackUrl", callBackURL);
            json.addProperty("clientName", applicationName);
            json.addProperty("tokenScope", tokenScope);
            json.addProperty("grantType", grantType);

            String clientEncoded;

            if (StringUtils.isEmpty(tenantDomain)) {
                json.addProperty("owner", appOwner);
                clientEncoded = DatatypeConverter.printBase64Binary((System.getProperty("systemUsername",
                        username) + ':' + System.getProperty("systemUserPwd", password))
                        .getBytes(StandardCharsets.UTF_8));
            } else {
                json.addProperty("owner", username + '@' + tenantDomain);
                clientEncoded = DatatypeConverter.printBase64Binary((username + '@' + tenantDomain + ':' + password)
                        .getBytes(StandardCharsets.UTF_8));
            }

            // Calling DCR endpoint
            url = new URL(dcrEndpoint);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded); //temp fix
            urlConn.getOutputStream().write((json.toString()).getBytes("UTF-8"));

            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {  //If the DCR call is success
                String responseStr = getResponseString(urlConn.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject jObj = parser.parse(responseStr).getAsJsonObject();
                consumerKey = jObj.getAsJsonPrimitive("clientId").getAsString();
                consumerSecret = jObj.getAsJsonPrimitive("clientSecret").getAsString();
            } else { //If DCR call fails
                throw new RuntimeException("DCR call failed. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String errorMsg = "Can not create OAuth application  : " + applicationName;
            throw new RuntimeException(errorMsg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }


    private static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String file = "";
            String str;
            while ((str = buffer.readLine()) != null) {
                file += str;
            }
            return file;
        }
    }
}
