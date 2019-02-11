#!/bin/bash

# Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#=== FUNCTION ==================================================================
# NAME: generate_code_coverage
# DESCRIPTION: Genaret code coverage reports.
# PARAMETER 1: Path to input directory
# PARAMETER 2: Path to output directory
#===============================================================================

#set -o xtrace

VAR_TEST_PLAN_ID=""
VAR_IS_TESTGRID=""
VAR_TINKERER_ENDPOINT=""
VAR_TINKERER_USERNAME=""
VAR_TINKERER_PASSWORD=""
VAR_REM_DIR=""

function generate_code_coverage(){
    INPUT_DIR=$1
    OUTPUT_DIR=$2
    HOME=`pwd`

    #=============== Read Deployment.property file ===============================================

    VAR_TEST_PLAN_ID=$(get_prop 'TEST_PLAN_ID')
    VAR_IS_TESTGRID=$(get_prop 'IS_TESTGRID')
    VAR_TINKERER_ENDPOINT=$(echo $(get_prop 'TESTGRID_TINKERER_ENDPOINT') | sed 's/\\//g')
    VAR_TINKERER_USERNAME=$(get_prop 'TESTGRID_TINKERER_USERNAME')
    VAR_TINKERER_PASSWORD=$(get_prop 'TESTGRID_TINKERER_PASSWORD')
    VAR_REM_DIR=$(get_prop 'RemoteProductDir')
    user=ubuntu

    #=============== Code Coverage Report Generation ===========================================

    #Copy files to the client side through tinkerer
    if [ "$VAR_IS_TESTGRID" = True ] || [ "$VAR_IS_TESTGRID" = true ]; then

        #Get IP address
        echo "Get the IP address of client"
        ipaddress=$(echo `curl http://169.254.169.254/latest/meta-data/public-ipv4`)

        #Get Active Agents for the defined test plan
        mkdir -p "${HOME}"/code-coverage/resources
        echo $(curl -X GET "${VAR_TINKERER_ENDPOINT}"/api/test-plan/"${VAR_TEST_PLAN_ID}"/agents \
        -u "${VAR_TINKERER_USERNAME}":"${VAR_TINKERER_PASSWORD}") > "${HOME}"/code-coverage/resources/agentsList.json

        python -m json.tool "${HOME}"/code-coverage/resources/agentsList.json | \
        awk -F'"' '/instanceName/{print $4}' > "${HOME}"/code-coverage/resources/agent_name_list.txt

        i=0
        while read line
        do
            agent_name[$i]="$line"

            #Stop the wso2server.sh
            echo "Stop the server in node ${agent_name[i]}"

            echo $(tinkerer_curl_commands ${agent_name[i]} "$VAR_REM_DIR/bin/wso2server.sh\tstop")

            sleep 30

            #Zip the jacoco folder
            echo "Zip the jacoco folder in node ${agent_name[i]}"

            echo $(tinkerer_curl_commands ${agent_name[i]} "cd\t$VAR_REM_DIR/repository/logs/jacoco;\tzip\t-r\tjacoco.zip\t.")

            #Generate key
            echo "Generate a key in node ${agent_name[i]}"

            echo $(tinkerer_curl_commands ${agent_name[i]} "mkdir\t-p\t$VAR_REM_DIR/../keys")
            echo $(tinkerer_curl_commands ${agent_name[i]} "ssh-keygen\t-b\t2048\t-t\trsa\t-f\t$VAR_REM_DIR/../keys/deploy.key\t-q\t-N\t''")

            #Add key to authorized_keys
            echo "Add key of node ${agent_name[i]} to the authorized_keys in client side"

            echo `tinkerer_curl_commands ${agent_name[i]} "cat\t$VAR_REM_DIR/../keys/deploy.key.pub"` | \
            sed -nE 's/.*"response":"(.*)","completed.*/\1/p' >> /home/ubuntu/.ssh/authorized_keys

            #Copy file to client side
            echo "Copy file to the client side from ${agent_name[i]}"
            mkdir -p ${HOME}/code-coverage/resources/instance$((i+1))

            #Give permission to .key file
            tinkerer_curl_commands ${agent_name[i]} "sudo\tchmod\t400\t$VAR_REM_DIR/../keys/deploy.key"

            echo $(tinkerer_curl_commands ${agent_name[i]} \
            "scp\t-o\tStrictHostKeyChecking=no\t-i\t$VAR_REM_DIR/../keys/deploy.key\t$VAR_REM_DIR/repository/logs/jacoco/jacoco.zip\t$user@$ipaddress:$HOME/code-coverage/resources/instance$((i+1))")
            sleep 30

            echo "Extract the copied coverage artifact file of ${agent_name[i]}"
            unzip ${HOME}/code-coverage/resources/instance$((i+1))/jacoco.zip \
            -d ${HOME}/code-coverage/resources/instance$((i+1))/jacoco

            sleep 40

            i=$((i+1))

        done < "${HOME}"/code-coverage/resources/agent_name_list.txt

        #Execute code-coverage POM and generate coverage reports
        echo "Generate coverage reports from coverage artifacts"
        mvn clean install -f ${HOME}/code-coverage/pom.xml

        #Copy Code Coverage Reports
        echo "Copy code coverage reports to the output directory"
        cp -r ${HOME}/code-coverage/target/scenario-code-coverage ${OUTPUT_DIR}

        #Copy Code Coverage Artifacts to the output directory
        echo "Copy code coverage artifacts to the output directory"
        mkdir ${HOME}/code-coverage/resources/code-coverage-artifacts
        cp -r ${HOME}/code-coverage/resources/instance* ${HOME}/code-coverage/resources/code-coverage-artifacts
        cp -r ${HOME}/code-coverage/resources/code-coverage-artifacts ${OUTPUT_DIR}

    fi
}

#=== FUNCTION ==================================================================
# NAME: testgrid tinkerer curl commands
# DESCRIPTION: Curl command to perform shell commands on server side through testgrid tinkerer
# PARAMETER 1: agent_name
# PARAMETER 2: shell_command which need to execute on server sides
#===============================================================================
function tinkerer_curl_commands(){

    agent_name=$1
    shell_command=$2

    curl -X POST "${VAR_TINKERER_ENDPOINT}"/api/test-plan/"${VAR_TEST_PLAN_ID}"/agent/"${agent_name}"/operation \
           -H "content-type: application/json" \
           -d "{\"code\":\"SHELL\", \"request\":\"$shell_command\"}" \
           -u "${VAR_TINKERER_USERNAME}":"${VAR_TINKERER_PASSWORD}"

     sleep 30
}

#=== FUNCTION ==================================================================
# NAME: get_prop
# DESCRIPTION: Retrieve specific property from deployment.properties file
# PARAMETER 1: property_value
#===============================================================================
function get_prop {
    local prop=$(grep -w "${1}" "${INPUT_DIR}/deployment.properties" | cut -d'=' -f2)
    echo $prop
}
