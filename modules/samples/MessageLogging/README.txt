How to log request/response message details with custom sequences ?
===================================================================
Steps :
-------
1. Extract wso2am-zzz.zip (e.g. wso2am-2.6.0.zip)

2. Go to <AM_HOME>/bin folder and start wso2am-2.6.0 by executing wso2am-2.6.0/bin/wso2server.sh

3. You can log in to API Publisher Console.
URL - https://localhost:9443/publisher/
Username/password - admin/admin

4. Create an API.

We can log custom request/response messages when invoking the API using a custom sequence extension.


Adding as a Custom Sequence Extension
-------------------------------------
To add the custom logs to API Manager to be used when adding an API follow the steps below.

steps :
-----
(a). Open API Manager Management console.
     URL - https://localhost:9443/carbon/
     Username/password - admin/admin

(b). Go to the registry path and add the custom_log_sequence.xml file provided in the sample as an In sequence or an Out sequence.

Registry paths :
_____________________________________________________________
|Sequence |         Registry path                            |
|_________|__________________________________________________|
|  In	  |   /_system/governance/apimgt/customsequences/in  |
|_________|__________________________________________________|
| Out	  |   /_system/governance/apimgt/customsequences/out |
|_________|__________________________________________________|
|Fault	  |  /_system/governance/apimgt/customsequences/fault|
__________|__________________________________________________|

(c). Save the sequence in the registry path.

(d). Click on the API created in publisher and edit it.


(e). Go to the Manage stage and tick the sequences option.

(f). The sequence XML file you added can be found in the the In Flow or Outflow as you configured.
     If it is added as an 'In sequence ' go to In Flow sequence and select the sequence that you added.

(g). Save the changes to the API and publish it.

(h). Log in to the Store API, subscribe to the API and invoke it.
     URL - https://localhost:9443/store/
     Username/password - admin/admin or you can use self sign up

The above sequence prints a log message on the console on  the invocation of that particular API only.


Adding as a global sequence Extension
-------------------------------------

If we want to log request/response messages in every API invocation we can add a global sequence extension.

(a). Copy the synapse configuration custom_log_sequence.xml and save it into an XML file.(e.g. _global_ext.xml)

(b). Change the pattern of the sequence to a global extension sequence by changing the name "WSO2AM--Ext--In".

(c). save the xml file in <APIM_HOME>/repository/deployment/server/synapse-configs/default/sequences directory.

(d). Log in to the Store API, subscribe to the API and invoke it.
     URL - https://localhost:9443/store/
     Username/password - admin/admin or you can use self sign up


The above sequence prints a log message on the console on every API invocation.