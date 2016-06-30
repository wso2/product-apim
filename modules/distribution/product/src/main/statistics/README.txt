---------------------------------------------------------------------------------
	Introduction
---------------------------------------------------------------------------------

This document describes how to setup WSO2 Data Analytics Server (DAS) to collect
and analyze runtime statistics from the WSO2 API Manager. Thrift protocol is used
to publish data from the API Manager to DAS. Information processed at DAS are stored
in a database from which the API Publisher can retrieve them and display in the
corresponding UI screens.

---------------------------------------------------------------------------------
	Requirements
---------------------------------------------------------------------------------

1. WSO2 DAS latest snapshot (WSO2 DAS 3.0.0)
2. Java Runtime Environment

NOTE:
You can download a latest snapshot of WSO2 DAS from http://wso2.com/products/business-activity-monitor/

---------------------------------------------------------------------------------
Configuring DAS
---------------------------------------------------------------------------------

1. Extract the DS binary distribution to your local file system.
2. Change port offset to 1 by editing the repository/conf/carbon.xml
3. Copy the API_Manager_Analytics_REST.car and API_Manager_Analytics_RDBMS.car to <WSO2AM_HOME>/statistics
4. Add the following to <DAS_HOME>/conf/datasources/master-datasources.xml file if hope to use RDBMS Client.
   Please note that if you set port offset in BAM server increment and update cassandra datasource.
   Ex: If port offset is one set url config like this <url>jdbc:cassandra://localhost:9161/EVENT_KS</url>
   (By default port is set to 9161 assuming port offset is one)
  <datasource>
          <name>WSO2AM_STATS_DB</name>
          <description>The datasource used for getting statistics to API Manager</description>
	    <jndiConfig>
                <name>jdbc/WSO2AM_STATS_DB</name>
            </jndiConfig>
          <definition type="RDBMS">
          <configuration>
                 <!-- JDBC URL to query the database -->
                 <url>jdbc:h2:<BAM_HOME>/repository/database/APIMGTSTATS_DB;AUTO_SERVER=TRUE</url>
                 <username>wso2carbon</username>
                 <password>wso2carbon</password>
                 <driverClassName>org.h2.Driver</driverClassName>
                 <maxActive>50</maxActive>
                 <maxWait>60000</maxWait>
                 <testOnBorrow>true</testOnBorrow>
                 <validationQuery>SELECT 1</validationQuery>
                 <validationInterval>30000</validationInterval>
            </configuration>
         </definition>
  </datasource>

5. Start WSO2 DAS server

---------------------------------------------------------------------------------
Configuring API Manager
---------------------------------------------------------------------------------


----------------------
Configure RDBMS Client
----------------------

configure the following properties in the api-manager.xml file of API Manager.
<StatisticClientProvider>org.wso2.carbon.apimgt.usage.client.impl.APIUsageStatisticsRdbmsClientImpl</StatisticClientProvider>
    <!--
        JNDI name of the data source to be used for getting BAM statistics.This data source should
        be defined in the master-datasources.xml file in conf/datasources directory.
    -->
    <DataSourceName>jdbc/WSO2AM_STATS_DB</DataSourceName>

And you need to configure the data source definition in the master-datasources.xml file
of API Manager.

    <datasource>
         <name>WSO2AM_STATS_DB</name>
         <description>The datasource used for getting statistics to API Manager</description>
         <jndiConfig>
            <!-- This jndi name should be same as the DataSourceName defined in api-manager.xml -->
            <name>jdbc/WSO2AM_STATS_DB</name>
         </jndiConfig>
         <definition type="RDBMS">
            <configuration>
                <!-- JDBC URL to query the database -->
                <url>jdbc:h2:<BAM_HOME>/repository/database/APIMGTSTATS_DB;AUTO_SERVER=TRUE</url>
                <username>wso2carbon</username>
                <password>wso2carbon</password>
                <driverClassName>org.h2.Driver</driverClassName>
                <maxActive>50</maxActive>
                <maxWait>60000</maxWait>
                <testOnBorrow>true</testOnBorrow>
                <validationQuery>SELECT 1</validationQuery>
                <validationInterval>30000</validationInterval>
            </configuration>
         </definition>
    </datasource>

---------------------
Configure REST Client
---------------------

configure the following properties in the api-manager.xml file of API Manager.
<StatisticClientProvider>org.wso2.carbon.apimgt.usage.client.impl.APIUsageStatisticsRestClientImpl</StatisticClientProvider>


Start APIM

1. go to admin-dashboard using https://localhost:9443/admin/
2. login and click Configure Analytics under settings section
3. select enable combo box and will appear setting to configure analytics
4. set the Event Receiver Configurations according to the DAS
5. Then click the Add url group button to save it.
6. Enter Data Analyzer Configurations according to the DAS
7. Once done click save

NOTE: 1) Replace <DAS_HOME> with the absolute path to the installation directory of DAS.

      2) <DataSourceName> of <APIUsageTracking> entry in api-manager.xml should be same as the
         JNDI config name in master-datasources.xml


---------------------------------------------------------------------------------
Data Purge (Optional)
---------------------------------------------------------------------------------

Data purge is one option to remove historical data in DAS. Since DAS does not allow to delete the DAS table data or Table deletion this option is very important. With data purging you can achieve high performance on data analyzing without removing analyzed summary data. Here we purge data only on stream data fired by APIM. Those data are contained in the following tables.

  ORG_WSO2_APIMGT_STATISTICS_DESTINATION
  ORG_WSO2_APIMGT_STATISTICS_FAULT
  ORG_WSO2_APIMGT_STATISTICS_REQUEST
  ORG_WSO2_APIMGT_STATISTICS_RESPONSE
  ORG_WSO2_APIMGT_STATISTICS_WORKFLOW
  ORG_WSO2_APIMGT_STATISTICS_THROTTLE
Make sure not to purge data other than above table. it will result in vanishing your summarized historical data. There is two ways to purge data in DAS.

-------------------
Using admin console
-------------------

1. Go the the Data-explorer and select above at a time.
2. Then click schedule data purge button.
3. Then set the time and days you need to purge.
4. Do this all of the above tables and wait for the data purging.

-------------
Global method
-------------

Note that this will affect all the tenants
1. Open the <DAS_HOME>/repository/conf/analytics/analytics-config.xml
2. change content of <analytics-data-purging> tag as below

  <analytics-data-purging>
    <!-- Below entry will indicate purging is enable or not. If user wants to enable data purging for cluster then this property need to be enable in all nodes -->
    <purging-enable>true</purging-enable>
    <cron-expression>0 0 12 * * ?</cron-expression>
    <!-- Tables that need include to purging. Use regex expression to specify the table name that need include to purging.-->
    <purge-include-table-patterns>
      <table>.*</table>
      <!--<table>.*jmx.*</table>-->
      </purge-include-table-patterns>
    <!-- All records that insert before the specified retention time will be eligible to purge -->
    <data-retention-days>365</data-retention-days>
  </analytics-data-purging>