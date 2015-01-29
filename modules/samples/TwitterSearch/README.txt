 Deploy & Test TwitterSearch Sample
====================================

###########################################################################################
### IMPORTANT: Scripts being used for this sample will not work in Windows environment. ###
### Windows support will be added in near future.                                       ###
### We are working on reducing number of steps required to deploy this sample.          ###
### Any suggestions are welcome @ https://wso2.org/jira/browse/APIMANAGER.               ###
###########################################################################################

Prerequisites:
--------------
Tools needed.
- cURL (http://curl.haxx.se/)
- Apache Ant (http://ant.apache.org)

This sample downloads some resources from the Internet.Therefore, need to have an Internet connetion.

Steps:
------
** IMPORTANT: If you have already configured any other sample, start from Step 7 **

1. Extract wso2am-xxx.zip (eg: wso2am-1.8.0.zip)

2. Go to AM_HOME/bin folder & type 'ant'

3. Start WSO2AM by executing AM_HOME/bin/wso2server.sh
This step will populate all master data required for WSO2AM server to start up. 

Create publisher/subscriber users
----------------------------------

4. Shutdown WSO2AM server. (IMPORTANT: This step is a MUST)

5. Run 'ant' inside AM_HOME/samples/Data
Output is similar to following. It adds two user accounts (provider1 & subscriber1) to WSO2AM's user store.

populate-user-database:
      [sql] Executing resource: .....  /AM_HOME/samples/Data/UserPopulator.sql
      [sql] 10 of 10 SQL statements executed successfully
      
      
6. Start WSO2AM server  and  you can login to API Publisher's console with the username/password as provider1/provider1.
Provider URL - http://localhost:9763/publisher/

Add/Publish API
-------------------

7. Run AM_HOME/samples/TwitterSearch/APIPopulator.sh  (windows: APIPopulator.bat)
Output will be similar to following .

{"error" : "false"}
{"error" : "false"}
{"error" : "false"}

 Refresh the publisher console. User will see the newly added TwitterSearch API.

 Invoke the API
 ----------------
 
8. Now let's try to access Twitter's search function through the newly deployed API. First you need to login
to the API Store and obtain an API accesstoken. Go to store console.
URL: http://localhost:9763/store

9. Login with username/password as subscriber1/subscriber1. Click on the "Applications" tab at
the top of the page, and create a new application.

10. Now click on the "APIs" tab at the top of the page, select the "TwitterSearch" API and subscribe to
it using the newly created application. Go to  "My Subscriptions" menu and select your application. Click
on the "Generate" option in the box titled "Production" to obtain an Application access token.

11. Now we are ready to invoke the TwitterAPI. Invoke the API as following. You can use the cURL utility to invoke the APIs or you can use the "TryIt" tool.

curl -k -d "q=wso2&count=5" -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" https://localhost:8280/twitter/1.0.0/search.atom

NOTE: Replace the Bearer token with the Application accesstoken you generated earlier.

You should be able to see search results from Twitter.
eg:
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:twitter="http://api.twitter.com/" xmlns:georss="http://www.georss.org/georss"
xmlns:google="http://base.google.com/ns/1.0" xmlns:openSearch="http://a9.com/-/spec/opensearch/1.1/" xml:lang="en-US">
<id>tag:search.twitter.com,2005:search/wso2</id><link type="text/html" href="http://search.twitter.com/search?q=wso2"
rel="alternate" /><link type="application/atom+xm..............

12. Try executing the above command several times with different API keys. Note the authentication
failures returned by the API gateway when you pass invalid API keys.

13. After a few invocations, the throttling policy of the API will get activated and the API gateway
will start responding with 503 Service Unavailable response messages.
