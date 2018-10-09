package org.wso2.am.integration.test.utils.bean;


public class ClientCertificateCreationBean extends AbstractRequest {
    private String apiName;
    private String apiProviderName;
    private String apiVersionName;
    private String certificate;
    private String tierName;
    private String alias;

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
