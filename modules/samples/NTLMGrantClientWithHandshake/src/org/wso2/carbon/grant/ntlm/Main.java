package org.wso2.carbon.grant.ntlm;

import com.sun.jna.platform.win32.Sspi;
import waffle.apache.NegotiateAuthenticator;
import waffle.util.Base64;
import waffle.windows.auth.IWindowsCredentialsHandle;
import waffle.windows.auth.impl.WindowsAccountImpl;
import waffle.windows.auth.impl.WindowsCredentialsHandleImpl;
import waffle.windows.auth.impl.WindowsSecurityContextImpl;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.grant.ntlm.utils.CommandHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * NTLM sample Main class.This class will create a NTLM token and send to the wso2 api manager token end point and will
 * receive an access token.
 */
public class Main extends Thread{

    /**
     * Main method of the sample
     * 
     * @param args used to get consumer key, consumer secret, token endpoint url 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        System.out.println("Started NTLM grant client................");
        CommandHandler.setInputs(args);
        NegotiateAuthenticator authenticator = new NegotiateAuthenticator();

        String securityPackage = "Negotiate";
        WindowsSecurityContextImpl clientContext = null;

        IWindowsCredentialsHandle clientCredentials = null;
        // client credentials handle
        clientCredentials = WindowsCredentialsHandleImpl.getCurrent(securityPackage);
        clientCredentials.initialize();

        // initial client security context
        clientContext = new WindowsSecurityContextImpl();
        String username = WindowsAccountImpl.getCurrentUsername();
        System.out.println("username :" + username);

        clientContext.setPrincipalName(username);
        clientContext.setCredentialsHandle(clientCredentials.getHandle());
        clientContext.setSecurityPackage(securityPackage);
        clientContext.initialize(null, null, "localhost");

        String clientTokenType1 = Base64.encode(clientContext.getToken());
        System.out.println("Generated base64 encoded NTLM type 1 token:" + clientTokenType1);

        //sending the generated base64 encoded NTLM type 1 token to the token endpoint and receives base64 encoded the challenge from the server (NTLM token type 2)
        HttpResponse response = invokeNTLMTokenEndpoint(clientTokenType1);
        Header tokenHeader = response.getFirstHeader("WWW-Authenticate");
        String clientTokenType2 = tokenHeader.getValue().trim().split(" ")[1];
        System.out.println("Recieved base64 encoded NTLM type 2 token from server:" + clientTokenType2);

        //decodes the base64 encoded token and generates the NTLM type 3 token based on type 2 token 
        byte[] decodedToken2 = Base64.decode(clientTokenType2);
        Sspi.SecBufferDesc continueToken = new Sspi.SecBufferDesc(Sspi.SECBUFFER_TOKEN, decodedToken2);
        clientContext.initialize(clientContext.getHandle(), continueToken, "localhost");

        //encodes the NTLM type 3 token in base64
        byte[] clientTokenType3 = clientContext.getToken();
        String encodedToken3 = Base64.encode(clientTokenType3);
        System.out.println("Generated base64 encoded NTLM type 3 token:" + encodedToken3);

        //sends the NTLM type 3 token to the token endpoint
        response = invokeNTLMTokenEndpoint(encodedToken3);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        System.out.println(responseString);

        System.out.println("Completed NTLM grant client................");
        System.exit(0);
    }

    /**
     * Invokes the token endpoint with the NTLM token
     * 
     * @param clientToken base64 encoded NTLM token
     * @return response from the endpoint
     */
    private static HttpResponse invokeNTLMTokenEndpoint(String clientToken) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            byte[] userKey = (CommandHandler.getConsumerKey() + ":" + CommandHandler.getConsumerSecret()).getBytes();
            String encoding = Base64Utils.encode(userKey);
            HttpPost httppost = new HttpPost(CommandHandler.getTokenendpoint());
            httppost.setHeader("Authorization", "Basic " + encoding);
            List<NameValuePair> paramVals = new ArrayList<NameValuePair>();
            paramVals.add(new BasicNameValuePair("grant_type", "iwa:ntlm"));
            paramVals.add(new BasicNameValuePair("windows_token", clientToken));

            System.out.println("executing token API request " + httppost.getRequestLine());
            HttpResponse response = null;
            httppost.setEntity(new UrlEncodedFormEntity(paramVals, "UTF-8"));
            response = httpclient.execute(httppost);
            return response;

        } catch (IOException e) {
            System.out.println("Error has occoured while sending POST request to Gateway " + e.getMessage());
        }
        return null;
    }
}