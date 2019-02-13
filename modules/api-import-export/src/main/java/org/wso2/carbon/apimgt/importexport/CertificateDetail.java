/*
 *
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package org.wso2.carbon.apimgt.importexport;

/**
 * Class to hold certificate information detail hostname/alias/certificate which will write to a json file
 *
 */
public class CertificateDetail {
    private String hostName;
    private String alias;
    private String certificate;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getAlias() {
        return alias;
    }

    public String getCertificate() {
        return certificate;
    }
}
