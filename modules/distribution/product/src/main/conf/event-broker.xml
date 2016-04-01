<!--
this is the configuration file for the carbon event broker component. this configuration file configures the various subsystems of the event
broker compoent and parameters.
-->
<eventBrokerConfig xmlns="http://wso2.org/carbon/event/broker">
    <eventBroker name="carbonEventBroker" class = "org.wso2.carbon.event.core.internal.CarbonEventBrokerFactory">
         <!-- topic manager implemenation class.-->
        <topicManager name="TopicManager" class="org.wso2.carbon.event.core.internal.topic.registry.RegisistryTopicManagerFactory">
            <!-- root node of the topic tree -->
            <topicStoragePath>event/topics</topicStoragePath>
        </topicManager>
        <!-- subscriptionmnager implementaion. subscription manager persits the
        subscriptions at the registry.  users can configure the topics root node and the topicIndex path -->
        <subscriptionManager name="subscriptionManager"
                             class="org.wso2.carbon.event.core.internal.subscription.registry.RegistrySubscriptionManagerFactory">
            <topicStoragePath>event/topics</topicStoragePath>
            <indexStoragePath>event/topicIndex</indexStoragePath>
        </subscriptionManager>

        <!-- delivery manager inmplementation. delivary manager does actual delivary part of the event broker -->
        <deliveryManager name="deliveryManager"
                 class="org.wso2.carbon.event.core.internal.delivery.inmemory.InMemoryDeliveryManagerFactory">
            <minSpareThreads>25</minSpareThreads>
            <maxThreads>150</maxThreads>
            <maxQueuedRequests>100</maxQueuedRequests>
            <keepAliveTime>1000</keepAliveTime>
            <topicStoragePath>event/topics</topicStoragePath>
            <matchingManager name="matchingManager"
                     class="org.wso2.carbon.event.core.internal.delivery.inmemory.InMemoryMatchingManagerFactory"/>

        </deliveryManager>

        <!-- delivary manager inmplementation. delivary manager does actual delivary part of the event broker -->
<!--        <deliveryManager name="deliveryManager"
                         class="org.wso2.carbon.event.core.internal.delivery.jms.QpidJMSDeliveryManagerFactory"
                         type="local"> -->
           <!--  <remoteMessageBroker>
                <hostName>localhost</hostName>
                <servicePort>9443</servicePort>
                <webContext>/</webContext>
                <userName>admin</userName>
                <password>admin</password>
                <qpidPort>5672</qpidPort>
                <clientID>clientID</clientID>
                <virtualHostName>carbon</virtualHostName>
            </remoteMessageBroker> -->
    <!--    </deliveryManager> -->

         <!-- when publising an event event broker uses a seperate thread pool with an executor. following parameters configure different parameters of that -->
        <eventPublisher>
            <minSpareThreads>5</minSpareThreads>
            <maxThreads>50</maxThreads>
            <maxQueuedRequests>1000</maxQueuedRequests>
            <keepAliveTime>1000</keepAliveTime>
        </eventPublisher>
    </eventBroker>
</eventBrokerConfig>
