package org.wso2.carbon.apimgt.frauddetection;

import com.google.gson.Gson;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * The mediator which publisher transaction data to WSO2 DAS.
 */
public class TransactionDataPublishingMediator extends AbstractMediator{

    private String dasHost;
    private String dasPort;
    private String dasUsername;
    private String dasPassword;

    public String streamName;
    public String streamVersion;

    public boolean mediate(MessageContext messageContext) {

        TransactionDataPublisher publisher = getTransactionDataPublisher();
        publishTransactionData(publisher, messageContext);
        return true;
    }

    private void publishTransactionData(TransactionDataPublisher publisher, MessageContext messageContext) {

        OMElement transactionInfoPayload = getTransactionInfoPayload(messageContext);

        if(transactionInfoPayload != null){
            Object[] transactionStreamPayload = buildTransactionStreamPayload(transactionInfoPayload, messageContext);

            if(transactionStreamPayload != null){
                log.debug(String.format("transaction stream payload => %s", Arrays.toString(transactionStreamPayload)));
                publisher.publish((transactionStreamPayload));
            }else{
                log.error("Cannot build the transaction payload. Therefore data is not published to DAS.");
            }

        }


    }

    public String getDasHost() {
        return dasHost;
    }

    public void setDasHost(String dasHost) {
        this.dasHost = dasHost;
    }

    public String getDasPort() {
        return dasPort;
    }

    public void setDasPort(String dasPort) {
        this.dasPort = dasPort;
    }

    public String getDasUsername() {
        return dasUsername;
    }

    public void setDasUsername(String dasUsername) {
        this.dasUsername = dasUsername;
    }

    public String getDasPassword() {
        return dasPassword;
    }

