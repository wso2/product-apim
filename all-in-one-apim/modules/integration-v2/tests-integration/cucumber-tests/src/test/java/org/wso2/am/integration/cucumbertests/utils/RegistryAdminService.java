/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils;

import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.io.IOException;

/**
 * Narrow wrapper for the Carbon registry admin SOAP operations that have no REST equivalent.
 *
 * <p>This is intentionally called by an explicit setup step, as required by the integration-v2 provisioning
 * boundary. It is not a container-listener mutation and it never uses a side-channel token: the caller supplies
 * the actor whose tenant registry is being repaired.</p>
 */
public final class RegistryAdminService {

    private static final String SERVICE_PATH = "services/ResourceAdminService";
    private static final String OPERATION_NAMESPACE = "http://services.resource.registry.carbon.wso2.org";
    private static final String COLLECTION_MEDIA_TYPE = "application/vnd.wso2.registry.collection";
    private static final String THROTTLE_DATA_PATH = "/_system/governance/event/topics/throttledata";

    private RegistryAdminService() {
    }

    /** Ensures the complete registry collection hierarchy required by the throttle-data subscription. */
    public static boolean ensureThrottleDataHierarchy(String baseUrl, User actor) throws IOException {

        String current = "";
        boolean repaired = false;
        for (String segment : THROTTLE_DATA_PATH.substring(1).split("/")) {
            current += "/" + segment;
            repaired |= ensureCollection(baseUrl, actor, current);
        }
        return repaired;
    }

    private static boolean ensureCollection(String baseUrl, User actor, String path) throws IOException {

        HttpResponse existing = getResourceData(baseUrl, actor, path);
        if (existing != null && existing.getResponseCode() == 200) {
            return false;
        }
        if (existing == null || existing.getResponseCode() != 500) {
            throw new IOException("ResourceAdminService could not inspect registry collection " + path + ": got="
                    + describe(existing));
        }

        int separator = path.lastIndexOf('/');
        String parent = separator == 0 ? "/" : path.substring(0, separator);
        String name = path.substring(separator + 1);
        HttpResponse created = Requests.soap(baseUrl + SERVICE_PATH,
                addCollectionEnvelope(parent, name), "urn:addCollection", actor.getUserName(), actor.getPassword());
        if (created == null || created.getResponseCode() != 200) {
            throw new IOException("ResourceAdminService.addCollection failed for " + path + ": got="
                    + describe(created));
        }

        HttpResponse verified = getResourceData(baseUrl, actor, path);
        if (verified == null || verified.getResponseCode() != 200) {
            throw new IOException("Registry collection " + path + " was not readable after addCollection: got="
                    + describe(verified));
        }
        return true;
    }

    /** Intermediate read: its response is consumed here and must not replace a scenario assertion response. */
    private static HttpResponse getResourceData(String baseUrl, User actor, String path) throws IOException {

        return SimpleHTTPClient.getInstance().sendSoapRequest(baseUrl + SERVICE_PATH,
                getResourceDataEnvelope(path), "urn:getResourceData", actor.getUserName(), actor.getPassword());
    }

    private static String addCollectionEnvelope(String parent, String name) {

        return envelope("<ser:addCollection>"
                + "<ser:parentPath>" + Utils.escapeXml(parent) + "</ser:parentPath>"
                + "<ser:collectionName>" + Utils.escapeXml(name) + "</ser:collectionName>"
                + "<ser:mediaType>" + COLLECTION_MEDIA_TYPE + "</ser:mediaType>"
                + "<ser:description>Integration-v2 throttleData infrastructure</ser:description>"
                + "</ser:addCollection>");
    }

    private static String getResourceDataEnvelope(String path) {

        return envelope("<ser:getResourceData><ser:paths>" + Utils.escapeXml(path)
                + "</ser:paths></ser:getResourceData>");
    }

    private static String envelope(String body) {

        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"" + OPERATION_NAMESPACE + "\"><soapenv:Header/><soapenv:Body>"
                + body + "</soapenv:Body></soapenv:Envelope>";
    }

    private static String describe(HttpResponse response) {

        return response == null ? "null" : response.getResponseCode() + "/" + response.getData();
    }
}
