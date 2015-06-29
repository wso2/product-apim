package org.wso2.carbon.apimgt.frauddetection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.TransportException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * This class publishes transaction data to WSO2 DAS.
 */
public class TransactionDataPublisher {

    private static final Log log = LogFactory.getLog(TransactionDataPublisher.class);

    private String dasHost;
    private String dasPort;
    private String dasUsername;
    private String dasPassword;

    public String streamName;
    public String streamVersion;

    private String streamId;
    private boolean ready;
    private boolean initialized;
    private DataPublisher dataPublisher;

    private static TransactionDataPublisher instance = null;


    protected TransactionDataPublisher() {
    }

    public static TransactionDataPublisher getInstance() {
        if (instance == null) {
            synchronized (TransactionDataPublisher.class) {
                if (instance == null) {
                    instance = new TransactionDataPublisher();
                }
            }
        }
        return instance;
    }


    protected void init(DataPublisherConfig config) {

        try {

            if(config == null){
                log.error(String.format("DataPublisherConfig is null. Cannot initialize TransactionDataPublisher"));
                return;
            }

            setDataPublisherConfig(config);
            dataPublisher = new DataPublisher(String.format("tcp://%s:%s", dasHost, dasPort), dasUsername, dasPassword);
            streamId = dataPublisher.findStreamId(streamName, streamVersion);

            if(streamId != null){
                this.ready = true;
                log.info("Transaction data publisher has been initialized.");
            }

        } catch (MalformedURLException e) {
            logInitializationError(e);
        } catch (AgentException e) {
            logInitializationError(e);
        } catch (AuthenticationException e) {
            logInitializationError(e);
        } catch (TransportException e) {
            logInitializationError(e);
        }

    }

    public void shutdown() {
        dataPublisher.stop();
        log.info("Transaction data publisher has been shutdown.");
    }

    public void publish(Object[] transactionStreamPayload) {

        if(isReady()){

            Event transactionEvent = new Event(streamId, System.currentTimeMillis(), null, null, transactionStreamPayload);
            try {
                dataPublisher.publish(transactionEvent);
                log.debug(String.format("Published event : %s", transactionEvent.toString()));
            } catch (AgentException e) {
               this.ready = false;
               log.error("Cannot publish transaction stream payload to DAS", e);
            }
        }else{
            log.error("Transaction data publisher has not been initialized properly. Cannot publish data");
        }

    }

    public boolean isReady() {
        return ready;
    }

    private void setDataPublisherConfig(DataPublisherConfig config){

        dasHost = config.getDasHost();
        dasPort = config.getDasPort();
        dasUsername = config.getDasUsername();
        dasPassword = config.getDasPassword();;

        streamName = config.getStreamName();
        streamVersion = config.getStreamVersion();

        log.debug(String.format("Fraud detection DAS properties => host : '%s', port : '%s', username : '%s', password : '%s', streamName : '%s', streamVersion : '%s'",
                                dasHost, dasPort, dasUsername, dasPassword.replaceAll(".", "x"), streamName, streamVersion));

    }


    private void logInitializationError(Exception e) {
        log.error("Cannot initialize TransactionDataPublisher.", e);
    }

   
}
