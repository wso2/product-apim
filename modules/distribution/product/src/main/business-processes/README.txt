Work Flow integration for API manager
=====================================

We can engage business process execution for API management-releated operations. With API manager we have added 3 workflow plug points for below operations.
01. User creation process.
02. Application creation process.
03. Subscription process.
04. Application Registration process. 

If we performed any of above operations with properly configured API Manager to integrate web service based workflow executor[which is default ship with AM 1.6.0],then API manager will execute business process workflow for each of above operations. Each business process has 4 main operations.
01. Accept workflow request coming from API manager to BPS.
02. Create human tasks instance in BPS.
03. User interaction to approve workflow. User can log in to "Admin Portal" jaggery app which is default ship with API manager 1.6.0 and approve process.The url for "Admin Portal" jaggery application is "http[s]://ip:port/admin"
04. Call back to API manager after workflow execution.


Configure workflow execution
============================
In order to run this we will assume users running API manager with port offset 0 and Business Process server with port offset 02.

Business Process Server Configurations 
======================================
Set port offset as 2 in carbon.xml file of Business Process Server 3.1.0. Edit following element of carbon.xml file. If you require to make the offset something other than 2, see section 'Changing port offset of BPS' section below.
       <Offset>2</Offset>

Copy the /epr folder in to the repository/conf folder of Business Process Server.
Then copy human tasks (archived) files to repository/deployment/server/humantasks folder of Business Process Server.
Then copy business process (archived) files to repository/deployment/server/bpel folder of Business Process Server.
Then start server.
If you wish to change port offset to some other value or set hostname to a different value than localhost [when running BPS and AM in different machines] , then you will need to edit all .epr files available inside repository/conf/epr folder.
In addition to that we need to edit human tasks wsdl files as well. We need this because once human task finished it will call back to workflow. So it 
should aware about exact service location of workflow.
 


API Manager Configurations 
==========================
Edit the  API manager configurations stored in the registry to enable web service based workflow execution. For this we need to log in to APIManager admin console (https://<Server Host>:9443/carbon) and select Browse under Resources.Go to /_system/governance/apimgt/applicationdata/workflow-extensions.xml resource and edit the file as follows to disable the Simple Workflow Executor and enable WS Workflow Executor:
Edit WorkFlowExtensions as follows. Please note that all workflow process services
are running on port 9765 of Business Process Server(as it is running with port offset2). 

      <ApplicationCreation executor="org.wso2.carbon.apimgt.impl.workflow.ApplicationCreationWSWorkflowExecutor">
           <Property name="serviceEndpoint">http://localhost:9765/services/ApplicationApprovalWorkFlowProcess</Property>
           <Property name="username">admin</Property>
           <Property name="password">admin</Property>
           <Property name="callbackURL">https://localhost:8243/services/WorkflowCallbackService</Property>
      </ApplicationCreation>
    
      <SubscriptionCreation executor="org.wso2.carbon.apimgt.impl.workflow.SubscriptionCreationWSWorkflowExecutor">
           <Property name="serviceEndpoint">http://localhost:9765/services/SubscriptionApprovalWorkFlowProcess</Property>
           <Property name="username">admin</Property>
           <Property name="password">admin</Property>
           <Property name="callbackURL">https://localhost:8243/services/WorkflowCallbackService</Property>
      </SubscriptionCreation>
      
      <UserSignUp executor="org.wso2.carbon.apimgt.impl.workflow.UserSignUpWSWorkflowExecutor">
           <Property name="serviceEndpoint">http://localhost:9765/services/UserSignupProcess</Property>
           <Property name="username">admin</Property>
           <Property name="password">admin</Property>
           <Property name="callbackURL">https://localhost:8243/services/WorkflowCallbackService</Property>
      </UserSignUp>

      <ProductionApplicationRegistration executor="org.wso2.carbon.apimgt.impl.workflow.ApplicationRegistrationWSWorkflowExecutor">
        <Property name="serviceEndpoint">http://localhost:9765/services/ApplicationRegistrationWorkFlowProcess/</Property>
        <Property name="username">admin</Property>
        <Property name="password">admin</Property>
        <Property name="callbackURL">https://localhost:8248/services/WorkflowCallbackService</Property>
      </ProductionApplicationRegistration>

      <SandboxApplicationRegistration executor="org.wso2.carbon.apimgt.impl.workflow.ApplicationRegistrationWSWorkflowExecutor">
        <Property name="serviceEndpoint">http://localhost:9765/services/ApplicationRegistrationWorkFlowProcess/</Property>
        <Property name="username">admin</Property>
        <Property name="password">admin</Property>
        <Property name="callbackURL">https://localhost:8248/services/WorkflowCallbackService</Property>
      </SandboxApplicationRegistration>
    </WorkFlowExtensions>


Changing port offset of BPS
===========================

The default configurations of the Business Processes have been defined for a BPS which is running on offset 2. However if you require to change it to some other value, following are the steps you need to carry out.

Note: The default http and https ports (without offset) of a carbon server respectively are 9763 and 9443. The same when offset by 2 becomes 9765 and 9445 respectively.

1. Grep (search) for the value 9765 within all the files in the /epr directory and change them accordingly. 

2. 
   a) Unzip the file(s) within the relevant business process' 'HumanTask' directory (ex: subscription-creation/HumanTask)
   b) Grep (search) for the value 9765 within all the .wsdl files and update them accordingly.
   c) Zip the contents you unzipped in (a) and recreate the zip file. Note to deploy the newly created zip file when deploying the same on BPS.

3. Open the <AM_HOME>/repository/deployment/server/jaggeryapps/admin/site/conf/site.json file and update all references of 9445 accordingly.
