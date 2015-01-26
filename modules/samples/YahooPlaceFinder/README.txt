How to Deploy & Test YahooPlaceFinder Sample?
===========================================

###########################################################################################
### IMPORTANT: Scripts being used for this sample will not work in Windows environment. ###
### Windows support will be added in near future.                                       ###
### We are working on reducing number of steps required to deploy this sample.          ###
### Any suggestions are welcome @ https://wso2.org/jira/browse/APISTORE .               ###
###########################################################################################

Prerequisites:
--------------
Following tools should be available on you environment.
- cURL (http://curl.haxx.se/)
- Apache Ant (http://ant.apache.org)

This sample downloads some resources from the Internet. Therefore you need to have an Internet
connection on your machine.

Steps:
------
** IMPORTANT: If you have already configured any other sample, start from Step 7 **

1. Extract wso2am-xxx.zip (eg: wso2am-1.0.0.zip)

2. Go to wso2am-1.0.0/bin folder & type 'ant'

3. Start WSO2AM by executing wso2am-1.0.0/bin/wso2server.sh
This step will populate all master data required for WSO2AM server to start up. In next few steps, we are going to add
some more master data required for this sample.

4. Shutdown WSO2AM server. (IMPORTANT: This step is a MUST)

5. Run 'ant' inside wso2am-1.0.0/samples/Data
You will see an output similar to following. This step adds two user accounts (provider1 & subscriber1) to WSO2AM's user base.

populate-user-database:
      [sql] Executing resource: .....  /wso2am-1.0.0/samples/Data/UserPopulator.sql
      [sql] 10 of 10 SQL statements executed successfully

      
6. Start WSO2AM again & now you can login to API Publisher's console
URL - http://localhost:9763/publisher/
Username/password - provider1/provider1

Take a note of the fact that there are no APIs published. Next step adds an API & publishes it to API store.

7. Run wso2am-1.0.0/samples/YahooPlaceFinder/APIPopulator.sh (or APIPopulator.bat if you are on Windows)
You will see an output similar to following on the console. Refresh above page & you should be seeing the newly added YahooPlaceFinder API.

{"error" : "false"}
{"error" : "false"}
{"error" : "false"}

8. Now let's try to access Yahoo's place finder search function through our newly deployed API. First you need to login
to the API Store and obtain an API key. Launch a web browser and enter the URL http://localhost:9763/store

9. Login as the user "subscriber1" with password "subscriber1". Click on the "Applications" tab at
the top of the page, and create a new application. Provide any name you prefer.

10. Now click on the "APIs" tab at the top of the page, select the "TwitterSearch" API and subscribe to
it using the newly created application. Go to the "My Subscriptions" tab and select your application. Click
on the "Generate" option in the box titled "Production" to obtain an Application key.

11. Now we are ready to invoke the API. Copy and paste following into a new console window & execute it.

curl -v -H "Authorization: Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" http://localhost:8280/placeFinder/1.0.0?q=Colombo

(** NOTE: Replace the string '9nEQnijLZ0Gi0gZ6a3pZICktVUca' with the Application key you generated earlier)

You should be able to see search results from PlaceFinder on you console.
eg:
 <ResultSet version="1.0"><Error>0</Error><ErrorMessage>No error</ErrorMessage><Locale>us_US</Locale>
 <Quality>40</Quality><Found>1</Found><Result><quality>40</quality><latitude>6.927200</latitude>
 <longitude>79.872200</longitude><offsetlat>6.927200</offsetlat><offsetlon>79.872200</offsetlon>
 <radius>6500</radius><name /><line1 /><line2>Colombo</line2><line3 /><line4>Sri Lanka</line4><house />
 <street /><xstreet /><unittype /><unit /><postal /><neighborhood /><city>Colombo</city><county>Colombo</county>
 <state>Western</state><country>Sri Lanka</country><countrycode>LK</countrycode><statecode /><countycode /><uzip />
 <hash /><woeid>2189783</woeid><woetype>7</woetype></Result></ResultSet>

12. Try executing the above command several times with different API keys. Note the authentication
failures returned by the API gateway when you pass invalid API keys.

13. After a few invocations, the throttling policy of the API will get activated and the API gateway
will start responding with 503 Service Unavailable response messages.
