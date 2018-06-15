================================================================================
                          WSO2 API Manager Samples
================================================================================

Introduction
=============

WSO2 API Manager ships with a collection of basic samples that demonstrate its
core functionality. You will find all the relevant configurations, scripts and
instructions required to run these samples in the subdirectories of the 'samples'
top level directory. As of now, the following samples are available and released with
the API Manager binary distribution.

1. WikipediaAPI -
   A simple REST API based on Wikipedia's web API access to wiki features, data, and meta-data over HTTP.

2. Billing Sample
    A sample to generate bill for API usage of consumers. 

3. PizzaShack sample
   A sample to demonstrate use of REST APIs.

These samples are packaged according to the following file hierarchy under the
'samples' directory.

samples
├── Billing
│   ├── API_Manager_Analytics.tbox
│   ├── billing-conf.xml
│   └── README.txt
├── Data
│   ├── build.xml
│   └── UserPopulator.sql
├── PizzaShack
│   ├── pizza-shack-api
│   ├── pizza-shack-web
│   ├── pom.xml
│   └── README.txt
└── WikipediaAPI
    ├── APIPopulator.bat
    ├── APIPopulator.sh
    └── README.txt



Requirements
=============

1. JDK 1.6.0_23 or higher
2. Apache ANT 1.7 or higher
3. A HTTP client tool such as cURL
4. A JavaScript compatible web browser
5. An active Internet connection


Setting Up the Samples
======================

Each sample requires you to first start the API Manager. 

To create the sample configurations in the API Manager, each sample provides a
simple shell script (named APIPopulator.sh). Executing this script will create the
sample API in the Publisher and promote it to the API Store. If you login to
API Publisher (http://localhost:9763/publisher) or API Store (http://localhost:9763/store)
you will be able to consume sample API listed on both portals. Through the API Store, you can
subscribe to the sample API, obtain a key and invoke it.

Refer the README files in each of the sample subdirectories for more detailed
instructions on how to run the samples.


Invoking the Sample APIs
=========================

All samples are based on live APIs, openly available on the web. Therefore when
invoking the APIs, make sure your machine has an active Internet connection. To send
the sample requests, you may use a client tool such as cUrl. The necessary cUrl commands
are given in sample README files. Alternatively you may use any other HTTP client tool
that allows you to send a customized 'Authorization' header.


After Running the Samples
=========================

Samples only scratch the surface when it comes to demonstrating the capabilities of
WSO2 API Manager. Therefore it's highly recommended to try out the API Publisher and
API Store manually to understand its full potential. Also go through our documentation
at https://docs.wso2.com/display/AM200/WSO2+API+Manager+Documentation to learn
more about the product and its features.
