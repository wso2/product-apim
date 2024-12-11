# WSO2 API Manager

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/Get Support on Stack Overflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-am/)
[![Join us on Discord](https://img.shields.io/badge/Join us on Discord-wso2--apim-blueviolet)](https://discord.com/invite/wso2)

---

WSO2 API Manager is a complete platform for building, integrating, and exposing your digital services as managed APIs in the cloud, on-premise, and hybrid architectures to drive your digital transformation strategy.

It allows API developers to design, publish, and manage the lifecycle of APIs and API product managers to create API products from one or more APIs.

To learn more about WSO2 API Manager please visit http://wso2.com/products/api-manager.

If you run on Kubernetes, we recommend you check our specific solution

Getting Started
=============

## System prerequisites

Refer to this page to consult pre-requisites based on your target architecture and operating system: https://apim.docs.wso2.com/en/latest/install-and-setup/install/installation-prerequisites/

Installation
----------------------------------

1. This product requires a JDK to run. We support JDK from v17 through v21. Check our [supported JDKs](https://apim.docs.wso2.com/en/latest/install-and-setup/setup/reference/product-compatibility/#tested-operating-systems-and-jdks) documentation  for a complete list.
2. Make sure you have set the `JAVA_HOME` environment variable to point to your JDK. See [this documentation](https://apim.docs.wso2.com/en/latest/install-and-setup/install/installing-the-product/installing-api-m-runtime/#setting-up-java_home) if you need help doing so. 
3. Extract the wso2am-4.4.0.zip and go to the 'bin' directory
4. Run the api-manager.sh or api-manager.bat script based on you operating system.
5. Access the respective WSO2 API-M interfaces
    * API Publisher web application is running at: https://localhost:9443/publisher.
      You may sign in to the Publisher using the default administrator credentials (username: admin, password: admin).
    * Developer Portal web application is running at: - https://localhost:9443/devportal \
      You may sign in to the Developer Portal using the default administrator credentials (username: admin, password: admin).

## Exposing your first API

Follow our [Quick Start guide ](https://apim.docs.wso2.com/en/latest/get-started/api-manager-quick-start-guide/) to expose and test your first API while getting familiar with our API designer and developer portal.

Documentation
==============

Online product documentation is available at: https://apim.docs.wso2.com/en/latest/

Support
==================================

WSO2 Inc. offers a variety of development and production support programs, ranging from Web-based support up through normal business hours, to premium 24x7 phone support.

For additional support information please refer to the [WSO2 support page](https://wso2.com/support).

Can you fill this survey ?
==================================

WSO2 wants to learn more about our open source software (OSS) community and your communication preferences to serve you better.

In addition, we may reach out to a small number of respondents to ask additional questions and offer a small gift.

Link to survey: https://forms.gle/h5q4M3K7vyXba3bK6


Known Issues
==================================

All known issues of WSO2 API Manager are filed at: https://github.com/wso2/api-manager/issues

Please check this list before opening a new issue.

Opening an issue
==================================

Please submit any bug reports or feature requests through GitHub:  https://github.com/wso2/api-manager/issues. 


Crypto Notice
==================================

This distribution includes cryptographic software.  The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.  **Before** using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.  See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.  The form and manner of this Apache Software Foundation distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

The following provides more details on the included cryptographic software:

* Apache Rampart   : http://ws.apache.org/rampart/
* Apache WSS4J     : http://ws.apache.org/wss4j/
* Apache Santuario : http://santuario.apache.org/
* Bouncycastle     : http://www.bouncycastle.org/

--------------------------------------------------------------------------------
(c) Copyright 2024 WSO2 Inc.
