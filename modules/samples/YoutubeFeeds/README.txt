How to Deploy & Test YoutubeFeeds Sample?
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

1. Extract wso2am-xxx.zip (eg: wso2am-1.8.0.zip)

2. Go to wso2am-1.8.0/bin folder & type 'ant'

3. Start WSO2AM by executing wso2am-1.8.0/bin/wso2server.sh
 You can login to API Publisher's console
URL - http://localhost:9763/publisher/
Username/password - admin/admin

Take a note of the fact that there are no APIs published. Next step adds an API & publishes it to API store.

7. Run wso2am-1.8.0/samples/YoutubeFeeds/APIPopulator.sh  (or APIPopulator.bat if you are on Windows)
You will see an output similar to following on the console. Refresh above page & you should be seeing the newly added YoutubeFeeds API.

{"error" : "false"}
{"error" : "false"}
{"error" : "false"}

8. Now let's try to access Youtube live feeds through our newly deployed API. First you need to login
to the API Store and obtain an API key. Launch a web browser and enter the URL http://localhost:9763/store

9. Login as the user "admin" with password "admin". Click on the "Applications" tab at
the top of the page, and create a new application. Provide any name you prefer.

10. Now click on the "APIs" tab at the top of the page, select the "YoutubeFeeds" API and subscribe to
it using the newly created application. Go to the "My Subscriptions" tab and select your application. Click
on the "Generate" option in the box titled "Production" to obtain an Application key.

11. Now we are ready to invoke the API. Copy and paste following into a new console window & execute it.

curl -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" http://localhost:8280/youtube/1.0.0/most_popular

(** NOTE: Replace the string '9nEQnijLZ0Gi0gZ6a3pZICktVUca' with the Application key you generated earlier)

You should be able to see the Atom feed from Youtube on you console.

eg:
<?xml version='1.0' encoding='UTF-8'?><feed xmlns='http://www.w3.org/2005/Atom' xmlns:app='http://purl.org/atom/app#'
xmlns:media='http://search.yahoo.com/mrss/' xmlns:openSearch='http://a9.com/-/spec/opensearchrss/1.0/'
xmlns:gd='http://schemas.google.com/g/2005' xmlns:yt='http://gdata.youtube.com/schemas/2007'>
<id>http://gdata.youtube.com/feeds/api/standardfeeds/most_popular</id><updated>2012-07-26T04:51:52.363-07:00</updated>
<category scheme='http://schemas.google.com/g/2005#kind' term='http://gdata.youtube.com/schemas/2007#video'/>
<title type='text'>Most Popular</title><logo>http://www.youtube.com/img/pic_youtubelogo_123x63.gif</logo>
<link rel='alternate' type='text/html' href='http://www.youtube.com/browse?s=bzb'/>...

12. Try accessing various other feeds in the Youtube API by changing the last segment of the invoked URL.

curl -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" http://localhost:8280/youtube/1.0.0/top_rated
curl -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" http://localhost:8280/youtube/1.0.0/most_shared
curl -H "Authorization :Bearer 9nEQnijLZ0Gi0gZ6a3pZICktVUca" http://localhost:8280/youtube/1.0.0/most_viewed
