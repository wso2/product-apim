================================================================================
                               WSO2 API Manager
================================================================================

WSO2 API Manager (WSO2 API-M) is a powerful platform for creating, managing, consuming, 
and monitoring web APIs. It combines tried and tested SOA best practices with modern 
day API management principles to solve a wide range of enterprise challenges associated 
with API provisioning, governance, and integration.

WSO2 API Manager consists of several loosely coupled modules.

        * API publisher
        * API store
        * API gateway
	* API Key Manager
	* API Traffic Manager

The API publisher module allows API publishers to easily define APIs and manage them 
using a strong governance model that consists of well-established concepts such as, 
versioning and lifecycles. API consumers can use the API Store module to discover 
published, production-ready APIs and access them in a secure and reliable manner 
using unique API keys. The built-in API Gateway module provides powerful tools to 
secure and control the load on individual APIs.

WSO2 API Manager is based on the revolutionary WSO2 Carbon [Middleware a' la carte]
framework. All the major features have been developed as reusable Carbon
components.

To learn more about WSO2 API Manager please visit http://wso2.com/products/api-manager.

Key Features
=============

* Design and Prototype APIs:   
      - Design APIs, gather developer's feedback before implementing (API First Design).
      - Design can be done from the publishing interface or by importing an existing Swagger 
      definition.
      - Deploy a prototyped API, provide early access to APIs, and get early feedback.
      - Mock API implementation using Javascript.
      - Support publishing SOAP, REST, JSON, and XML style services as XML.    
    
* Create a Store of All Available APIs:    
      - Graphical experience similar to Android Marketplace or Apple App Store.
      - Browse APIs by provider, tags, or name.
      - Self-registration to developer community to subscribe to APIs.
      - Subscribe to APIs and manage subscriptions on per-application basis.
      - Subscriptions can be at different service tiers based on the expected usage levels.
      - Role based access to API Store, which helps to manage public and private APIs.
      - Manage subscriptions per-developer.
      - Browse API documentation, download helpers for easy consumption.
      - Comment on and rate APIs.
      - Forum for discussing API usage issues (Available soon in a future version).
      - Try APIs directly on the Store front.
      - Internationalization (i18n) support.
  
* Publishing and Governing API Use:     
      - Publish APIs to external consumers and partners, as well as internal users.
      - Supports publishing multiple protocols including SOAP, REST, JSON, and XML style 
      services as APIs.
      - Manage API versions and deployment status by version.
      - Govern the API lifecycle (publish, deprecate, retire).
      - Attach documentation (files, external URLs) to APIs.
      - Provision and Manage API keys.
      - Track consumers per API.
      - One-click deployment to API Gateway for immediate publishing.
        
* Control Access and Enforce Security:
      - Apply Security policies to APIs (authentication and authorization).
      - Rely on OAuth2 standard for API access (implicit, authorization code, client, SAML, 
      IWA Grant type).
      - Restrict API access tokens to domains/IPs
      - Block a subscription and restrict a complete application.
      - Associate API available to system defined service tiers.
      - Leverage XACML for entitlements management and fine grained authorization.
      - Configure Single Sign-On (SSO) using SAML 2.0 for easy integration with existing 
      web apps.
      - Powered by WSO2 Enterprise Service Bus (WSO2 ESB).
        
* Route API Traffic:
      - Supports API authentication with OAuth2.
      - Extremely high performance pass-through message routing with sub-millisecond 
      latency.
      - Enforce rate limiting and throttling policies for APIs by consumer.
      - Horizontally scalable with easy deployment into cluster using proven routing 
      infrastructure.
      - Scales to millions of developers/users.
      - Capture all statistics and push to pluggable analytics system.
      - Configure API routing policies with capabilities of WSO2 Enterprise Service Bus.
      - Powered by WSO2 Enterprise Service Bus.           

* Manage Developer Community:   
      - Self-sign up for API consumption.
      - Manage user account including resetting password.
      - Developer interaction with APIs via comments and ratings.
      - Support for developer communication via forums (Available soon in a future version).
      - Powered by WSO2 Identity Server (WSO2 IS).
        
* Govern Complete API Lifecycle:        
      - Manage API lifecycle from cradle to grave: create, publish, block, deprecate, 
      and retire.
      - Publish both production and sandbox keys for APIs to enable easy developer testing.
      - Publish APIs to partner networks such as, ProgrammableWeb 
      (Available soon in a future version).
      - Powered by WSO2 Governance Registry (WSO2 G-Reg).
        
* Monitor API Usage and Performance:        
      - All API usage published to pluggable analytics framework.
      - Out-of-the-box support for the WSO2 Analytics Platform and Google Analytics.
      - View metrics by user, API, and more.
      - Customized reporting via plugging reporting engines.
      - Monitor SLA compliance.
      - Powered by WSO2 Data Analytics Server (WSO2 DAS).     
    
* Pluggable, Extensible and Themeable:        
      - All components are highly customizable through styling, theming, and open source code.
      - Storefront implemented with Jaggery (jaggeryjs.org) for easy of customization.
      - Pluggable to third-party analytics systems and billing systems 
      (Available soon in a future version).
      - Pluggable to existing user stores including JDBC and LDAP.
      - Components usable separately.
      - Store front can be used to front APIs that are routed through third-party gateways 
      such as, Intel Expressway Service Gateway.
      - Support for Single Sign On (SSO) using SAML 2.0 for easy integration with existing 
      web apps.
        
* Easily Deployable in Enterprise Setting:        
      - Role based access control (RBAC) for managing users and their authorization levels.
      - Store-front can be deployed in DMZ for external access with the Publisher inside the 
      firewall for private control.
      - Different user stores for developer focused store-front and internal operations in 
      the publisher.
      - Integrates with enterprise identity systems including LDAP and Microsoft Active Directory.
      - Gateway can be deployed in DMZ with controlled access to WSO2 Identity Server 
      (for authentication/authorization) and governance database behind a firewall.

* Support for creating multi-tenanted APIs:        
      - Run a single instance and provide API Management to multiple customers.
      - Share APIs between different departments in a large enterprise.
        
* Publishing and Governing API Use:        
      - Document an API using Swagger.
      - Restrict API access tokens to domains/IPs.
      - Ability to block a subscription and restricting a complete application.
      - Ability to revoke access tokens.
      - Separate validity period configuration for application access token.
      - OAuth2 authorization code grant type support.
      - Configuring execution point of mediation extensions.
        
* Monitor API Usage and Performance:        
      - Improved dashboard for monitoring usage statistics (Filtering data for a date range, 
      More visually appealing widgets).    
 
System Requirements
==================================

1. Minimum memory - 2GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java 1.8 or higher
4. The Management Console requires you to enable Javascript of the Web browser,
   with MS IE 7. In addition to JavaScript, ActiveX should also be enabled
   with IE. This can be achieved by setting your security level to
   medium or lower.
5. Apache Ant is required to compile and run the sample clients. Apache Ant 1.7.0 
is recommended.
6. To build WSO2 API Manager from the Source distribution, it is necessary that you have
   JDK 1.8 version and Maven 3.0.4 or later

Installation & Running
==================================

1. Extract the wso2am-2.6.0.zip and go to the 'bin' directory
2. Run the wso2server.sh or wso2server.bat script based on you operating system.
3. Access the respective WSO2 API-M interfaces
    * API Publisher web application is running at - https://localhost:9443/publisher
      You may sign in to the Publisher using the default administrator credentials 
      (username: admin, password: admin).
    * API Store web application is running at - https://localhost:9443/store
      You may sign in to the Store using the default administrator credentials 
      (username: admin, password: admin).

Documentation
==============

On-line product documentation is available at:
        https://docs.wso2.com/display/AM250/WSO2+API+Manager+Documentation

Support
==================================

WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support

For more information on WSO2 API Manager please visit http://wso2.com/products/api-manager

Known Issues of WSO2 API Manager
==================================

All known issues of WSO2 API Manager are filed at:

* https://wso2.org/jira/issues/?filter=14454 
* https://github.com/wso2/product-apim/issues?q=is%3Aopen+is%3Aissue+label%3A2.6.0

Issue Tracker
==================================

Help us make our software better. Please submit any bug reports or feature
requests through GitHub:

    https://github.com/wso2/product-apim/issues


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

   Apache Rampart   : http://ws.apache.org/rampart/
   Apache WSS4J     : http://ws.apache.org/wss4j/
   Apache Santuario : http://santuario.apache.org/
   Bouncycastle     : http://www.bouncycastle.org/

--------------------------------------------------------------------------------
(c) Copyright 2018 WSO2 Inc.
