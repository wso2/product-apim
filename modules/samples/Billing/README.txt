---------------------------------------------------------------------------------
	Introduction
---------------------------------------------------------------------------------

This document describes how to setup WSO2 Business Activity Monitor (BAM) to collect
and accumilate runtime statistics from the WSO2 API Manager and generate bill for API
usage of consumers. 

---------------------------------------------------------------------------------
	Requirements
---------------------------------------------------------------------------------

1. WSO2 BAM latest snapshot (WSO2 BAM 2.0.0 or above)
2. Java Runtime Environment

NOTE:
You can download a latest snapshot of WSO2 BAM from our main product page.

---------------------------------------------------------------------------------
Configuring BAM
---------------------------------------------------------------------------------

1. Extract the BAM binary distribution to your local file system.
2. Change port offset to 1 by editing the repository/conf/carbon.xml(If you are running API Manager in default ports)
3. Copy the  API_Manager_Analytics.tbox attached in this zip to repository/deployment/server/bam-toolbox
   (Create the bam-toolbox directory if it already doesn't exist)
4. Add the following to <BAM_HOME>/conf/datasources/master-datasources.xml file.

  <datasource>
          <name>WSO2AM_STATS_DB</name>
          <description>The datasource used for getting statistics to API Manager</description>
	  <jndiConfig>
                <name>jdbc/WSO2AM_STATS_DB</name>
            </jndiConfig>
          <definition type="RDBMS">
          <configuration>
                 <!-- JDBC URL to query the database -->
                 <url>jdbc:h2:repository/database/APIMGTSTATS_DB;AUTO_SERVER=TRUE</url>
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

Following data-source configuration can be found in master-datasources.xml file of BAM. In this data-source, cassandra port given in JDBC connection url 
need be changed according to the port offset defined in BAM server. If the port offset is 1, then cassandra port should changed to '9161'. 

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

1.To enable API statistics collection you need to configure the following properties in the
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

     <!--
            Enable/Disable Usage metering and billing for api usage
     -->
     <EnableBillingAndUsage>true</EnableBillingAndUsage>



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

Copy billing-conf.xml file into  <AM_HOME>repository/conf/ folder.

NOTE: 1) Replace <BAM_HOME> with the absolute path to the installation directory of BAM.

      2) <DataSourceName> of <APIUsageTracking> entry in api-manager.xml should be same as the
         JNDI config name in master-datasources.xml 
