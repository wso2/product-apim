#!/bin/bash
# ----------------------------------------------------------------------------
#  Copyright 2017 WSO2, Inc. http://www.wso2.org
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

echo "API Manage sample business scenarios sample data populator."
echo
echo "1 - Managing Public, Partner vs Private APIs"
echo "2 - Ownership, permission and collaborative API development"
echo "3 - Developer Optimized APIs Development"
#echo "4 - API Security"
echo "7 - API Lifecycle Management"
echo "8 - API Versioning"
echo "9 - API Governance"
echo "10 - API Rate Limiting"
echo "11 - API Monitization"
echo

set -e

while [ -z $sample_number ]
do
  echo "Enter the sample number from above listed samples: "
  read -r sample_number
done

if [ $sample_number == '3' ] || [ $sample_number == '11' ]
then
    echo "Please refer the README.md file in [APIM_HOME]/sample-scenarios/scenario"$sample_number" directory."
else
    echo "This script will deploy a sample backend run the sample"$sample_number" scenario."

    echo "Running Sample"$sample_number"..."

    java -jar $(ls scenario$sample_number/org.wso2.carbon.apimgt.samples.sample$sample_number-*)
    echo $'\e[1;32m'"DONE!"$'\e[0m'
fi
