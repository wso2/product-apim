/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.test.utils.bean;

/**
 * This is request bean for uploading client certificate.
 */
public class ClientCertificateCreationBean extends AbstractRequest {
    private String apiName;
    private String apiProviderName;
    private String apiVersionName;
    private String certificate;
    private String tierName;
    private String alias;

    /**
     * Constructs an instance of client certificate creation.
     *
     * @param apiName         Name of the API.
     * @param apiProviderName Name of the API Provider
     * @param apiVersionName  Version of the API
     * @param certificate     Relevant Base64 encoded certificate.
     * @param tierName        Name of the tier.
     * @param alias           Alias.
     */
    public ClientCertificateCreationBean(String apiName, String apiProviderName, String apiVersionName,
            String certificate, String tierName, String alias) {
        this.apiName = apiName;
        this.apiProviderName = apiProviderName;
        this.apiVersionName = apiVersionName;
        this.certificate = certificate;
        this.tierName = tierName;
        this.alias = alias;
    }

    @Override
    public void setAction() {
        this.action = "addClientCertificate";
    }

    @Override
    public void init() {
        this.addParameter("alias", alias);
        this.addParameter("certificate", certificate);
        this.addParameter("name", apiName);
        this.addParameter("version", apiVersionName);
        this.addParameter("provider", apiProviderName);
        this.addParameter("tierName", tierName);

    }
}
