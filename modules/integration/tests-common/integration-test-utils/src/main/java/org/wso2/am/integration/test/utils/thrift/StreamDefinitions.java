package org.wso2.am.integration.test.utils.thrift;

/**
 * this class is hold list of event stream definitions that used to published to DAS for APIM analytics
 */
public class StreamDefinitions {
    public static final String APIMGT_STATISTICS_RESPONSE_STREAM_ID = "org.wso2.apimgt.statistics.response:1.1.0";
    public static final String APIMGT_STATISTICS_REQUEST_STREAM_ID = "org.wso2.apimgt.statistics.request:1.1.0";
    public static final String APIMGT_STATISTICS_EXECUTION_TIME_STREAM_ID = "org.wso2.apimgt.statistics.execution.time:1.0.0";
    public static final String APIMGT_STATISTICS_WORKFLOW_STREAM_ID = "org.wso2.apimgt.statistics.workflow:1.0.0";

    public static String getStreamDefinitionResponse() {
        return "{" +
                "  'name':'" +
                "org.wso2.apimgt.statistics.response" + "'," +
                "  'version':'" +
                "1.1.0" + "'," +
                "  'nickName': 'API Manager Response Data'," +
                "  'description': 'Response Data'," +
                "  'metaData':[" +
                "          {'name':'clientType','type':'STRING'}" +
                "  ]," +
                "  'payloadData':[" +
                "          {'name':'consumerKey','type':'STRING'}," +
                "          {'name':'context','type':'STRING'}," +
                "          {'name':'api_version','type':'STRING'}," +
                "          {'name':'api','type':'STRING'}," +
                "          {'name':'resourcePath','type':'STRING'}," +
                "          {'name':'resourceTemplate','type':'STRING'}," +
                "          {'name':'method','type':'STRING'}," +
                "          {'name':'version','type':'STRING'}," +
                "          {'name':'response','type':'INT'}," +
                "          {'name':'responseTime','type':'LONG'}," +
                "          {'name':'serviceTime','type':'LONG'}," +
                "          {'name':'backendTime','type':'LONG'}," +
                "          {'name':'username','type':'STRING'}," +
                "          {'name':'eventTime','type':'LONG'}," +
                "          {'name':'tenantDomain','type':'STRING'}," +
                "          {'name':'hostName','type':'STRING'}," +
                "          {'name':'apiPublisher','type':'STRING'}," +
                "          {'name':'applicationName','type':'STRING'}," +
                "          {'name':'applicationId','type':'STRING'}," +
                "          {'name':'cacheHit','type':'BOOL'}," +
                "          {'name':'responseSize','type':'LONG'}," +
                "          {'name':'protocol','type':'STRING'}," +
                "          {'name':'responseCode','type':'INT'}" +
                "  ]" +
                "}";
    }

    public static String getStreamDefinitionRequest() {
        /*
          Please use this comment to track the steam changes that were done.
          Current Version -
            1.1.0
          Changes -
            1.1.0 -  Added the resourceTemplate parameter.
         */
        return "{" +
                "  'name':'" +
                "org.wso2.apimgt.statistics.request" + "'," +
                "  'version':'" +
                "1.1.0" + "'," +
                "  'nickName': 'API Manager Request Data'," +
                "  'description': 'Request Data'," +
                "  'metaData':[" +
                "          {'name':'clientType','type':'STRING'}" +
                "  ]," +
                "  'payloadData':[" +
                "          {'name':'consumerKey','type':'STRING'}," +
                "          {'name':'context','type':'STRING'}," +
                "          {'name':'api_version','type':'STRING'}," +
                "          {'name':'api','type':'STRING'}," +
                "          {'name':'resourcePath','type':'STRING'}," +
                "          {'name':'resourceTemplate','type':'STRING'}," +
                "          {'name':'method','type':'STRING'}," +
                "          {'name':'version','type':'STRING'}," +
                "          {'name':'request','type':'INT'}," +
                "          {'name':'requestTime','type':'LONG'}," +
                "          {'name':'userId','type':'STRING'}," +
                "          {'name':'tenantDomain','type':'STRING'}," +
                "          {'name':'hostName','type':'STRING'}," +
                "          {'name':'apiPublisher','type':'STRING'}," +
                "          {'name':'applicationName','type':'STRING'}," +
                "          {'name':'applicationId','type':'STRING'}," +
                "          {'name':'userAgent','type':'STRING'}," +
                "          {'name':'tier','type':'STRING'}," +
                "          {'name':'throttledOut','type':'BOOL'}," +
                "          {'name':'clientIp','type':'STRING'}" +
                "  ]" +
                "}";
    }

    public static String getStreamDefinitionExecutionTime() {
        return "{" +
                "  'name': '" + "org.wso2.apimgt.statistics.execution.time" + "'," +
                "  'version': '" + "1.0.0" + "'," +
                "  'nickName': 'Execution Time Data'," +
                "  'description': 'This stream will persist the data which send by the mediation executions'," +
                "  'metaData': [" +
                "    {" +
                "      'name': 'clientType'," +
                "      'type': 'STRING'" +
                "    }" +
                "  ]," +
                "  'payloadData': [" +
                "    {" +
                "      'name': 'api'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'api_version'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'tenantDomain'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'apiPublisher'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'mediationName'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'executionTime'," +
                "      'type': 'LONG'" +
                "    }," +
                "    {" +
                "      'name': 'context'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'eventTime'," +
                "      'type': 'LONG'" +
                "    }" +
                "  ]" +
                "}";
    }

    public static String getStreamDefinitionWorkflow() {
        return "{" +
                "  'name': 'org.wso2.apimgt.statistics.workflow'," +
                "  'version': '1.0.0'," +
                "  'nickName': 'API Manager Workflow Data'," +
                "  'description': 'Workflow Data'," +
                "  'metaData': [" +
                "    {" +
                "      'name': 'clientType'," +
                "      'type': 'STRING'" +
                "    }" +
                "  ]," +
                "  'payloadData': [" +
                "    {" +
                "      'name': 'workflowReference'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'workflowStatus'," +
                "      'type': 'STRING'" +
                "    }," +
                "    {" +
                "      'name': 'tenantDomain'," +
                "      'type': 'STRING'" + "    }," +
                "    {" +
                "      'name': 'workflowType'," +
                "      'type': 'STRING'" + "    }," +
                "    {" +
                "      'name': 'createdTime'," +
                "      'type': 'LONG'" + "    }," +
                "    {" +
                "      'name': 'updatedTime'," +
                "      'type': 'LONG'" + "    }" +
                "  ]" +
                "}";
    }
}
