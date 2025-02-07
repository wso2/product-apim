# AI based Recommendation System for the API Store

## What is WSO2 API Manager?

WSO2 API Manager is a full lifecycle API Management solution which has an API Gateway and a Microgateway. See more on https://wso2.com/api-management/


## What is API Recommendation System?

WSO2 API Manager Store Portal is a marketplace for APIs. Developers can login to the portal to choose and subscribe which APIs to leverage in their applications. Currently, the developer has to pick the APIs of interest by browsing through the APIs or searching with a particular information associated with the API. API Recommendation System recommends APIs which can be beneficial for the developer using AI technologies. 


# Quick Start Guide

## Prerequisites

1. Install Python 3.6 or higher.
2. Install pip version 3 if not already installed.
    ```
    $ sudo apt install python3-pip
    ```
3. Install the required python packages, by running the following command in the project home directory.
    ```
    $ pip3 install -r requirements.txt
    ```
4. Run the following command to download the spacy model.
    ```
    $ python3 -m spacy download en_core_web_lg
    $ python3 -m spacy link en_core_web_lg en
    ```
5. Create a mongodb instance with the port: 27017
9. Change the configurations at the `<project-home-directory>/resources/conf/config.yaml` file as needed.
10. To start the recommendation server, run `<project-home-directory>/bin/recommendation-server.sh`
11. To stop the recommendation server, run `<project-home-directory>/bin/recommendation-server.sh stop`

    

   
