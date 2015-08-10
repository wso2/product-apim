---------------------------------------------------------------------------------
	Introduction
---------------------------------------------------------------------------------

This document describes how to setup WSO2 Business Activity Monitor (BAM) to collect
and analyze runtime statistics from the WSO2 API Manager. Thrift protocol is used
to publish data from the API Manager to BAM. Information processed at BAM are stored
in a database from which the API Publisher can retrieve them and display in the
corresponding UI screens.

---------------------------------------------------------------------------------
	Requirements
---------------------------------------------------------------------------------

1. WSO2 BAM latest snapshot (WSO2 BAM 2.3.0)
2. Java Runtime Environment

NOTE:
You can download a latest snapshot of WSO2 BAM from http://wso2.com/products/business-activity-monitor/

---------------------------------------------------------------------------------
Configuring BAM
---------------------------------------------------------------------------------

1. Extract the BAM binary distribution to your local file system.
2. Change port offset to 1 by editing the repository/conf/carbon.xml
3. Copy the  API_Manager_Analytics.tbox to repository/deployment/server/bam-toolbox
   (Create the bam-toolbox directory if it already doesn't exist)
4. Add the following to <BAM_HOME>/conf/datasources/master-datasources.xml file.
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

        <datasource>
           <name>WSO2BAM_CASSANDRA_DATASOURCE</name>
           <description>The datasource used for Cassandra data</description>
           <definition type="RDBMS">
               <configuration>
                   <url>jdbc:cassandra://localhost:9161/EVENT_KS</url>
                   <username>admin</username>
                   <password>admin</password>
               </configuration>
           </definition>
       </datasource>

5. Start WSO2 BAM server

---------------------------------------------------------------------------------
Configuring API Manager
---------------------------------------------------------------------------------

To enable API statistics collection you need to configure the following properties in the
api-manager.xml file of API Manager.

    <!--
	    Enable/Disable the API usage tracker.
    -->
	<Enabled>true</Enabled>

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


NOTE: 1) Replace <BAM_HOME> with the absolute path to the installation directory of BAM.

      2) <DataSourceName> of <APIUsageTracking> entry in api-manager.xml should be same as the
         JNDI config name in master-datasources.xml

---------------------------------------------------------------------------------
Changing the Statistics Database
---------------------------------------------------------------------------------
It is possible to use a different database than the default h2 database for statitic publishing.
When doing this you need to change the properties of the datasource element, and additionally delete
some meta data tables created by the previous executions of the hive script.

Drop the meta data table by executing the following in BAM script editor.
You can go to the script editor in BAM by accessing, Main > Analytics > Add in BAM Management Console.

drop table APIRequestData;
drop table APIRequestSummaryData;
drop table APIVersionUsageSummaryData;
drop table APIVersionUsageSummaryData;
drop table APIResponseData;
drop table APIResponseSummaryData;
drop table APIRequestData;
drop table APIRequestSummaryData;
drop table APIVersionUsageSummaryData;
drop table APIResponseData;
drop table APIResponseSummaryData;
drop table APIRequestDataMinimal;
drop table APIRequestSummaryDataMinimal;
drop table APIThrottleData;
drop table APIThrottleSummaryData;
