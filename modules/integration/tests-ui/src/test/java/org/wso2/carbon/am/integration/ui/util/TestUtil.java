/*
*Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.am.integration.ui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class TestUtil {
	
	public boolean createTenant(String adminUserName, String adminPassword, String serviceUrl) {
		return true;
	}
	
	/**
	 * Login to API Store or Publisher
	 * @param userName
	 * @param password
	 * @param URL API Store or Publisher URL
	 * @return
	 * @throws Exception
	 */
	public static HttpContext login(String userName, String password, String URL) throws Exception {
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL + APIMTestConstants.APISTORE_LOGIN_URL);
        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(3);

        params.add(new BasicNameValuePair(APIMTestConstants.API_ACTION, APIMTestConstants.API_LOGIN_ACTION));
        params.add(new BasicNameValuePair(APIMTestConstants.APISTORE_LOGIN_USERNAME, userName));
        params.add(new BasicNameValuePair(APIMTestConstants.APISTORE_LOGIN_PASSWORD, password));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpResponse response = httpclient.execute(httppost, httpContext);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        boolean isError=Boolean.parseBoolean(responseString.split(",")[0].split(":")[1].split("}")[0].trim());

        if (isError) {
            String errorMsg=responseString.split(",")[1].split(":")[1].split("}")[0].trim();
            throw new Exception("Error while Login to API Publisher : " + errorMsg);

        } else{
            return httpContext;
        }

	}
	
	/**
	 * Create an API with provided Name and Version
	 * @param providerName
	 * @param apiName
	 * @param apiVersion
	 * @param httpContext
	 * @param publisherURL
	 * @return
	 * @throws Exception
	 */
	public static boolean addAPI(String providerName, String apiName, String apiVersion, HttpContext httpContext, String publisherURL) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(publisherURL + APIMTestConstants.APIPUBLISHER_ADD_URL);
		List<NameValuePair> params = getAPICreateParamsList(providerName, apiName, apiVersion, APIMTestConstants.API_ADD_ACTION);
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		HttpResponse response = httpclient.execute(httppost, httpContext);
        
		HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        boolean isError=Boolean.parseBoolean(responseString.split(",")[0].split(":")[1].split("}")[0].trim());
        File createdTmpFile = new File("tmp/icon");//With multipart file uploading
        if(createdTmpFile.exists()){
            createdTmpFile.delete();
        }
        if (!isError) { //If API creation success
        	return true;
        }else{
        	String errorMsg=responseString.split(",")[1].split(":")[1].split("}")[0].trim();
        	throw new Exception("Error while adding the API " + errorMsg );
        }
	}
	
	/**
	 * Publish the given API
	 * @param providerName
	 * @param apiName
	 * @param apiVersion
	 * @param httpContext
	 * @param publisherURL
	 * @return
	 * @throws Exception
	 */
	public static boolean publishAPI(String providerName, String apiName, String apiVersion, HttpContext httpContext, String publisherURL) throws Exception{
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(publisherURL + APIMTestConstants.APIPUBLISHER_PUBLISH_URL);
		List<NameValuePair> paramVals = new ArrayList<NameValuePair>();
        paramVals.add(new BasicNameValuePair(APIMTestConstants.API_ACTION, APIMTestConstants.API_CHANGE_STATUS_ACTION));
        paramVals.add(new BasicNameValuePair("name", apiName));
        paramVals.add(new BasicNameValuePair("provider", providerName));
        paramVals.add(new BasicNameValuePair("version", apiVersion));
        paramVals.add(new BasicNameValuePair("status", APIMTestConstants.PUBLISHED));
        paramVals.add(new BasicNameValuePair("publishToGateway", "true"));
        paramVals.add(new BasicNameValuePair("deprecateOldVersions", "false"));
        paramVals.add(new BasicNameValuePair("requireResubscription", "false"));
        
        httppost.setEntity(new UrlEncodedFormEntity(paramVals, "UTF-8"));
        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost,httpContext);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        boolean isError = Boolean.parseBoolean(responseString.split(",")[0].split(":")[1].split("}")[0].trim());
        //If API publishing success
        if (!isError) {  
            return true;

        }else{
            String errorMsg = responseString.split(",")[1].split(":")[1].split("}")[0].trim();
            throw new Exception("Error while publishing the API- " + errorMsg);

        }
	}
	
	/**
	 * Returns the parameters required to create an API
	 * @param providerName
	 * @param apiName
	 * @param apiVersion
	 * @param action
	 * @return
	 * @throws Exception
	 */
	private static List<NameValuePair> getAPICreateParamsList(String providerName, String apiName, String apiVersion, 
	                                                          String action) throws Exception {
        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(APIMTestConstants.API_ACTION, action));
        params.add(new BasicNameValuePair("name", apiName));
        params.add(new BasicNameValuePair("version", apiVersion));
        params.add(new BasicNameValuePair("provider", providerName));
        params.add(new BasicNameValuePair("endpoint", "http://localhost:9090/test"));
        
        String endpont = "http://localhost:9090/test";
        
        String endpoint_config = "{\"production_endpoints\":{\"url\":\""+ endpont +"\", \"config\":null},\"sandbox_endpoint\":{\"url\":\""
        							+ endpont +"\",\"config\":null},\"endpoint_type\":\"http\"}";
        params.add(new BasicNameValuePair("endpoint_config", endpoint_config));
        
        params.add(new BasicNameValuePair("visibility", "public"));

        params.add(new BasicNameValuePair("http_checked", "http"));
        params.add(new BasicNameValuePair("https_checked", "https"));

        params.add(new BasicNameValuePair("tiersCollection", "Gold"));
        params.add(new BasicNameValuePair("context", apiName));

        params.add(new BasicNameValuePair("resourceCount", "0"));
        params.add(new BasicNameValuePair("uriTemplate-0", "/*"));
        params.add(new BasicNameValuePair("resourceMethod-0", "GET"));
        params.add(new BasicNameValuePair("resourceMethodAuthType-0", "Application"));
        params.add(new BasicNameValuePair("resourceMethodThrottlingTier-0", "Unlimited"));

        return params;
    }
	
	
	/**
	 * Adds an API Subscription for given API using given Application and Tier
	 * @param providerName
	 * @param apiName
	 * @param apiVersion
	 * @param tier
	 * @param appName
	 * @param httpContext
	 * @param storeURL
	 * @return
	 * @throws Exception
	 */
	public static boolean addSubscription(String providerName, String apiName, String apiVersion,
	                                      String tier, String appName, HttpContext httpContext, String storeURL) throws Exception{
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(storeURL + APIMTestConstants.ADD_SUBSCRIPTION_URL);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(APIMTestConstants.API_ACTION, APIMTestConstants.ADD_SUBSCRIPTION_ACTION));
        params.add(new BasicNameValuePair("name", apiName));
        params.add(new BasicNameValuePair("version", apiVersion));
        params.add(new BasicNameValuePair("provider", replaceEmailDomain(providerName)));
        params.add(new BasicNameValuePair("tier", tier));
        params.add(new BasicNameValuePair("applicationName", appName));
        
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost,httpContext);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        boolean isError = Boolean.parseBoolean(responseString.split(",")[0].split(":")[1].split("}")[0].trim());
        //If API publishing success
        if (!isError) {  
            return true;

        }else{
            String errorMsg = responseString.split(",")[1].split(":")[1].split("}")[0].trim();
            throw new Exception("Error while subscribing to the API- " + errorMsg);

        }
	}
	
	/**
	 * Generate Application tokens using given Application and KeyType
	 * @param keyType
	 * @param appName
	 * @param httpContext
	 * @param storeURL
	 * @return
	 * @throws Exception
	 */
	public static boolean generateApplicationtokens(String keyType, String appName, HttpContext httpContext, String storeURL) throws Exception{
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(storeURL + APIMTestConstants.ADD_SUBSCRIPTION_URL);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(APIMTestConstants.API_ACTION, APIMTestConstants.GENERATE_APPLICATION_KEY_ACTION));
        params.add(new BasicNameValuePair("application", appName));
        params.add(new BasicNameValuePair("authorizedDomains", "ALL"));
        params.add(new BasicNameValuePair("callbackUrl", ""));
        params.add(new BasicNameValuePair("keytype", keyType));
        params.add(new BasicNameValuePair("validityTime", "3600"));
        
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost,httpContext);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        boolean isError = Boolean.parseBoolean(responseString.split(",")[0].split(":")[1].split("}")[0].trim());
        //If API publishing success
        if (!isError) {  
            return true;

        }else{
            String errorMsg = responseString.split(",")[1].split(":")[1].split("}")[0].trim();
            throw new Exception("Error while subscribing to the API- " + errorMsg);

        }

	}
	
	/**
	 * Replaces the '@' with -AT- in the provided String
	 * @param input
	 * @return
	 */
	public static String replaceEmailDomain(String input){
        if(input!=null&& input.contains(APIMTestConstants.EMAIL_DOMAIN_SEPARATOR) ){
            input=input.replace(APIMTestConstants.EMAIL_DOMAIN_SEPARATOR,APIMTestConstants.EMAIL_DOMAIN_SEPARATOR_REPLACEMENT);
        }
        return input;
    }

	
	/**
	 * Returns the username with tenant domain prefix
	 * @param userName
	 * @param tenantDomain
	 * @return
	 */
	public static String getTenantUserName(String userName, String tenantDomain) {
		return userName + "@" + tenantDomain;		
	}
	
	/**
	 * Returns the tenant URL of the given url prefix.
	 * @param productURL
	 * @param tenantDomain
	 * @param urlPrefix
	 * @return
	 * @throws Exception
	 */
	public static String getTenantURL(String productURL, String tenantDomain, String urlPrefix) throws Exception{
		if(productURL.contains("/carbon")) {
            return productURL.split("carbon")[0] + "t/" + tenantDomain + urlPrefix;
        } else {
        	throw new Exception("Error while composing Publisher Login URL");
        }
    }
	
	

}
