/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
*
*/
package org.wso2.am.integration.test.utils.thrift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.DataBridge;
import org.wso2.carbon.databridge.core.Utils.AgentSession;
import org.wso2.carbon.databridge.core.definitionstore.InMemoryStreamDefinitionStore;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.core.internal.authentication.AuthenticationHandler;
import org.wso2.carbon.databridge.receiver.thrift.ThriftDataReceiver;
import org.wso2.carbon.user.api.UserStoreException;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to start a test thrift server to simulate DAS event receiver. It used to test APIM stat publishing
 * to DAS
 */
public class DASThriftTestServer {
    private Log log = LogFactory.getLog(DASThriftTestServer.class);
    private ThriftDataReceiver thriftDataReceiver;
    private InMemoryStreamDefinitionStore streamDefinitionStore;
    private AtomicInteger numberOfEventsReceived;
    private ReStarterThread reStarterThread;
    private Map<String, List<Event>> dataTables = new HashMap<String, List<Event>>();
    private int tenantId;
    private String tenantDomain;

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public void clearTables() {
        dataTables.clear();
    }

    public Map<String, List<Event>> getDataTables() {
        return dataTables;
    }

    public void resetTables() {
        this.dataTables.clear();
    }

    public void addStreamDefinition(StreamDefinition streamDefinition, int tenantId)
            throws StreamDefinitionStoreException {
        streamDefinitionStore.saveStreamDefinitionToStore(streamDefinition, tenantId);
    }

    public void addStreamDefinition(String streamDefinitionStr, int tenantId)
            throws StreamDefinitionStoreException, MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = EventDefinitionConverterUtils.convertFromJson(streamDefinitionStr);
        getStreamDefinitionStore().saveStreamDefinitionToStore(streamDefinition, tenantId);
    }

    private InMemoryStreamDefinitionStore getStreamDefinitionStore() {
        if (streamDefinitionStore == null) {
            streamDefinitionStore = new InMemoryStreamDefinitionStore();
        }
        return streamDefinitionStore;
    }

    public void start(int receiverPort) throws DataBridgeException {
        DataPublisherTestUtil.setKeyStoreParams();
        streamDefinitionStore = getStreamDefinitionStore();
        numberOfEventsReceived = new AtomicInteger(0);
        DataBridge databridge = new DataBridge(new AuthenticationHandler() {
            public boolean authenticate(String userName, String password) {
                return true;// allays authenticate to true
            }

            public String getTenantDomain(String userName) {
                return tenantDomain;
            }

            public int getTenantId(String tenantDomain) throws UserStoreException {
                return tenantId;
            }

            public void initContext(AgentSession agentSession) {
                // To change body of implemented methods use File | Settings |
                // File Templates.
            }

            public void destroyContext(AgentSession agentSession) {

            }
        }, streamDefinitionStore, DataPublisherTestUtil.getDataBridgeConfigPath());

        thriftDataReceiver = new ThriftDataReceiver(receiverPort, databridge);

        databridge.subscribe(new AgentCallback() {
            int totalSize = 0;

            public void definedStream(StreamDefinition streamDefinition, int tenantId) {
                // log.info("StreamDefinition " + streamDefinition);
            }

            public void removeStream(StreamDefinition streamDefinition, int tenantId) {
                log.info("StreamDefinition remove " + streamDefinition);
            }

            /**
             * Retrving and handling all the events to DAS event receiver
             * @param eventList list of event it received
             * @param credentials client credentials
             */
            public void receive(List<Event> eventList, Credentials credentials) {
                for (Event event : eventList) {
                    String streamKey = event.getStreamId();
                    if (!dataTables.containsKey(streamKey)) {
                        dataTables.put(streamKey, new ArrayList<Event>());
                    }
                    dataTables.get(streamKey).add(event);
                }
                numberOfEventsReceived.addAndGet(eventList.size());
                log.info("Received events : " + numberOfEventsReceived);
            }
        });

        String address = "localhost";
        log.info("DAS Test Thrift Server starting on " + address);
        thriftDataReceiver.start(address);
        log.info("DAS Test Thrift Server Started");
    }

    public int getNumberOfEventsReceived() {
        if (numberOfEventsReceived != null)
            return numberOfEventsReceived.get();
        else
            return 0;
    }

    public void resetReceivedEvents() {
        numberOfEventsReceived.set(0);
    }

    public void stop() {
        thriftDataReceiver.stop();
        log.info("DAS Test Thrift Server Stopped");
    }

    public void stopAndStartDuration(int port, long stopAfterTimeMilliSeconds, long startAfterTimeMS)
            throws SocketException, DataBridgeException {
        reStarterThread = new ReStarterThread(port, stopAfterTimeMilliSeconds, startAfterTimeMS);
        Thread thread = new Thread(reStarterThread);
        thread.start();
    }

    public int getEventsReceivedBeforeLastRestart() {
        return reStarterThread.eventReceived;
    }

    class ReStarterThread implements Runnable {
        int eventReceived;
        int port;

        long stopAfterTimeMilliSeconds;
        long startAfterTimeMS;

        ReStarterThread(int port, long stopAfterTime, long startAfterTime) {
            this.port = port;
            stopAfterTimeMilliSeconds = stopAfterTime;
            startAfterTimeMS = startAfterTime;
        }

        public void run() {
            try {
                Thread.sleep(stopAfterTimeMilliSeconds);
            } catch (InterruptedException e) {
            }
            if (thriftDataReceiver != null)
                thriftDataReceiver.stop();

            eventReceived = getNumberOfEventsReceived();

            log.info("Number of events received in server shutdown :" + eventReceived);
            try {
                Thread.sleep(startAfterTimeMS);
            } catch (InterruptedException e) {
            }

            try {
                if (thriftDataReceiver != null) {
                    thriftDataReceiver.start(DataPublisherTestUtil.LOCAL_HOST);
                } else {
                    start(port);
                }
            } catch (DataBridgeException e) {
                log.error(e);
            }

        }
    }

}
