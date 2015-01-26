/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.test.utils;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class APIMgtTestUtil {

	public static APIBean getAPIBeanFromHttpResponse(HttpResponse httpResponse) {
		JSONObject jsonObject = null;
		String APIName = null;
		String APIProvider = null;
		String APIVersion = null;
		APIBean apiBean = null;
		try {
			jsonObject = new JSONObject(httpResponse.getData());
			APIName = ((JSONObject) jsonObject.get("api")).getString("name");
			APIVersion = ((JSONObject) jsonObject.get("api")).getString("version");
			APIProvider = ((JSONObject) jsonObject.get("api")).getString("provider");
			APIIdentifier identifier = new APIIdentifier(APIProvider, APIName, APIVersion);
			apiBean = new APIBean(identifier);
			apiBean.setContext(((JSONObject) jsonObject.get("api")).getString("context"));
			apiBean.setDescription(((JSONObject) jsonObject.get("api")).getString("description"));
			apiBean.setWsdlUrl(((JSONObject) jsonObject.get("api")).getString("wsdl"));
			apiBean.setTags(((JSONObject) jsonObject.get("api")).getString("tags"));
			apiBean.setAvailableTiers(
					((JSONObject) jsonObject.get("api")).getString("availableTiers"));
			apiBean.setThumbnailUrl(((JSONObject) jsonObject.get("api")).getString("thumb"));
			apiBean.setSandboxUrl(((JSONObject) jsonObject.get("api")).getString("sandbox"));
			apiBean.setBusinessOwner(((JSONObject) jsonObject.get("api")).getString("bizOwner"));
			apiBean.setBusinessOwnerEmail(
					((JSONObject) jsonObject.get("api")).getString("bizOwnerMail"));
			apiBean.setTechnicalOwner(((JSONObject) jsonObject.get("api")).getString("techOwner"));
			apiBean.setTechnicalOwnerEmail(
					((JSONObject) jsonObject.get("api")).getString("techOwnerMail"));
			apiBean.setWadlUrl(((JSONObject) jsonObject.get("api")).getString("wadl"));
			apiBean.setVisibility(((JSONObject) jsonObject.get("api")).getString("visibility"));
			apiBean.setVisibleRoles(((JSONObject) jsonObject.get("api")).getString("roles"));
			apiBean.setEndpointUTUsername(
					((JSONObject) jsonObject.get("api")).getString("epUsername"));
			apiBean.setEndpointUTPassword(
					((JSONObject) jsonObject.get("api")).getString("epPassword"));
			apiBean.setEndpointSecured((Boolean.getBoolean(
					((JSONObject) jsonObject.get("api")).getString("endpointTypeSecured"))));
			apiBean.setTransports(((JSONObject) jsonObject.get("api")).getString("transport_http"));
			apiBean.setTransports(
					((JSONObject) jsonObject.get("api")).getString("transport_https"));
			apiBean.setInSequence(((JSONObject) jsonObject.get("api")).getString("inSequence"));
			apiBean.setOutSequence(((JSONObject) jsonObject.get("api")).getString("outSequence"));
			apiBean.setAvailableTiers(
					((JSONObject) jsonObject.get("api")).getString("availableTiersDisplayNames"));
			//-----------Here are some of unused properties, if we need to use them add params to APIBean class
			//((JSONObject) jsonObject.get("api")).getString("name");
			//((JSONObject) jsonObject.get("api")).getString("endpoint");
			//((JSONObject) jsonObject.get("api")).getString("subscriptionAvailability");
			//((JSONObject) jsonObject.get("api")).getString("subscriptionTenants");
			//((JSONObject) jsonObject.get("api")).getString("endpointConfig");
			//((JSONObject) jsonObject.get("api")).getString("responseCache");
			//(((JSONObject) jsonObject.get("api")).getString("cacheTimeout");
			//((JSONObject) jsonObject.get("api")).getString("endpointConfig");
			//((JSONObject) jsonObject.get("api")).getString("version");
			//((JSONObject) jsonObject.get("api")).getString("apiStores");
			// ((JSONObject) jsonObject.get("api")).getString("provider");
			//)((JSONObject) jsonObject.get("api")).getString("tierDescs");
			//((JSONObject) jsonObject.get("api")).getString("subs");
			//((JSONObject) jsonObject.get("api")).getString("context");
			// apiBean.setLastUpdated(Date.parse((JSONObject); jsonObject.get("api")).getString("lastUpdated")));
			// apiBean.setUriTemplates((JSONObject) jsonObject.get("api")).getString("templates"));
		} catch (JSONException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return apiBean;
	}

    public static void sendGetRequest(String url, String accessToken) throws XPathExpressionException, IOException {
        HttpResponse httpResponse;

        URL urlAPI = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setReadTimeout(10000);
        //setting headers

        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        conn.connect();
        // Get the response
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            httpResponse = new HttpResponse(sb.toString(), conn.getResponseCode());
            httpResponse.setResponseMessage(conn.getResponseMessage());
        } catch (IOException ignored) {

        } finally {
            if (rd != null) {
                rd.close();
            }
        }
    }


    public static String getDecodedJWT(String serverMessage) {
        String[] headerArray = serverMessage.split("\n");
        String[] jwtEncodedArray = headerArray[1].trim().split(":");
        String[] jwtTokenArray = jwtEncodedArray[1].split(Pattern.quote("."));

        byte[] jwtByteArray = Base64.decodeBase64(jwtTokenArray[1].getBytes());
        return new String(jwtByteArray);
    }
}
