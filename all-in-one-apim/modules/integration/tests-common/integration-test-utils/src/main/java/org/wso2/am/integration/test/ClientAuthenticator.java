package org.wso2.am.integration.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.ApplicationKeyBean;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import static org.wso2.am.integration.test.Constants.CHAR_AT;

public class ClientAuthenticator {

    public static final double JAVA_VERSION;
    private static TrustManager trustAll;
    private static String consumerKey = null;
    private static String consumerSecret = null;
    private static Map<String, ApplicationKeyBean> applicationKeyMap = new HashMap<>();
    private static final String TLS_PROTOCOL = "TLS";
    private static int count = 0;
    static {
        JAVA_VERSION = Double.parseDouble(System.getProperty("java.specification.version"));

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

    public static String getAccessToken(String scopeList, String appName, String callBackURL, String tokenScope, String appOwner,
                                        String grantType, String dcrEndpoint, String username, String password, String tenantDomain, String tokenEndpoint) {
        URL url;
        HttpsURLConnection urlConn = null;
        //calling token endpoint
        try {
            url = new URL(tokenEndpoint);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            ApplicationKeyBean applicationKeyBean = applicationKeyMap.get(appName);
            String clientEncoded = DatatypeConverter.printBase64Binary(
                    (applicationKeyBean.getConsumerKey()
                            + ':' + applicationKeyBean.getConsumerSecret()).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain) || username.contains(CHAR_AT)) {
                username = username + CHAR_AT + tenantDomain;
            }
            String postParams;
            if (APIMIntegrationConstants.GRANT_TYPE.PASSWORD.equals(grantType)) {
                postParams = "grant_type=password&username=" + username + "&password=" + password;
            } else {
                postParams = "grant_type=client_credentials";
            }
            if (!scopeList.isEmpty()) {
                postParams += "&scope=" + scopeList+" device_"+count;
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
                count++;
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

    public static ApplicationKeyBean makeDCRRequest(DCRParamRequest dcrParamRequest) {

        String applicationName = dcrParamRequest.getAppName();
        try {
            //Create json payload for DCR endpoint
            JsonObject json = new JsonObject();
            json.addProperty("callbackUrl", dcrParamRequest.getCallBackURL());
            json.addProperty("clientName", applicationName);
            json.addProperty("tokenScope", dcrParamRequest.getTokenScope());
            json.addProperty("grantType", dcrParamRequest.getGrantType());
            json.addProperty("saasApp", true);

            String clientEncoded;

            if (StringUtils.isEmpty(dcrParamRequest.getTenantDomain())) {
                json.addProperty("owner", dcrParamRequest.getAppOwner());
                clientEncoded = DatatypeConverter.printBase64Binary(
                        (System.getProperty("systemUsername", dcrParamRequest.getUsername()) + ':' + System
                                .getProperty("systemUserPwd", dcrParamRequest.getPassword()))
                                .getBytes(StandardCharsets.UTF_8));
            } else {
                json.addProperty("owner", dcrParamRequest.getUsername() + CHAR_AT + dcrParamRequest.getTenantDomain());
                clientEncoded = DatatypeConverter.printBase64Binary(
                        (dcrParamRequest.getUsername() + CHAR_AT + dcrParamRequest.getTenantDomain() + ':'
                                + dcrParamRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
            }

            // Calling DCR endpoint
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            HttpPost httpPost = new HttpPost();
            httpPost.setURI(URI.create(dcrParamRequest.getDcrEndpoint()));

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Authorization", "Basic " + clientEncoded);
            httpPost.setEntity(new StringEntity(json.toString()));
            try (CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == 200) {  //If the DCR call is success
                    try (InputStream content = httpResponse.getEntity().getContent()) {
                        String responseStr = IOUtils.toString(content);
                        ApplicationKeyBean applicationKeyBean = new ApplicationKeyBean();
                        JsonParser parser = new JsonParser();
                        JsonObject jObj = parser.parse(responseStr).getAsJsonObject();
                        applicationKeyBean.setConsumerKey(jObj.getAsJsonPrimitive("clientId").getAsString());
                        applicationKeyBean.setConsumerSecret(jObj.getAsJsonPrimitive("clientSecret").getAsString());
                        applicationKeyMap.put(dcrParamRequest.getAppName(), applicationKeyBean);
                        return applicationKeyBean;
                    }
                } else { //If DCR call fails
                    throw new RuntimeException("DCR call failed. Status code: " + statusCode);
                }
            }
        } catch (IOException e) {
            String errorMsg = "Can not create OAuth application  : " + applicationName;
            throw new RuntimeException(errorMsg, e);
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
