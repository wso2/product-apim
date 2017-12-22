# API Security sample

### Sample use case

* Prevent misuse or abuse of information or of any application resources exposed by an API

* Have the ability to distinguish between internal, partner and public use of APIs via security controls and audits 

* Ability to trace back which apps are using what APIs (hence data or resources) with which user credentials, permissions and roles 

* Ability to support multiple security standards and protocols 

    * Embed custom security algorithms globally or for selected services

* Ability to enforce both authentication (are you a valid user) and authorization (are you permitted to perform this action) on APIs

### Business Story

* Let ABC organization needs to manage their salary details among a set of permitted employees in there finance department.

* Let the organization is a mobile phone manufacturing company and they have to expose there mobile prices to the public.

* Let the organization management need to secure the services using a custom method.

* Let the organization has a requirement to back track who has utilised the services, and restrict the service usage  with the employees.

### Business Use Cases

* Let ABC organization has an API to manage employee salary details. Let this API should only be accessed by some specific set of people for a given time period to prevent misusing these data.

* Let this organization needs to secure there API’s for internal, partner and public API’s.

* Let this organization needs to check who did changes to their APIS, and who invoked the API’s and etc..

* Further they have a requirement to add custom security standards.

* They need the users of their system to be authenticated to facilitate that only the permitted set of users are using the system and when the API invocation happens they need to authorize the users whether they are permitted to access the API’s.

### How this Business Scenario can be achieved Using WSO2 API Manager ?

We need to create and API to get Salary details of the employees

We need to have separate tenants to manage there API’s through tenants. You can refer the sample scenario one (Managing Public, Partner vs Private APIs)[1]

We need to enable audit logs to trance the API’s creations, API invocations and etc..

As and example to enable custom security algorithms we can engage Kerberos OAuth2 Grant by following this documentation(https://docs.wso2.com/display/AM210/Kerberos+OAuth2+Grant). [2]

We can authorize the users through API Manager access tokens and we can use scopes to authorise the API consumers when consuming the APIS.

By executing the scenario 4 using the run.sh as mentioned below will add the sample data relate the the above business use case to WSO2 API Manager distribution

### How to run the sample to populate the above mentioned sample data

1. Start wso2am-2.2.0-updateX is distribution by executing **[APIM_HOME]**/bin/wso2server.sh or **[APIM_HOME]**/bin/wso2server.bat

2. Run the file run.sh in sample scenarios root directory**[APIM_HOME/sample-scenarios] **as ./run.sh and enter number 4.

#### User credentials needed for login to API Manager instance which the sample data is populated

Username: admin

Password: admin

How the audit logs are printed when the scenario4 sample was run in  **[APIM_HOME]/repository/logs/audit.log**

After invoking and creating the API’s Audi logs looks as below

[2017-12-22 11:48:03,227]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:03,226+0530]

[2017-12-22 11:48:03,341]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:03,339+0530]

[2017-12-22 11:48:09,390]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:09,389+0530]

[2017-12-22 11:48:09,398]  INFO -  Initiator : admin@carbon.super | Action : Add User | Target : tom | Data : { Roles :Internal/subscriber, } | Result : Success

[2017-12-22 11:48:09,437]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:09,436+0530]

[2017-12-22 11:48:09,458]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:09,457+0530]

[2017-12-22 11:48:10,396]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:10,395+0530]

[2017-12-22 11:48:10,819] John@finance.abc.com [1] [AM] INFO -  Initiator : John | Action : create | Target : 0 | Data : { John_Integration_Test_App } | Result : Success

[2017-12-22 11:48:10,941] John@finance.abc.com [1] [AM] INFO -  Initiator : John | Action : update | Target : 1 | Data : { John_Integration_Test_App } | Result : Success

[2017-12-22 11:48:11,132]  INFO -  Initiator : admin | Action : create | Target : 0 | Data : { admin_Integration_Test_App } | Result : Success

[2017-12-22 11:48:11,138]  INFO -  Initiator : admin | Action : update | Target : 2 | Data : { admin_Integration_Test_App } | Result : Success

[2017-12-22 11:48:12,437]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:12,436+0530]

[2017-12-22 11:48:12,709]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:12,709+0530]

[2017-12-22 11:48:12,813]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:12,813+0530]

[2017-12-22 11:48:12,880]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:12,879+0530]

[2017-12-22 11:48:12,909]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:12,909+0530]

[2017-12-22 11:48:13,116]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,116+0530]

[2017-12-22 11:48:13,207]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,207+0530]

[2017-12-22 11:48:13,234]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,234+0530]

[2017-12-22 11:48:13,293]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,293+0530]

[2017-12-22 11:48:13,342]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,341+0530]

[2017-12-22 11:48:13,425]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,425+0530]

[2017-12-22 11:48:13,484]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,484+0530]

[2017-12-22 11:48:13,503] ERROR -  Illegal access attempt at [2017-12-22 11:48:13,0502] from IP address 127.0.0.1 while trying to authenticate access to service EventProcessorAdminService

[2017-12-22 11:48:13,509]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,509+0530]

[2017-12-22 11:48:13,594]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:13,593+0530]

[2017-12-22 11:48:14,449] John@finance.abc.com@finance.abc.com [1] [AM] INFO -  {"performedBy":"John","action":"created","typ":"API","info":"{\"provider\":\"John-AT-finance.abc.com\",\"name\":\"Salary_details_API\",\"context\":\"\\\/t\\\/finance.abc.com\\\/salaries\\\/1.0.0\",\"version\":\"1.0.0\"}"}

[2017-12-22 11:48:14,911]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:14,910+0530]

