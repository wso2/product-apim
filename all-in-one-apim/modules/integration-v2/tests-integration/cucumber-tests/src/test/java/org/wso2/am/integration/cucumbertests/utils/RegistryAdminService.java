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
 * <p>Called by explicit setup steps and the distributed infrastructure initializer, which establishes the
 * shared broker hierarchy before consumers start. The caller supplies the actor; no token is minted here.</p>
 */
public final class RegistryAdminService {

    private static final String SERVICE_PATH = "services/ResourceAdminService";
    private static final String OPERATION_NAMESPACE = "http://services.resource.registry.carbon.wso2.org";
    private static final String COLLECTION_MEDIA_TYPE = "application/vnd.wso2.registry.collection";
    private static final String THROTTLE_DATA_PATH = "/_system/governance/event/topics/throttledata";

    private RegistryAdminService() {
    }

    /** Waits for the actual registry service, not just Carbon's HTTP listener, before dependent components start. */
    public static void awaitAvailable(String baseUrl, User actor, long timeoutMillis) throws InterruptedException {
        HttpResponse response = Utils.retryUntilWithInterval(timeoutMillis, 1000L, () -> {
            HttpResponse observed = getResourceData(baseUrl, actor, "/");
            if (observed != null && observed.getResponseCode() != 200
                    && observed.getResponseCode() != 404 && observed.getResponseCode() != 502
                    && observed.getResponseCode() != 503) {
                throw new IllegalStateException("Registry service prerequisite failed: " + describe(observed));
            }
            return observed;
        }, observed -> observed != null && observed.getResponseCode() == 200);
        if (response == null || response.getResponseCode() != 200) {
            throw new IllegalStateException("Registry service was not available within " + timeoutMillis
                    + "ms: " + describe(response));
        }
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
            requireCollection(existing, path);
            return false;
        }
        if (!isMissingCollection(existing, path)) {
            throw new IOException("ResourceAdminService could not inspect registry collection " + path + ": got="
                    + describe(existing));
        }

        int separator = path.lastIndexOf('/');
        String parent = separator == 0 ? "/" : path.substring(0, separator);
        String name = path.substring(separator + 1);
        HttpResponse created = SimpleHTTPClient.getInstance().withSocketTimeout(10000,
                () -> Requests.soap(baseUrl + SERVICE_PATH, addCollectionEnvelope(parent, name), "urn:addCollection",
                        actor.getUserName(), actor.getPassword()));
        if (created == null || created.getResponseCode() != 200) {
            throw new IOException("ResourceAdminService.addCollection failed for " + path + ": got="
                    + describe(created));
        }

        HttpResponse verified = getResourceData(baseUrl, actor, path);
        if (verified == null || verified.getResponseCode() != 200) {
            throw new IOException("Registry collection " + path + " was not readable after addCollection: got="
                    + describe(verified));
        }
        requireCollection(verified, path);
        return true;
    }

    private static void requireCollection(HttpResponse response, String path) throws IOException {
        try {
            var types = Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='resourceType']");
            if (types.size() != 1 || !"collection".equals(types.get(0).trim())) {
                throw new IOException("Registry path " + path + " is not a collection: " + describe(response));
            }
        } catch (org.jaxen.JaxenException | RuntimeException e) {
            throw new IOException("Could not parse registry collection metadata for " + path, e);
        }
    }

    /** Intermediate read: its response is consumed here and must not replace a scenario assertion response. */
    private static HttpResponse getResourceData(String baseUrl, User actor, String path) throws IOException {

        return SimpleHTTPClient.getInstance().withSocketTimeout(10000,
                () -> SimpleHTTPClient.getInstance().sendSoapRequest(baseUrl + SERVICE_PATH,
                        getResourceDataEnvelope(path), "urn:getResourceData", actor.getUserName(), actor.getPassword()));
    }

    /** A storage/authentication fault must never be interpreted as permission to create a missing collection. */
    private static boolean isMissingCollection(HttpResponse response, String path) throws IOException {
        if (response == null || response.getResponseCode() != 500 || response.getData() == null) {
            return false;
        }
        try {
            return Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='Fault']/*[local-name()='faultstring']")
                    .stream().anyMatch(fault -> fault.trim().equals("Resource does not exist at path " + path));
        } catch (org.jaxen.JaxenException | RuntimeException e) {
            throw new IOException("Could not parse registry collection fault for " + path, e);
        }
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
