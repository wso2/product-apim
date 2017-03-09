StockQuote Service
------------------

This is sample service available in wso2 ms4j samples called 'SimpleStockquote Fat Jar Sample'. 

A fat jar is a jar file which includes all the dependencies in one fat (uber) jar. 
This mode of creating a fat jar and running it in a Java process is also referred to as server-less execution.


Please refer to following for more information.
  
  https://github.com/wso2/msf4j/tree/master/samples/stockquote/fatjar
    

####How to run

Use following command to run the application

```
java -jar stockquote-fatjar-*.jar
```

This will start a service which listen for http traffic over port 8080


####How to test the sample 

Use following cURL commands.

```
curl http://localhost:8080/stockquote/IBM
```

You should get a successful response if everything worked fine.


####Configure Ports


We can configure ports using  netty configuration file (Sample config files available in conf directory)

```
java -jar -Dtransports.netty.conf=conf/netty-8081.yml stockquote-fatjar-*.jar
```
