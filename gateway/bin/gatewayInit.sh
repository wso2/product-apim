#!/bin/bash

#if [ "$OFFLINE_GATEWAY_REPO" = " " ]; then
#    bin/ballerina run service services.bsz org/wso2/carbon/apimgt/gateway;
#else
#    bin/ballerina run service microservices.bsz microgateway;
#fi

#GW_HOME="$PWD"


export GW_HOME="$PWD";
bin/ballerina run service microservices.bsz microgateway;

#DIR="microgateway"

#if [ "$(ls -A $DIR)" ]; then
#     echo "Take action $DIR is not Empty"
#     bin/ballerina run service microservices.bsz microgateway;
#else
#    echo "$DIR is Empty"
#    bin/ballerina run service services.bsz org/wso2/carbon/apimgt/gateway;
#fi