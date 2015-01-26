================================================================================
                          WSO2 API Manager Samples
================================================================================

Introduction
=============

WSO2 API Manager ships with a collection of basic samples that demonstrate its
core functionality. You will find all the relevant configurations, scripts and
instructions required to run these samples in the subdirectories of the 'samples'
top level directory. As of now following samples are available and released with
the API Manager binary distribution.

1. TwitterSearch -
   A simple Atom API based on Twitter's on-line search functionality

2. YahooPlaceFinder -
   A simple REST API based on Yahoo's PlaceFinder API

3. YoutubeFeeds -
   A simple Atom API based on Yahoo's GData API

4. Billing Sample
    A sample to generate bill for API usage of consumers. 

5. BPS workflow sample
   A sample to demo how to connect external BPS work flow for user adding action

6. PizzaShack sample
   A sample to demo use of rest API.

7. Data sample
   This is a sample script whic adds users and apps to system.


These samples are packaged according to the following file hierarchy under the
'samples' directory.

samples
├── Billing
│   ├── API_Manager_Analytics.tbox
│   ├── billing-conf.xml
│   ├── README.txt
│   └── README.txt~
├── BPSWorkFlow
│   └── UserCreation
├── Data
│   ├── build.xml
│   └── UserPopulator.sql
├── PizzaShack
│   ├── pizza-shack-api
│   ├── pizza-shack-web
│   ├── pom.xml
│   └── README.txt
├── TwitterSearch
│   ├── APIPopulator.bat
│   ├── APIPopulator.sh
│   └── README.txt
├── YahooPlaceFinder
│   ├── APIPopulator.bat
│   ├── APIPopulator.sh
│   └── README.txt
└── YoutubeFeeds
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

You will find 7 subdirectories containing the instructions to run the above samples.
Each sample requires you to first install the API Manager and setup the installation
by running the 'ant' command from the 'bin' directory. Then you need to start the
API Manager once, shut it down and run the 'ant' command once more from the
samples/Data directory. This will create two user accounts (provider1 and subscriber1)
which can be used to login to the API Publisher and API Store. This basic setup
procedure needs to be performed only once per installation. If you ever want to try out
multiple samples on the same installation, performing the above procedure once is
sufficient.

To create the sample configurations in the API Manager, each sample provides a
simple shell script (named APIPopulator.sh). Executing this script will create the
sample API in the Publisher and promote it to the API Store. If you login to
API Publisher (http://localhost:9763/publisher) or API Store (http://localhost:9763/store)
you will be able to sample API listed on both portals. Through the API Store, you can
subscribe to the sample API, obtain a key and invoke it.

Refer the README files in each of the sample subdirectories for more detailed
instructions on how to run the samples.


Invoking the Sample APIs
=========================

All 7 samples are based on live APIs, openly available on the web. Therefore when
invoking the APIs, make sure your machine has an active Internet connection. To send
the sample requests, you may use a client tool such as cUrl. The necessary cUrl commands
are given in sample README files. Alternatively you may use any other HTTP client tool
that allows you to send a customized 'Authorization' header.


After Running the Samples
=========================

Samples only scratch the surface when it comes to demonstrating the capabilities of
WSO2 API Manager. Therefore it's highly recommended to try out the API Publisher and
API Store manually to understand its full potential. Also go through our documentation
at http://docs.wso2.org/wiki/display/AM120/WSO2+API+Manager+Documentation to learn
more about the product and its features.
