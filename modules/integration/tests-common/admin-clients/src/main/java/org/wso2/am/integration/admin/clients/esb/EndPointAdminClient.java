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
package org.wso2.am.integration.admin.clients.esb;

        import org.apache.axiom.om.OMElement;
        import org.apache.axiom.om.impl.builder.StAXOMBuilder;
        import org.apache.axis2.AxisFault;
        import org.apache.commons.logging.Log;
        import org.apache.commons.logging.LogFactory;
        import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
        import org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException;
        import org.wso2.carbon.endpoint.stub.types.EndpointAdminStub;
        import org.wso2.carbon.endpoint.stub.types.service.EndpointMetaData;

        import javax.activation.DataHandler;
        import javax.xml.stream.XMLInputFactory;
        import javax.xml.stream.XMLStreamException;
        import javax.xml.stream.XMLStreamReader;
        import java.io.IOException;
        import java.rmi.RemoteException;

public class EndPointAdminClient {
    private static final Log log = LogFactory.getLog(EndPointAdminClient.class);

    private final String serviceName = "EndpointAdmin";
    private EndpointAdminStub endpointAdminStub;

    public EndPointAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        endpointAdminStub = new EndpointAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, endpointAdminStub);
    }

    public EndPointAdminClient(String backEndURL, String userName, String password)
            throws AxisFault {
        String endPoint = backEndURL + serviceName;
        endpointAdminStub = new EndpointAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, endpointAdminStub);
    }

    public boolean addEndPoint(DataHandler dh)
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
        //create the builder
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement endPointElem = builder.getDocumentElement();
        return endpointAdminStub.addEndpoint(endPointElem.toString());
    }

    public boolean addEndPoint(OMElement endPointElem)
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        return endpointAdminStub.addEndpoint(endPointElem.toString());
    }

    public boolean addDynamicEndPoint(String key, DataHandler dh)
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
        //create the builder
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement endPointElem = builder.getDocumentElement();
        return endpointAdminStub.addDynamicEndpoint(key, endPointElem.toString());
    }

    public boolean addDynamicEndPoint(String key, OMElement endPointElem)
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        return endpointAdminStub.addDynamicEndpoint(key, endPointElem.toString());
    }

    public int getEndpointCount() throws EndpointAdminEndpointAdminException, RemoteException {
        return endpointAdminStub.getEndpointCount();
    }

    public String[] getEndpointNames() throws EndpointAdminEndpointAdminException, RemoteException {
        return endpointAdminStub.getEndPointsNames();
    }

    public int getDynamicEndpointCount()
            throws EndpointAdminEndpointAdminException, RemoteException {
        return endpointAdminStub.getDynamicEndpointCount();
    }

    public String getDynamicEndpoint(String key)
            throws EndpointAdminEndpointAdminException, RemoteException {
        return endpointAdminStub.getDynamicEndpoint(key);
    }

    public String[] getDynamicEndpoints()
            throws EndpointAdminEndpointAdminException, RemoteException {
        return endpointAdminStub.getDynamicEndpoints();
    }

    public void enableEndpointStatistics(String endpointName)
            throws RemoteException, EndpointAdminEndpointAdminException {
        endpointAdminStub.enableStatistics(endpointName);
        String endpoint = endpointAdminStub.getEndpointConfiguration(endpointName);
        assert (endpoint.contains("statistics=\"enable"));
    }

    public boolean deleteEndpoint(String endpointName)
            throws RemoteException, EndpointAdminEndpointAdminException {
        return endpointAdminStub.deleteEndpoint(endpointName);
    }

    public boolean deleteDynamicEndpoint(String key)
            throws RemoteException, EndpointAdminEndpointAdminException {
        return endpointAdminStub.deleteDynamicEndpoint(key);
    }

    public String getEndpointConfiguration(String endpointName)
            throws RemoteException, EndpointAdminEndpointAdminException {
        return endpointAdminStub.getEndpointConfiguration(endpointName);
    }

    public EndpointMetaData[] getEndpointsData()
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        return endpointAdminStub.getEndpointsData();
    }

}