[2017-12-22 11:48:15,003]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:15,003+0530]

[2017-12-22 11:48:15,175]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:15,175+0530]

[2017-12-22 11:48:15,629]  INFO -  {"performedBy":"admin","action":"created","typ":"API","info":"{\"provider\":\"admin\",\"name\":\"Mobile_stock_API\",\"context\":\"\\\/stocks\\\/1.0.0\",\"version\":\"1.0.0\"}"}

[2017-12-22 11:48:15,689]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:15,689+0530]

[2017-12-22 11:48:15,722]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:15,722+0530]

[2017-12-22 11:48:15,808]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:15,808+0530]

[2017-12-22 11:48:18,412]  INFO -  {"performedBy":"admin","action":"created","typ":"Application","info":"{\"tier\":\"Unlimited\",\"name\":\"Application_one\",\"callbackURL\":null}"}

[2017-12-22 11:48:18,507]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:18,507+0530]

[2017-12-22 11:48:18,512]  INFO -  Initiator : admin | Action : create | Target : 0 | Data : { admin_Application_one_PRODUCTION } | Result : Success

[2017-12-22 11:48:18,514]  INFO -  Initiator : admin | Action : update | Target : 3 | Data : { admin_Application_one_PRODUCTION } | Result : Success

[2017-12-22 11:48:18,520]  INFO -  Initiator : admin | Action : update | Target : 3 | Data : { admin_Application_one_PRODUCTION } | Result : Success

[2017-12-22 11:48:18,671]  INFO -  {"performedBy":"admin","action":"updated","typ":"Application","info":"{\"Generated keys for application\":\"Application_one\"}"}

[2017-12-22 11:48:18,720]  INFO -  {"performedBy":"admin","action":"created","typ":"Subscription","info":"{\"application_name\":\"Application_one\",\"tier\":\"Unlimited\",\"provider\":\"admin\",\"api_name\":\"Mobile_stock_API\",\"application_id\":2}"}

[2017-12-22 11:48:18,732]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:18,732+0530]

[2017-12-22 11:48:18,884]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:18,884+0530] from IP address

[2017-12-22 11:48:19,823]  INFO -  {"performedBy":"admin","action":"created","typ":"Application","info":"{\"tier\":\"Unlimited\",\"name\":\"Application_two\",\"callbackURL\":null}"}

[2017-12-22 11:48:19,848]  INFO -  Initiator : admin | Action : create | Target : 0 | Data : { admin_Application_two_PRODUCTION } | Result : Success

[2017-12-22 11:48:19,851]  INFO -  Initiator : admin | Action : update | Target : 4 | Data : { admin_Application_two_PRODUCTION } | Result : Success

[2017-12-22 11:48:19,854]  INFO -  Initiator : admin | Action : update | Target : 4 | Data : { admin_Application_two_PRODUCTION } | Result : Success

[2017-12-22 11:48:19,888]  INFO -  {"performedBy":"admin","action":"updated","typ":"Application","info":"{\"Generated keys for application\":\"Application_two\"}"}

[2017-12-22 11:48:19,904]  INFO -  {"performedBy":"admin","action":"created","typ":"Subscription","info":"{\"application_name\":\"Application_two\",\"tier\":\"Unlimited\",\"provider\":\"admin\",\"api_name\":\"Mobile_stock_API\",\"application_id\":3}"}

[2017-12-22 11:48:19,908]  INFO -  'admin@carbon.super [-1234]' logged in at [2017-12-22 11:48:19,908+0530]

When analysing the audit logs 

This log  show that a user has been created

[2017-12-22 11:48:09,398]  INFO -  Initiator : admin@carbon.super | Action : Add User | Target : tom | Data : { Roles :Internal/subscriber, } | Result : Success

This log show that an application has been created

[2017-12-22 11:48:10,819] John@finance.abc.com [1] [AM] INFO -  Initiator : John | Action : create | Target : 0 | Data : { John_Integration_Test_App } | Result : Success

This log show the API creation of SAlary details API

[2017-12-22 11:48:14,449] John@finance.abc.com@finance.abc.com [1] [AM] INFO -  {"performedBy":"John","action":"created","typ":"API","info":"{\"provider\":\"John-AT-finance.abc.com\",\"name\":\"Salary_details_API\",\"context\":\"\\\/t\\\/finance.abc.com\\\/salaries\\\/1.0.0\",\"version\":\"1.0.0\"}"}

Further these data can be view in API Manager analytics server as mentioned in scenario9 documentation[3] 

Let’s assume this salary api get should be restricted for admin role users.

For this we have create a scope named new_scope and engaged the admin role to the scope as shown in below

![](images/image_0.png)

When the sample is executing it invokes the api without this scope. The results will show as not authorized as below and print a log

![](images/image_1.png)

WARN - APIAuthenticationHandler API authentication failure due to The access token does not allow you to access the requested resource

Further it will invoke the get with the scope and it will return the response as below

![](images/image_2.png)

### References

[1] - [https://docs.wso2.com/display/AM2xx/Managing+Public%2C+Partner+vs+Private+APIs+Sample+Documentation](https://docs.wso2.com/display/AM2xx/Managing+Public%2C+Partner+vs+Private+APIs+Sample+Documentation)

[2] - [https://docs.wso2.com/display/AM210/Kerberos+OAuth2+Grant](https://docs.wso2.com/display/AM210/Kerberos+OAuth2+Grant)

[3] - [https://docs.wso2.com/display/AM2xx/API+Governanc](https://docs.wso2.com/display/AM2xx/API+Governance)[e](https://docs.wso2.com/display/AM2xx/API+Governance)

