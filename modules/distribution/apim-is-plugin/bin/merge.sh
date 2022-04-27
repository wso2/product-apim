#!/bin/bash
# ------------------------------------------------------------------------
#
# Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
#
# This software is the property of WSO2 Inc. and its suppliers, if any.
# Dissemination of any information or reproduction of any material contained
# herein is strictly forbidden, unless permitted by WSO2 in accordance with
# the WSO2 Commercial License available at http://wso2.com/licenses. For specific
# language governing the permissions and limitations under this license,
# please see the license as well as any agreement you’ve entered into with
# WSO2 governing the purchase of this software and any associated services.
#
# ------------------------------------------------------------------------

# merge.sh script copy the APIM-IS-PLUGIN artifacts on top of WSO2 IS
#
# merge.sh <IS-HOME>

IS_HOME=$1

# set APIM-IS-PLUGIN_HOME home
cd ../
APIM_IS_PLUGIN_HOME=$(pwd)
echo "[INFO] WSO2 APIM-IS Plugin home is: ${APIM_IS_PLUGIN_HOME}"

# set product home
if [ "${IS_HOME}" == "" ];
  then
    echo "[ERROR] IS_HOME is not specified, please try again with correct arguments";
    exit 2;
fi
echo "[INFO] Product IS home is: ${IS_HOME}"

# validate product home
if [ ! -d "${IS_HOME}/repository/components" ]; then
  echo "[ERROR] Specified product path is not a valid carbon product path";
  exit 2;
else
  echo "[INFO] Valid carbon product path";
fi

# create the apim-IS-plugin folder in product home, if not exist
APIM_IS_PLUGIN_AUDIT="${IS_HOME}"/apim-is-plugin
APIM_IS_PLUGIN_AUDIT_BACKUP="${APIM_IS_PLUGIN_AUDIT}"/backup
if [ ! -d  "${APIM_IS_PLUGIN_AUDIT}" ]; then
   mkdir -p "${APIM_IS_PLUGIN_AUDIT_BACKUP}"
   mkdir -p "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/webapps
   mkdir -p "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins
   echo "[INFO] APIM-IS-PLUGIN audit folder [""${APIM_IS_PLUGIN_AUDIT}""] is created";
else
   echo "[INFO] APIM-IS-PLUGIN audit folder is present at [""${APIM_IS_PLUGIN_AUDIT}""]";
   if [ -d  "${APIM_IS_PLUGIN_AUDIT_BACKUP}" ]; then
     rm -rf "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/webapps/*
     rm -rf "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins/*
   fi
fi

echo "[INFO] Backup original product files.."
cp -R "${IS_HOME}"/repository/deployment/server/webapps/keymanager-operations/ "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/webapps/keymanager-operations 2>/dev/null
cp "${IS_HOME}"/repository/deployment/server/webapps/keymanager-operations.war "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/webapps 2>/dev/null
cp "${IS_HOME}"/repository/components/dropins/wso2is.key.manager.core-1.0.16*.jar "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins 2>/dev/null
cp "${IS_HOME}"/repository/components/dropins/wso2is.key.manager.core_1.0.16*.jar "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins 2>/dev/null
cp "${IS_HOME}"/repository/components/dropins/wso2is.notification.event.handlers-1.0.16*.jar "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins 2>/dev/null
cp "${IS_HOME}"/repository/components/dropins/wso2is.notification.event.handlers_1.0.16*.jar "${APIM_IS_PLUGIN_AUDIT_BACKUP}"/dropins 2>/dev/null

echo "[INFO] Clean up extracted webapps.."
rm -rf "${IS_HOME}"/repository/deployment/server/webapps/keymanager-operations/

echo "[INFO] Clean up keymanager-operations.war"
rm -f "${IS_HOME}"/repository/deployment/server/webapps/keymanager-operations.war

echo "[INFO] Clean up key-manager jars from dropins.."
rm -f "${IS_HOME}"/repository/components/dropins/wso2is.key.manager.core-1.0.16*.jar
rm -f "${IS_HOME}"/repository/components/dropins/wso2is.notification.event.handlers-1.0.16*.jar
rm -f "${IS_HOME}"/repository/components/dropins/wso2is.key.manager.core_1.0.16*.jar
rm -f "${IS_HOME}"/repository/components/dropins/wso2is.notification.event.handlers_1.0.16*.jar

echo "[INFO] Copying APIM Key Manager connector artifacts to dropins"
echo "================================================"
cp -r "${APIM_IS_PLUGIN_HOME}"/dropins/* "${IS_HOME}/repository/components/dropins"/

echo "[INFO] Copying APIM Key Manager connector artifacts to webapps"
echo "================================================"
cp -r "${APIM_IS_PLUGIN_HOME}"/webapps/keymanager-operations.war "${IS_HOME}/repository/deployment/server/webapps"/

echo "[INFO] Completed!"
echo "$(date)" - "$USER" - "WSO2 APIM-IS Plugin 4.1.0" | tee -a "${APIM_IS_PLUGIN_AUDIT}"/merge_audit.log >/dev/null