#!/bin/bash
#This script execute the tests.

set -o xtrace
TEST_DIR='product-scenarios'
DIR=$2
export DATA_BUCKET_LOCATION=$DIR

mvn clean install

echo "Copying surefire-reports to data bucket"

cp -r 1.1-manage-api-versions/1.1.1-create-new-api-version/target/surefire-reports ${DIR}
ls ${DIR}