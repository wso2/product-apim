---------------------------------------------------------------------------------
	Requirements
---------------------------------------------------------------------------------

1. WSO2 BAM-2.0.0-ALPHA2
2. Apache Ant
3. Java Runtime Environment

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

And you need to configure the datasource definition in the master-datasources.xml file[in conf/datasources]
of API Manager.

    <datasource>
         <name>WSO2AM_STATS_DB</name>
         <description>The datasource used for getting statistics to API Manager</description>
         <jndiConfig>
               <name>jdbc/WSO2AM_STATS_DB</name> //This jndi lookup name should equal to the DataSourceName defined in api-manager.xml as above
         </jndiConfig>
         <definition type="RDBMS">
                <configuration>
                <url>jdbc:h2:<BAM_HOME>/repository/database/APIMGTSTATS_DB;AUTO_SERVER=TRUE</url> //JDBC URL to query remote JDBC database
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


NOTE: 1) Replace <BAM_HOME> with the absolute path to the installation directory of BAM,in JDBC URL
      entry of above datasource definition.

      2) <DataSourceName> of <APIUsageTracking> entry in api-manager.xml should equal to JNDI config name
        of master-datasources.xml


---------------------------------------------------------------------------------
Configuring BAM
---------------------------------------------------------------------------------

1.Copy StatClient directory into BAM_HOME
2.Change port offset to 1 by editing the repository/conf/carbon.xml
3.Start WSO2 BAM server
4.Go to StatClient directory and run "ant initialize_column_family_datastore" command (You need to
  have an Internet connection.)
5.Finally you can see the message - "BAM configured successfully for collecting API stats"
6.Now start deploying samples & invoking them. Your invocation statistics should be visible under,
- APIs -> All Statistics
- My APIs -> Statistics
- APIs -> All -> [YOUR API SAMPLE NAME] -> 'Versions' tab
- APIs -> All -> [YOUR API SAMPLE NAME] -> 'Users' tab