    public void setDasPassword(String dasPassword) {
        this.dasPassword = dasPassword;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getStreamVersion() {
        return streamVersion;
    }

    public void setStreamVersion(String streamVersion) {
        this.streamVersion = streamVersion;
    }

    private Object[] buildTransactionStreamPayload(OMElement transactionInfoPayload, MessageContext messageContext) {

        // Extract credit card info
        OMElement creditCardInfo = transactionInfoPayload.getFirstChildWithName(new QName(null, "payer")).
                getFirstChildWithName(new QName(null, "funding_instruments")).
                getFirstChildWithName(new QName("credit_card"));

        // Extract shipping info
        OMElement shippingInfo = transactionInfoPayload.getFirstChildWithName(new QName(null, "shipment")).getFirstChildWithName(new QName("shipping_address"));

        // Extract transaction info
        OMElement transactionAmountInfo = transactionInfoPayload.getFirstChildWithName(new QName(null, "transactions")).getFirstChildWithName(new QName("amount"));

        // Extract order info
        OMElement orderInfo = transactionInfoPayload.getFirstChildWithName(new QName(null, "transactions")).getFirstChildWithName(new QName("order"));

        String transactionId = transactionInfoPayload.getFirstChildWithName(new QName(null, "id")).getText();
        long creditCardNumber = Long.parseLong(creditCardInfo.getFirstChildWithName(new QName(null, "number")).getText());
        double transactionAmount = Double.parseDouble(transactionAmountInfo.getFirstChildWithName(new QName(null, "total")).getText());
        String currency = transactionAmountInfo.getFirstChildWithName(new QName(null, "currency")).getText();
        String email = transactionInfoPayload.getFirstChildWithName(new QName(null, "payer")).getFirstChildWithName(new QName(null,"email")).getText();
        String shippingAddress = getShippingAddress(shippingInfo);
        String billingAddress = getBillingAddress(creditCardInfo);
        String ip = getClientIPAddress(messageContext);
        String itemNo = orderInfo.getFirstChildWithName(new QName(null,"item_number")).getText();
        int quantity = Integer.parseInt(orderInfo.getFirstChildWithName(new QName(null, "quantity")).getText());
        long timestamp = System.currentTimeMillis();
        float ipLatitude = 0.0f;
        float ipLongitude = 0.0f;
        float addressLatitude = 0.0f;
        float addressLongitude = 0.0f;

        Map<String, Float> ipCoordinates = getIPCoordinates(ip);

        if(ipCoordinates != null){
            ipLatitude = ipCoordinates.get("lat");
            ipLongitude = ipCoordinates.get("lng");
        }else{
            return null;
        }

        Map<String, Double> addressCoordinates = getAddressCoordinates(shippingAddress);

        if(addressCoordinates != null){
            addressLatitude = addressCoordinates.get("lat").floatValue();
            addressLongitude = addressCoordinates.get("lng").floatValue();
        }else{
            return null;
        }

        return new Object[]{transactionId, creditCardNumber, transactionAmount, currency, email, shippingAddress, billingAddress, ip, itemNo, quantity, timestamp,
                                ipLatitude, ipLongitude, addressLatitude, addressLongitude};

    }

    private Map<String, Double> getAddressCoordinates(String shippingAddress) {

        HttpClient httpclient = new DefaultHttpClient();
        String urlEncodedAddress = null;
        try {
            urlEncodedAddress = URLEncoder.encode(shippingAddress, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpget = new HttpGet(String.format("https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=AIzaSyAE5HNqYSWEsBuskseqJDRtgIyg3603aQw", urlEncodedAddress));

        HttpResponse response = null;
        InputStream responseContentStream = null;
        try {
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            responseContentStream = entity.getContent();

            String responseString = IOUtils.toString(responseContentStream);

            Gson gson = new Gson();

            Map<String, Object> parsedResponse = gson.fromJson(responseString, HashMap.class);

            Map<String, Double> coordinates = (Map<String, Double>) ((Map)((Map)((List) parsedResponse.get("results")).get(0)).get("geometry")).get("location");

            return coordinates;

        } catch (Exception e) {
            log.error("Cannot get shipping address coordinates.", e);
        }finally {
            try {
                if (responseContentStream != null){
                    responseContentStream.close();
                }
            } catch (IOException e) {
                log.error("Cannot the input stream of the http response.", e);
            }
        }

        return null;


    }

    private Map<String, Float> getIPCoordinates(String ip) {

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(String.format("http://ipinfo.io/%s/json", ip));
        HttpResponse response = null;
        InputStream responseContentStream = null;
        try {
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            responseContentStream = entity.getContent();

            String responseString = IOUtils.toString(responseContentStream);

            Gson gson = new Gson();
            Map<String, Object> parsedResponse = gson.fromJson(responseString, HashMap.class);

            Map<String, Float> coordinates = new HashMap<String, Float>();

            String coordinatesString = (String) parsedResponse.get("loc");

            coordinates.put("lat", Float.parseFloat(coordinatesString.split(",")[0]));
            coordinates.put("lng", Float.parseFloat(coordinatesString.split(",")[1]));

            return coordinates;

        } catch (Exception e) {
            log.error("Cannot get IP coordinates.", e);
        }finally {
            try {
                if (responseContentStream != null){
                    responseContentStream.close();
                }
            } catch (IOException e) {
                log.error("Cannot the input stream of the http response.", e);
            }
        }

        return null;
    }

    private String getClientIPAddress(MessageContext messageContext) {
        return Util.getClientIPAddress(messageContext);
    }

    private String getBillingAddress(OMElement creditCardInfo) {
        OMElement billingAddressInfo = creditCardInfo.getFirstChildWithName(new QName(null, "billing_address"));
        return String.format("%s, %s, %s, %s, %s",
                billingAddressInfo.getFirstChildWithName(new QName(null, "line1")).getText(),
                billingAddressInfo.getFirstChildWithName(new QName(null, "city")).getText(),
                billingAddressInfo.getFirstChildWithName(new QName(null, "state")).getText(),
                billingAddressInfo.getFirstChildWithName(new QName(null, "postal_code")).getText(),
                billingAddressInfo.getFirstChildWithName(new QName(null, "country_code")).getText());
    }

    private String getShippingAddress(OMElement shippingInfo) {
        return String.format("%s, %s, %s, %s, %s",
                shippingInfo.getFirstChildWithName(new QName(null, "line1")).getText(),
                shippingInfo.getFirstChildWithName(new QName(null, "city")).getText(),
                shippingInfo.getFirstChildWithName(new QName(null, "state")).getText(),
                shippingInfo.getFirstChildWithName(new QName(null, "postal_code")).getText(),
                shippingInfo.getFirstChildWithName(new QName(null, "country_code")).getText());
    }

    private OMElement getTransactionInfoPayload(MessageContext messageContext) {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();

        try {
            RelayUtils.buildMessage(axis2MessageContext);
        } catch (IOException e) {
            logDataPublishingException("Cannot build the incoming request message", e);
            return null;
        } catch (XMLStreamException e) {
            logDataPublishingException("Cannot build the incoming request message", e);
            return null;
        }

        Iterator iterator = messageContext.getEnvelope().getBody().getChildElements();

        OMElement payload = null;
        if(iterator.hasNext()){
            payload = (OMElement) iterator.next();
        }

        return payload;
    }

    private TransactionDataPublisher getTransactionDataPublisher() {

        TransactionDataPublisher publisher = TransactionDataPublisher.getInstance();

        if(!publisher.isReady()){

            synchronized (publisher){
                if(!publisher.isReady()){
                    DataPublisherConfig config = getDataPublisherConfig();
                    publisher.init(config);
                }
            }

        }

        return publisher;
    }

    private DataPublisherConfig getDataPublisherConfig() {

        DataPublisherConfig config = new DataPublisherConfig();

        config.setDasHost(dasHost);
        config.setDasPort(dasPort);
        config.setDasUsername(dasUsername);
        config.setDasPassword(dasPassword);
        config.setStreamName(streamName);
        config.setStreamVersion(streamVersion);

        return config;
    }

    private void logDataPublishingException(String reason, Exception e) {
        log.error(String.format("Cannot publish transaction data. Reason : %s", reason), e);
    }

}
