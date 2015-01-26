package org.wso2.carbon.bam.client;


import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.analytics.hive.stub.HiveScriptStoreServiceHiveScriptStoreException;
import org.wso2.carbon.analytics.hive.stub.HiveScriptStoreServiceStub;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.utils.FileUtil;

import java.io.IOException;

public class Client {

    private static String HIVE_SCRIPT_STORE_SERVICE = "HiveScriptStoreService";

    private static HiveScriptStoreServiceStub hiveScriptStoreServiceStub;


    public static void main(String[] args)
            throws IOException,
                   LoginAuthenticationExceptionException {

        String trustStore = System.getProperty("carbon.home") + "/repository/resources/security";
        System.setProperty("javax.net.ssl.trustStore", trustStore + "/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");


        String authenticationServiceURL = getProperty("bamUrl") + "AuthenticationAdmin";
        AuthenticationAdminStub authenticationAdminStub = new AuthenticationAdminStub(authenticationServiceURL);
        ServiceClient client = authenticationAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);

        authenticationAdminStub.login("admin", "admin", "localhost");

        ServiceContext serviceContext = authenticationAdminStub.
                _getServiceClient().getLastOperationContext().getServiceContext();
        String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);

        String hiveScriptStoreServiceURL = getProperty("bamUrl") + HIVE_SCRIPT_STORE_SERVICE;
        hiveScriptStoreServiceStub = new HiveScriptStoreServiceStub(hiveScriptStoreServiceURL);

        ServiceClient hiveScriptStoreServiceStubClient =  hiveScriptStoreServiceStub._getServiceClient();
        Options  hiveScriptStoreServiceStubOption = hiveScriptStoreServiceStubClient.getOptions();
        hiveScriptStoreServiceStubOption.setManageSession(true);
        hiveScriptStoreServiceStubOption.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);

        String apiVersionUsageSummaryContent = FileUtil.readFileToString(System.getProperty("configFilePath") +
                                                                         "APIVersionUsageSummaryScript.hiveql");
        String apiVersionKeyUsageSummaryContent = FileUtil.readFileToString( System.getProperty("configFilePath") +
                                                                             "APIVersionKeyUsageSummaryScript.hiveql");
        String apiVersionKeyLastAccessSummaryContent = FileUtil.readFileToString( System.getProperty("configFilePath") +
                                                                             "APIVersionKeyLastAccessSummaryScript.hiveql");
        String apiVersionServiceTimeSummaryContent = FileUtil.readFileToString( System.getProperty("configFilePath") +
                                                                             "APIVersionServiceTimeSummaryScript.hiveql");
        String keyUsageSummaryContent = FileUtil.readFileToString( System.getProperty("configFilePath") +
                                                                             "KeyUsageSummaryScript.hiveql");



        try {

            hiveScriptStoreServiceStub.saveHiveScript("APIVersionUsageSummaryScript",apiVersionUsageSummaryContent,"1 * * * * ? *");
            hiveScriptStoreServiceStub.saveHiveScript("APIVersionKeyUsageSummaryScript",apiVersionKeyUsageSummaryContent,"1 * * * * ? *");
            hiveScriptStoreServiceStub.saveHiveScript("APIVersionKeyLastAccessSummaryScript",apiVersionKeyLastAccessSummaryContent,"1 * * * * ? *");
            hiveScriptStoreServiceStub.saveHiveScript("APIVersionServiceTimeSummaryScript",apiVersionServiceTimeSummaryContent,"1 * * * * ? *");
            hiveScriptStoreServiceStub.saveHiveScript("KeyUsageSummaryScript",keyUsageSummaryContent,"1 * * * * ? *");
	    System.out.println("BAM configured successfully for collecting API stats");
            return;

        }  catch (HiveScriptStoreServiceHiveScriptStoreException e) {
            e.printStackTrace();
        }


    }

    private static String getProperty(String bamUrl) {
        String defaultVal = "https://localhost:9443/services/";
        String result = System.getProperty(bamUrl);
        if (result == null || result.length() == 0) {
            result = defaultVal;
        }
        return result;
    }
}
