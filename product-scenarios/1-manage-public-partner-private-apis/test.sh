#!/bin/bash
#This script execute the tests.

set -o xtrace
TEST_DIR='product-scenarios'
DIR=$2
export DATA_BUCKET_LOCATION=$DIR

mvn clean install

echo "Copying surefire-reports to data bucket"

cp -r 1.1-expose-service-as-rest-api-and-apply-qos/1.1.1-create-rest-api-from-scratch/target/surefire-reports ${DIR}/1.1.1-create-rest-api-from-scratch
ls ${DIR}
