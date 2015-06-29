package org.wso2.carbon.apimgt.frauddetection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Map;

/**
 * Common utils.
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);
    private static final String HTTP_HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HTTP_HEADER_MOCK_CLIENT_IP = "Fraud-Detection-Mock-Client-IP";

    public static String getClientIPAddress(MessageContext messageContext) {

        // Client IP should be retrieved based on the scenario. Following order should be followed.
        // 1) If the client IP is mocked, then there will be an HTTP header named 'FRAUD-DETECTION-MOCK-CLIENT-IP'
        // 2) API GW is fronted by a load balancer. Client IP should be retrieved from the 'X-Forwarded-For' header.
        // 3) Versioned API is called directly (No load balancer). Client IP should be retrieved from the 'REMOTE_ADDR' header

        String clientIPAddress = null;

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        Map<String, String> transportHeaders  = (Map) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        // Check whether the mock IP has been set.
        String mockClientIP = (String) transportHeaders.get(HTTP_HEADER_MOCK_CLIENT_IP);
        if (mockClientIP != null && !mockClientIP.isEmpty()) {

            clientIPAddress = mockClientIP;

            if(log.isDebugEnabled()){
                log.debug(String.format("Retrieved the client IP '%s' from the HTTP header '%s'", clientIPAddress, HTTP_HEADER_MOCK_CLIENT_IP));
            }
            return clientIPAddress;
        }

        // Check whether the request comes from the load balancer. If yes get the client IP from the 'X-Forwarded-For' header.
        String xForwardedForHeaderValue = (String) transportHeaders.get(HTTP_HEADER_X_FORWARDED_FOR);
        if (xForwardedForHeaderValue != null && !xForwardedForHeaderValue.isEmpty()) {

            clientIPAddress = xForwardedForHeaderValue.split(",")[0];

            if(log.isDebugEnabled()){
                log.debug(String.format("Retrieved the client IP '%s' from the HTTP header '%s'", clientIPAddress, HTTP_HEADER_X_FORWARDED_FOR));
            }

            return clientIPAddress;
        }

        // No special handling. Just get the address of the request sender.

        clientIPAddress = (String) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.REMOTE_ADDR);

        if(log.isDebugEnabled()){
            log.debug(String.format("Retrieved the client IP '%s' from the HTTP header '%s'", clientIPAddress, org.apache.axis2.context.MessageContext.REMOTE_ADDR));
        }

        return clientIPAddress;

    }


}
