<img src="https://wso2.cachefly.net/wso2/sites/all/image_resources/wso2-branding-logos/wso2-logo-orange.png" alt="WSO2 logo" width=30% height=30% />

# WSO2 API Manager

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/Get%20Support%20on%20Stack%20Overflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-am/)
[![Join the community on Discord](https://img.shields.io/badge/Join%20us%20on%20Discord-wso2--apim-blueviolet)](https://discord.com/invite/wso2)
[![X](https://img.shields.io/twitter/follow/wso2.svg?style=social&label=Follow%20Us)](https://twitter.com/intent/follow?screen_name=wso2)

---

WSO2 API Manager is a complete platform for building, integrating, and exposing your digital services as managed APIs in the cloud, on-premise, and hybrid architectures to drive your digital transformation strategy.

It allows API developers to design, publish, and manage the lifecycle of APIs and API product managers to create API products from one or more APIs.

To learn more about WSO2 API Manager please visit https://wso2.com/api-management/.

> If you run your workloads on Kubernetes, we recommend you check our specific solution for the Kubernetes platform https://wso2.com/api-platform-for-k8s/

## Why WSO2 API Manager?

From simple scenarios to comprehensive protocol support, WSO2 API Manager can handle it all. Use industry standards or extend our platform to integrate with your existing business needs, applications, and architectures. Tailor it to your exact use case, whether that means customizing the user interface, mediation, security, or integrating third-party solutions.

### Interoperability with open standards

* Achieve seamless interoperability between diverse systems without vendor lock-in, utilizing modern approaches with REST, GraphQL, and AsyncAPIs.
* Unlock collaborative innovation, long-term sustainability, enhanced security, and regulatory compliance.
* Strategically adopt modern service delivery and development paradigms without leaving behind the legacy systems that made you successful in the first place.

### Advanced integration support

The integration runtime supports creating composite microservices, message routing, transformation, message mediation, service orchestration, as well as consuming and processing streaming data.

### Extensibility and customizability

WSO2 API Manager is a versatile and adaptable platform, capable of catering to the requirements of enterprises of all sizes, ranging from emerging startups to well-established businesses with decades of history. It presents user-friendly extension opportunities to tailor authenticators, policies, mediations, API lifecycles, workflows, portals, and login pages to your specific needs.

## Getting started

### System prerequisites

Refer to this page to consult pre-requisites based on your target architecture and operating system: https://apim.docs.wso2.com/en/latest/install-and-setup/install/installation-prerequisites/

### Installation

1. This product requires a JDK to run. We support JDK from v17 through v21. Check our [supported JDKs](https://apim.docs.wso2.com/en/latest/install-and-setup/setup/reference/product-compatibility/#tested-operating-systems-and-jdks) documentation  for a complete list.
2. Make sure you have set the `JAVA_HOME` environment variable to point to your JDK. See [this documentation](https://apim.docs.wso2.com/en/latest/install-and-setup/install/installing-the-product/installing-api-m-runtime/#setting-up-java_home) if you need help doing so.
3. Extract the wso2am-4.6.0.zip and go to the 'bin' directory
4. Run the api-manager.sh or api-manager.bat script based on your operating system.
5. Access the respective WSO2 API-M interfaces
   * **API Publisher** web application is running at: https://localhost:9443/publisher.
     You may sign in to the Publisher using the default administrator credentials (username: admin, password: admin).
   * **Developer Portal** web application is running at: - https://localhost:9443/devportal \
     You may sign in to the Developer Portal using the default administrator credentials (username: admin, password: admin).

### Exposing your first API

Follow our [Quick Start guide ](https://apim.docs.wso2.com/en/latest/get-started/quick-start-guide/) to expose and test your first API while getting familiar with our API publisher and developer portal interfaces.

## Reporting product issues

All known issues of WSO2 API Manager are filed at: https://github.com/wso2/product-apim/issues. Please check this list before opening a new issue.

### Opening an issue

Help us make our software better! Submit any bug reports or feature requests through GitHub:  https://github.com/wso2/api-manager/issues.

### Reporting security issues

Please **do not** report security issues via GitHub issues. Instead, follow the [WSO2 Security Vulnerability Reporting Guidelines](https://security.docs.wso2.com/en/latest/security-reporting/vulnerability-reporting-guidelines/).

## Join the community!

- Read our [documentation](https://apim.docs.wso2.com/en/latest/).
- Get help on [Stack Overflow](https://stackoverflow.com/questions/tagged/wso2-api-manager).
- Join the conversation on [Discord](https://discord.gg/wso2).
- Learn more by reading articles from our [library](https://wso2.com/library/).

## Contributing

If you are planning on contributing to the development efforts of WSO2 API Manager, you can do so by checking out the latest development version. The master branch holds the latest unreleased source code.

Please follow the detailed instructions available here: https://apim.docs.wso2.com/en/latest/get-started/contributing-to-docs/


## Commercial support

You can take advantage of a WSO2 on-prem product subscription for the full range of software product benefits needed in your enterprise, like expert support, continuous product updates, vulnerability monitoring, and access to the licensed distribution for commercial use.

To learn more, check [WSO2 Subscription](https://wso2.com/subscription/).

## Can you fill out this survey?

WSO2 wants to learn more about our open source software (OSS) community and your communication preferences to serve you better.
In addition, we may reach out to a small number of respondents to ask additional questions and offer a small gift.

Survey is available at: https://forms.gle/h5q4M3K7vyXba3bK6

--------------------------------------------------------------------------------
(c) Copyright 2012 - 2025 WSO2 Inc.
