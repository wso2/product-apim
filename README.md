        ================================================================================

[![Join the chat at https://gitter.im/wso2/product-apim](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/wso2/product-apim?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
                                        WSO2 API Manager
        ================================================================================

WSO2 API Manager is a powerful platform for creating, managing, consuming and
monitoring web APIs. It combines tried and tested SOA best practices with modern
day API management principles to solve a wide range of enterprise challenges
associated with API provisioning, governance and integration.

WSO2 API Manager consists of several loosely coupled modules.

        * API publisher
        * API store
        * API gateway
	    * API Key Manager

The API publisher module allows API publishers to easily define APIs and manage them
using a strong governance model which consists of well-established concepts such as
versioning and lifecycles. API consumers can use the API store module to discover
published, production-ready APIs and access them in a secure and reliable manner
using unique API keys.  The built-in API gateway module provides powerful tools to
secure and control the load on individual APIs.

WSO2 API Manager is based on the revolutionary WSO2 Carbon [Middleware a' la carte]
framework. All the major features have been developed as reusable Carbon
components.

To learn more about WSO2 API Manager please visit http://wso2.com/products/api-manager.

Key Features
=============

* API publishers:
    - Simple web-based UI for defining APIs
    - Easily modify existing APIs and move them across life cycle states
    - Specify and attach documentation to defined APIs
    - Create new versions of existing APIs
    - Specify SLAs under which each API is exposed to the consumers
    - Track and monitor API usage

* API consumers:
    - Rich web portal to discover published APIs
    - Create applications, subscribe and obtain API keys
    - Browse documentation and samples associated with each API
    - Try APIs on-line before using them
    - Rate APIs and comment on their features, usability and other related aspects

System Requirements
==================================

1. Minimum memory - 1GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java SE Development Kit 1.6.0_24 or higher
4. The Management Console requires you to enable Javascript of the Web browser,
   with MS IE 6 and 7. In addition to JavaScript, ActiveX should also be enabled
   with IE. This can be achieved by setting your security level to
   medium or lower.
5. To compile and run the sample clients, an Ant version is required. Ant 1.7.0
   version is recommended
6. To build WSO2 API Manager from the Source distribution, it is necessary that you have
   JDK 1.6.x version and Maven 3.0.0 or later

Installation & Running
==================================

1. Extract the wso2am-1.8.0.zip and go to the 'bin' directory
2. Run the wso2server.sh or wso2server.bat as appropriate
3. API Publisher web application is running at http://localhost:9763/publisher. You may login
   to the Publisher using the default administrator credentials (user: admin, pass: admin).
4. API Store web application is running at http://localhost:9763/store. You may login
   to the Store using the default administrator credentials (user: admin, pass: admin).

Documentation
==============

On-line product documentation is available at:
        http://docs.wso2.org/wiki/display/AM180/WSO2+API+Manager+Documentation

Support
==================================

WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support

For more information on WSO2 API Manager please visit http://wso2.com/products/api-manager

Known issues of WSO2 API Manager
==================================

All known issues of WSO2 API Manager are filed at:

    https://wso2.org/jira/secure/IssueNavigator.jspa?requestId=10810

Issue Tracker
==================================

Help us make our software better. Please submit any bug reports or feature
requests through the WSO2 JIRA system:

    https://wso2.org/jira/browse/APIMANAGER


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
(c) Copyright 2014 WSO2 Inc.
