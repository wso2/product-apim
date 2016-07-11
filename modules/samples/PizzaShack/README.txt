
PRE-REQUESTIES
===============
1)WSO2 Application Server
2)WSO2 API Manager

INSTRUCTIONS
==============

1) Login to the Management Console of the API Manager (https://localhost:9443/carbon) as the admin user (admin/admin) and perform the steps below.

    a) Go to 'Configure -> Users and Roles -> Roles' and create a role named 'webuser' and assign it the 'Login' permission
    b) Go to 'Configure -> Users and Roles -> Users' and create a user named 'john' and assign him the 'webuser' role (created above).
    c) Go to 'Configure -> Users and Roles -> Users' and create a user named 'mike' and do not assign him any roles.

2) Login to the API Publisher (https://localhost:9443/publisher) and create the following API using the steps below. If you haven't created any APIs on the Publisher, "Deploy Sample API" option will automatically create this until step c.

   a) Designing the API

        Design the API by using the following information

        API Name = PizzaAPI
        Context  = /pizzashack
        Version  = 1.0.0

        Defining the Resources
        ----------------------

        Define 3 resources as per the information given in the table below.

        -----------------------------------------
        |   | Resource URL     | HTTP Method    |
        |---|------------------|----------------|
        | 1 | /menu            | GET            |
        |---|------------------|----------------|
        | 2 | /order           | POST           |
        |---|------------------|----------------|
        | 3 | /order/{orderid} | GET            |
        |---|------------------|----------------|


        After the above information is entered, press the 'Implement' button.

   b) Implementing the API

        Select 'Backend Endpoint' as the Implementation Method (default option) and provide the endpoint details as below.

        Endpoint Type : HTTP Endpoint
        Endpoint URL  : http://localhost:9443/am/sample/pizzashack/v1/api/

        After the above information is entered, press the 'Manage' button.

   c) Managing the API

        If 'Deploy Sample API' option is used to create the API, select the published API and press 'EDIT API' option on the top bar.
        Then select '3 Manage' tab from the API Edit mode and continue with following steps.

	    Choose appropriate throttling Tier(s) for the API ('Unlimited' preferred) and define an API Scope as per the information below

        Scope Key  : order_pizza
        Scope Name : Order Pizza
        Roles      : webuser

        Scroll down to the section which displays the API Resources and assign the 'Order Pizza' scope to the '/order' and '/order/{orderid}' Resources.
        API published using 'Deploy Sample API' will have extra resources for updating and removing orders which are
        not used in PizzaShack Web Application.
        Do not do any changes to /menu.

        After the above information is entered, press the 'Save & Publish' button and select 'Go to APIStore'.


3) Log in to the API Store (https://localhost:9443/store) and click on the API created earlier. Next, subscribe to it using the default application.

4) After subscription, a message appears. Choose 'View Subscriptions'.

5) The Application page opens. Select 'Production Keys' tab. Create a production key by clicking the Generate button associated with it. You also have the option to increase the default token validity period, which is 1 hour.

6) You get the access token, a consumer key and a consumer secret. Replace the consumer key and secret pair in <APIM_HOME>/samples/PizzaShack/pizza-shack-web/src/main/webapp/WEB-INF/web.xml with the newly generated ones. For example,

	<context-param>
	   <param-name>consumerKey</param-name>
	   <param-value>szsHscDYLeKUcwA1GhPARQlflusa</param-value>
	</context-param>
	<context-param>
	   <param-name>consumerSecret</param-name>
	   <param-value>wJEfRDE3JeFnGMuwVNseNzsXM1sa</param-value>
	</context-param>

    You now have a API subscribed under an application and an access token to the application. Next, we deploy a Web application in the Application Server and use it to invoke the API.

7) Run mvn clean install command in <APIM_HOME>/samples/PizzaShack/pizza-shack-web to build the sample files.

8) App Server should run with port offset 2. Start WSO2 AS (https://localhost:9445/console) and log into its management console

9) Deploy <APIM_HOME>/samples/PizzaShack/pizza-shack-web/target/pizzashack.war into the Application Server.

10) After deploying, access the application using http://localhost:9765/pizzashack. It opens the application in a Web browser.

11) You can use this application to order pizza. Internally, the APIs get invoked when you use the application. 
    
    You can use the two users defined above (john and mike) to login to the PizzaShack web application. You will notice that only user 'john' can order pizzas while 
    'mike' can only view the menu. This is because the '/order' resource of the API is protected with the 'Order Pizza' scope. And only users having the 'webuser' role
    are allowed to get access tokens bearing that scope.

