/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.admin.clients.mediation;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.databinding.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.client.stub.generated.*;
import org.wso2.carbon.event.client.stub.generated.addressing.AttributedURI;
import org.wso2.carbon.event.client.stub.generated.addressing.EndpointReferenceType;
import org.wso2.carbon.event.client.stub.generated.addressing.ReferenceParametersType;

import javax.xml.namespace.QName;
import java.rmi.RemoteException;

public class EventBrokerAdminClient {

	private static final Log log = LogFactory.getLog(EventBrokerAdminClient.class);

	String backendUrl = null;
	String SessionCookie = null;
	ConfigurationContext configurationContext = null;
	private static final String TOPIC_HEADER_NAME = "topic";

	private static final String TOPIC_HEADER_NS = "http://wso2.org/ns/2009/09/eventing/notify";

	public static final String WSE_EVENTING_NS = "http://schemas.xmlsoap.org/ws/2004/08/eventing";
	public static final String WSE_EN_IDENTIFIER = "Identifier";

	private static OMFactory omFactory = OMAbstractFactory.getOMFactory();

	public EventBrokerAdminClient(String backendUrl, String sessionCookie,
	                              ConfigurationContext configurationContext) {

		this.backendUrl = backendUrl + "EventBrokerService";
		this.SessionCookie = sessionCookie;
		this.configurationContext = configurationContext;

	}

	public String subscribe(String topic, String eventSinkUrl) throws RemoteException {
		log.debug("Subscribed to " + topic + " in " + eventSinkUrl);

		try {
			// append the topic name at the end of the broker URL
			// so that it seems there is a seperate uri each event source
			if (!topic.startsWith("/")) {
				topic = "/" + topic;
			}
			EventBrokerServiceStub stub = new EventBrokerServiceStub(configurationContext,
			                                                         backendUrl + topic);

			ServiceClient client = stub._getServiceClient();
			configureCookie(client);

			EndpointReferenceType epr = new EndpointReferenceType();
			epr.setAddress(createURI(eventSinkUrl));

			DeliveryType deliveryType = new DeliveryType();
			EndpointReferenceType eventSink = new EndpointReferenceType();
			eventSink.setAddress(createURI(eventSinkUrl));
			deliveryType.setNotifyTo(eventSink);

			ExpirationType expirationType = null;

			FilterType filterType = new FilterType();
			filterType.setDialect(new URI("urn:someurl"));
			filterType.setString(topic);

			SubscribeResponse subscribeResponse = stub.subscribe(epr, deliveryType, expirationType,
			                                                     filterType, null);
			ReferenceParametersType referenceParameters =
					subscribeResponse.getSubscriptionManager().getReferenceParameters();
			OMElement[] properties = referenceParameters.getExtraElement();

			String id = null;
			for (OMElement property : properties) {
				if (property.getLocalName().equals("Identifier")) {
					id = property.getText();
				}
			}
			return id;
		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (URI.MalformedURIException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void publish(String topic, OMElement element) throws AxisFault {
		log.debug("published element to " + topic);
		EventBrokerServiceStub service = new EventBrokerServiceStub(configurationContext, backendUrl
		                                                                                  +
		                                                                                  "/publish/" +
		                                                                                  topic);
		configureCookie(service._getServiceClient());
		ServiceClient serviceClient = service._getServiceClient();

		OMElement header = omFactory.createOMElement(new QName(TOPIC_HEADER_NS, TOPIC_HEADER_NAME));
		header.setText(topic);
		serviceClient.addHeader(header);
		serviceClient.getOptions().setTo(new EndpointReference(backendUrl + "/publish"));
		//serviceClient.getOptions().setTo(new EndpointReference(brokerUrl));
		serviceClient.getOptions().setAction("urn:publish");
		serviceClient.sendRobust(element);
	}

	public void unsubscribe(String subscriptionID) throws RemoteException {
		log.debug("Unsubscribed to " + subscriptionID);
		EventBrokerServiceStub service =
				new EventBrokerServiceStub(configurationContext, backendUrl);
		configureCookie(service._getServiceClient());
		ServiceClient serviceClient = service._getServiceClient();
		OMElement header = omFactory.createOMElement(new QName(WSE_EVENTING_NS, WSE_EN_IDENTIFIER));
		header.setText(subscriptionID);
		serviceClient.addHeader(header);
		service.unsubscribe(new OMElement[] { });
	}

	public GetSubscriptionsResponse getAllSubscriptions(int maxRequestCount, String resultFilter,
	                                                    int firstIndex) throws RemoteException {
		EventBrokerServiceStub service =
				new EventBrokerServiceStub(configurationContext, backendUrl);
		configureCookie(service._getServiceClient());
		return service.getSubscriptions(maxRequestCount, resultFilter, firstIndex);
	}

	private void configureCookie(ServiceClient client) throws AxisFault {
		if (SessionCookie != null) {
			Options option = client.getOptions();
			option.setManageSession(true);
			option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
			                   SessionCookie);
		}
	}

	private static AttributedURI createURI(String uriAddress) throws URI.MalformedURIException {
		AttributedURI address = new AttributedURI();
		address.setAnyURI(new URI(uriAddress));
		return address;
	}
}
