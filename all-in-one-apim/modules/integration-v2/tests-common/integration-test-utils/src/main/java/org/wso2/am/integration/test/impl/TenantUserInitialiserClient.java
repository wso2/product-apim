package org.wso2.am.integration.test.impl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class TenantUserInitialiserClient {
    private static final Logger logger = LoggerFactory.getLogger(TenantUserInitialiserClient.class);
    private String BASE_URL;
    private String USERNAME;
    private String PASSWORD;

    public TenantUserInitialiserClient(String username, String password, String port){
        this.USERNAME=username;
        this.PASSWORD=password;
        this.BASE_URL = "https://localhost:"+port+"//services/";

    }

    public void addTenant(String firstName,String lastName, String adminUserName, String adminPassword, String tenantDomain, String email) throws IOException {
        String url = BASE_URL + "TenantMgtAdminService";
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\" " +
                "xmlns:xsd=\"http://beans.common.stratos.carbon.wso2.org/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<ser:addTenant>" +
                "<ser:tenantInfoBean>" +
                "<xsd:active>true</xsd:active>" +
                "<xsd:admin>" + adminUserName + "</xsd:admin>" +
                "<xsd:adminPassword>" + adminPassword + "</xsd:adminPassword>" +
                "<xsd:email>" + email + "</xsd:email>" +
                "<xsd:firstname>" + firstName + "</xsd:firstname>" +
                "<xsd:lastname>" + lastName + "</xsd:lastname>" +
                "<xsd:tenantDomain>" + tenantDomain + "</xsd:tenantDomain>" +
                "</ser:tenantInfoBean>" +
                "</ser:addTenant>" +
                "</soapenv:Body></soapenv:Envelope>";

        sendSoapRequest(url, payload, "urn:addTenant");
    }

    public void addRole(String roleName) throws IOException {
        String url = BASE_URL + "UserAdmin";
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:addRole>" +
                "<xsd:roleName>" + roleName + "</xsd:roleName>" +
                "<xsd:isSharedRole>false</xsd:isSharedRole>" +
                "</xsd:addRole>" +
                "</soapenv:Body></soapenv:Envelope>";

        sendSoapRequest(url, payload, "urn:addRole");
    }

    public void addUserWithRoles(String userName, String password, String... roles) throws IOException {
        String url = BASE_URL + "UserAdmin";
        StringBuilder rolesXml = new StringBuilder();
        for (String role : roles) {
            rolesXml.append("<xsd:roles>").append(role).append("</xsd:roles>");
        }

        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:addUser>" +
                "<xsd:userName>" + userName + "</xsd:userName>" +
                "<xsd:password>" + password + "</xsd:password>" +
                rolesXml +
                "</xsd:addUser>" +
                "</soapenv:Body></soapenv:Envelope>";

//        System.out.print(payload);
        sendSoapRequest(url, payload, "urn:addUser");
    }

    private void sendSoapRequest(String url, String payload, String soapAction) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "text/xml; charset=UTF-8");
            post.setHeader("SOAPAction", "\"" + soapAction + "\"");
            String auth = this.USERNAME + ":" + this.PASSWORD;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            post.setHeader("Authorization", "Basic " + encodedAuth);
            post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
//                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                System.out.println("Response: " + response.getStatusLine());
                if(Objects.equals(soapAction, "urn:addTenant")){
                    System.out.println("Tenant");
                }
                if(Objects.equals(soapAction, "urn:addRole")){
                    System.out.println("Role");
                }
                if (Objects.equals(soapAction, "urn:addUser")){
                    System.out.println("User");
                }
            }
        }
    }
}
