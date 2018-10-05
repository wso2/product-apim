How to Deploy & Test WikipediaAPI Sample?
=========================================

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

1. Extract wso2am-xxx.zip (eg: wso2am-2.6.0.zip)

2. Go to wso2am-2.6.0/bin folder & type 'ant'

3. Start WSO2AM by executing wso2am-2.6.0/bin/wso2server.sh
This step will populate all master data required for WSO2AM server to start up. In next few steps, we are going to add
some more master data required for this sample.

6.You can login to API Publisher's console
URL - https://localhost:9443/publisher/
Username/password - admin/admin

Take a note of the fact that there are no APIs published. Next step adds an API & publishes it to API store.

7. Run wso2am-2.6.0/samples/WikipediaAPI/APIPopulator.sh  (or APIPopulator.bat if you are on Windows)
You will see an output similar to following on the console. Refresh above page & you should be seeing the newly added Wikipedia API.

{"error" : "false"}
{"error" : "false"}
{"error" : "false"}

8. Now let's try to access Wikipedia through our newly deployed API. First you need to login
to the API Store and obtain an API key. Launch a web browser and enter the URL https://localhost:9443/store

9. Login as the user "admin" with password "admin". Click on the "Applications" tab at
the top of the page, and create a new application. Provide any name you prefer.

10. Now click on the "APIs" tab at the top of the page, select the "WikipediaAPI" API and subscribe to
it using the newly created application. Go to the "My Subscriptions" tab and select your application. Click
on the "Generate" option in the box titled "Production" to obtain an Application key.

11. Now we are ready to invoke the API. Copy and paste following into a new console window & execute it.

curl -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" "http://localhost:8280/wikipedia/1.0.0?format=json&action=query&titles=Main Page&prop=revisions&rvprop=content"

(** NOTE: Replace the string '9nEQnijLZ0Gi0gZ6a3pZICktVUca' with the Application key you generated earlier)

You should be able to see the JSON result from Wikipedia API on you console.

eg:
{"query":{"pages":{"5982813":{"pageid":5982813,"ns":0,"title":"MainPage","revisions":[{"contentformat":"text/x-wiki","contentmodel":"wikitext",
"*":"#Redirect [[Main Page]]\n\n{{Redr|mod|rcc}}"}]}}}}...

12. Please refer "http://www.mediawiki.org/wiki/API:Main_page" to find more about wikipedia API. You can use command in step 11 to try out various API actions and features.
