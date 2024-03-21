# WSO2 API Manager
        

---


[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fwso2.org%2Fjenkins%2Fview%2Fproducts%2Fjob%2Fproducts%2Fjob%2Fproduct-apim%2F)](https://wso2.org/jenkins/view/products/job/products/job/product-apim/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-am/)
[![slack](https://img.shields.io/badge/slack-wso2--apim-blueviolet)](https://join.slack.com/t/wso2-apim/shared_invite/enQtNzEzMzk5Njc5MzM0LTgwODI3NmQ1MjI0ZDQyMGNmZGI4ZjdkZmI1ZWZmMjNkY2E0NmY3ZmExYjkxYThjNzNkOTU2NWJmYzM4YzZiOWU?src=sidebar)

---

WSO2 API Manager (WSO2 API-M) is a powerful platform for creating, managing, consuming, and
monitoring web APIs. It combines tried and tested SOA best practices with modern
day API management principles to solve a wide range of enterprise challenges
associated with API provisioning, governance, and integration.

WSO2 API Manager consists of several loosely coupled modules.

        * API Publisher
        * API Developer Portal
        * API Gateway
        * API Key Manager
        * API Traffic Manager


The API publisher module allows API publishers to easily define APIs and manage them
using a strong governance model that consists of well-established concepts such as,
versioning and lifecycles. API consumers can use the API Developer Portal to discover
published, production-ready APIs and access them in a secure and reliable manner
using unique API keys.  The built-in API Gateway module provides powerful tools to
secure and control the load on individual APIs.

WSO2 API Manager is based on the revolutionary WSO2 Carbon [Middleware a' la carte]
framework. All the major features have been developed as reusable Carbon
components.

To learn more about WSO2 API Manager please visit http://wso2.com/products/api-manager.

Key Features
=============
<details>
<summary>Design and Prototype APIs</summary>
        
    - Design APIs, gather developer's feedback before implementing (API First Design).
    - Design can be done from the publishing interface or by importing an existing Swagger definition.
    - Deploy a prototyped API, provide early access to APIs, and get early feedback.
    - Mock API implementation using Javascript.
    - Support publishing SOAP, REST, JSON, and XML style services as XML. 
</details> 

    
<details>
<summary>Create a Developer Portal of All the Available APIs</summary>
        
    - Graphical experience similar to Android Marketplace or Apple App Store.
    - Browse APIs by provider, tags, or name.
    - Self-registration to developer community to subscribe to APIs.
    - Subscribe to APIs and manage subscriptions on per-application basis.
    - Subscriptions can be at different service tiers based on the expected usage levels.
    - Role based access to API Developer Portal, which helps to manage public and private APIs.
    - Manage subscriptions per-developer.
    - Browse API documentation, download helpers for easy consumption.
    - Comment on and rate APIs.
    - Forum for discussing API usage issues (Available soon in a future version).
    - Try APIs directly on the Developer Portal.
    - Internationalization (i18n) support. 
</details>
    
<details>
<summary>Publishing and Governing API use</summary>
        
    - Publish APIs to external consumers and partners, as well as internal users.
    - Supports publishing multiple protocols including SOAP, REST, JSON, and XML style services as APIs.
    - Manage API versions and deployment status by version.
    - Govern the API lifecycle (publish, deprecate, retire).
    - Attach documentation (files, external URLs) to APIs.
    - Provision and Manage API keys.
    - Track consumers per API.
    - One-click deployment to API Gateway for immediate publishing.
</details>
        
<details>
<summary>Control Access and Enforce Security</summary>
        
    - Apply Security policies to APIs (authentication and authorization).
    - Rely on OAuth2 standard for API access (implicit, authorization code, client, SAML, IWA Grant type).
    - Restrict API access tokens to domains/IPs.
    - Block a subscription and restrict a complete application.
    - Associate API available to system defined service tiers.
    - Leverage XACML for entitlements management and fine grained authorization.
    - Configure Single Sign-On (SSO) using SAML 2.0 for easy integration with existing web apps.
    - Powered by WSO2 Enterprise Service Bus (WSO2 ESB).
</details>
        
<details>
<summary>Route API Traffic</summary>
        
    - Supports API authentication with OAuth2.
    - Extremely high performance pass-through message routing with sub-millisecond latency.
    - Enforce rate limiting and throttling policies for APIs by consumer.
    - Horizontally scalable with easy deployment into cluster using proven routing infrastructure.
    - Scales to millions of developers/users.
    - Capture all statistics and push to pluggable analytics system.
    - Configure API routing policies with capabilities of WSO2 Enterprise Service Bus.
    - Powered by WSO2 Enterprise Service Bus.            
</details>
        
<details>
<summary>Manage Developer Community</summary>
        
    - Self-sign up for API consumption.
    - Manage user account including resetting password.
    - Developer interaction with APIs via comments and ratings.
    - Support for developer communication via forums (Available soon in a future version).
    - Powered by WSO2 Identity Server (WSO2 IS).
</details>
        
<details>
<summary>Govern Complete API Lifecycle</summary>  
        
    - Manage API lifecycle from cradle to grave: create, publish, block, deprecate, and retire.
    - Publish both production and sandbox keys for APIs to enable easy developer testing.
    - Publish APIs to partner networks such as ProgrammableWeb (Available soon in a future version).
    - Powered by WSO2 Governance Registry (WSO2 G-Reg).
</details>
        
<details>
<summary>Monitor API Usage and Performance</summary>
        
    - All API usage published to pluggable analytics framework.
    - Out-of-the-box support for the WSO2 Analytics Platform and Google Analytics.
    - View metrics by user, API, and more.
    - Customized reporting via plugging reporting engines.
    - Monitor SLA compliance.
    - Powered by WSO2 Data Analytics Server (WSO2 DAS).      
</details>
    
<details>
<summary>Pluggable, Extensible, and Themeable</summary>  
        
    - All components are highly customizable through styling, theming, and open source code.
    - Developer Portal implemented with React.
    - Pluggable to third-party analytics systems and billing systems (Available soon in a future version).
    - Pluggable to existing user stores including JDBC and LDAP.
    - Components usable separately. 
    - Developer Portal can be used to front APIs that are routed through third-party gateways such as, Intel Expressway Service Gateway.
    - Support for Single Sign On (SSO) using SAML 2.0 for easy integration with existing web apps.
</details>

<details>
<summary>Easily Deployable in Enterprise Settings</summary>
        
    - Role based access control (RBAC) for managing users and their authorization levels.
    - Developer Portal can be deployed in DMZ for external access with the Publisher inside the firewall for private control.
    - Different user stores for developer focused Developer Portal and internal operations in the publisher.
    - Integrates with enterprise identity systems including LDAP and Microsoft Active Directory.
    - Gateway can be deployed in DMZ with controlled access to WSO2 Identity Server (for authentication/authorization) and governance database behind a firewall.
</details>

<details>
<summary>Support for Creating Multi-tenanted APIs</summary>  
        
    - Run a single instance and provide API Management to multiple customers.
    - Share APIs between different departments in a large enterprise.
</details>
 
<details>
<summary>Publishing and Governing API Use</summary> 
        
    - Document an API using Swagger.
    - Restrict API access tokens to domains/IPs.
    - Ability to block a subscription and restricting a complete application.
    - Ability to revoke access tokens.
    - Separate validity period configuration for application access token.
    - OAuth2 authorization code grant type support.
    - Configuring execution point of mediation extensions.
</details>
 
<details>
<summary>Monitor API Usage and Performance</summary>
        
    - Improved dashboard for monitoring usage statistics (Filtering data for a date range, More visually appealing widgets).   
    
</details>    
    

System Requirements
==================================

1. Minimum memory - 2GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java 11 or higher
4. The Management Console requires you to enable Javascript of the Web browser,
   with MS IE 7. In addition to JavaScript, ActiveX should also be enabled
   with IE. This can be achieved by setting your security level to
   medium or lower.
5. Apache Ant is required to compile and run the sample clients. Apache Ant 1.7.0
   version is recommended.
6. To build WSO2 API Manager from the source distribution, it is necessary that you have
   JDK 11 and Maven 3.0.4 or later.

Installation & Running
==================================

1. Extract the wso2am-4.3.0.zip and go to the 'bin' directory
2. Run the api-manager.sh or api-manager.bat script based on you operating system.
3. Access the respective WSO2 API-M interfaces
    * API Publisher web application is running at - https://localhost:9443/publisher \
  You may sign in to the Publisher using the default administrator credentials (username: admin, password: admin).
    * Developer Portal web application is running at - https://localhost:9443/devportal \
  You may sign in to the Developer Portal using the default administrator credentials (username: admin, password: admin).

Documentation
==============

Online product documentation is available at:
        https://apim.docs.wso2.com/en/latest/

Support
==================================

WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support

For more information on WSO2 API Manager please visit https://wso2.com/api-management/

Survey On Open Source Community Communication
==================================

WSO2 wants to learn more about our open source software (OSS) community and your communication preferences to serve you better.

In addition, we may reach out to a small number of respondents to ask additional questions and offer a small gift.

Link to survey: https://forms.gle/h5q4M3K7vyXba3bK6


Known Issues of WSO2 API Manager
==================================

All known issues of WSO2 API Manager are filed at:
   
* https://github.com/wso2/api-manager/issues

Issue Tracker
==================================

Help us make our software better. Please submit any bug reports or feature
requests through GitHub:

   https://github.com/wso2/api-manager/issues


Crypto Notice
==================================

   This distribution includes cryptographic software.  The country in
   which you currently reside may have restrictions on the import,
   possession, use, and/or re-export to another country, of
   encryption software.  BEFORE using any encryption software, please
   check your country's laws, regulations and policies concerning the
   import, possession, or use, and re-export of encryption software, to
   see if this is permitted.  See <http://www.wassenaar.org/> for more
   information.

   The U.S. Government Department of Commerce, Bureau of Industry and
   Security (BIS), has classified this software as Export Commodity
   Control Number (ECCN) 5D002.C.1, which includes information security
   software using or performing cryptographic functions with asymmetric
   algorithms.  The form and manner of this Apache Software Foundation
   distribution makes it eligible for export under the License Exception
   ENC Technology Software Unrestricted (TSU) exception (see the BIS
   Export Administration Regulations, Section 740.13) for both object
   code and source code.

   The following provides more details on the included cryptographic
   software:

* Apache Rampart   : http://ws.apache.org/rampart/
* Apache WSS4J     : http://ws.apache.org/wss4j/
* Apache Santuario : http://santuario.apache.org/
* Bouncycastle     : http://www.bouncycastle.org/

--------------------------------------------------------------------------------
(c) Copyright 2020 WSO2 Inc.
