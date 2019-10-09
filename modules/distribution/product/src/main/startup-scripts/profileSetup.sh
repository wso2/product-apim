#!/bin/bash
# ----------------------------------------------------------------------------
#  Copyright 2018 WSO2, Inc. http://www.wso2.org
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

userLocation=`pwd`
pathToApiManagerXML='../repository/conf/api-manager.xml'
pathToDeploymentConfiguration='../repository/conf/deployment.toml'
pathToAxis2XML='../repository/conf/axis2/axis2.xml'
pathToAxis2XMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2.xml.j2'
pathToTenantAxis2XML='../repository/conf/axis2/tenant-axis2.xml'
pathToTenantAxis2XMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2.xml.j2'
pathToAxis2BlockingClientXML='../repository/conf/axis2/axis2_blocking_client.xml'
pathToAxis2BlockingClientXMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_blocking_client.xml.j2'
pathToRegistry='../repository/conf/registry.xml'
pathToRegistryTemplate='../repository/resources/conf/templates/repository/conf/registry.xml.j2'
pathToInboundEndpoints='../repository/deployment/server/synapse-configs/default/inbound-endpoints/'
pathToWebapps='../repository/deployment/server/webapps'
pathToJaggeryapps='../repository/deployment/server/jaggeryapps'
pathToSynapseConfigs='../repository/deployment/server/synapse-configs/default'
pathToAxis2TMXml='../repository/conf/axis2/axis2_TM.xml'
pathToAxis2TMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_TM.xml.j2'
pathToAxis2KMXml='../repository/conf/axis2/axis2_KM.xml'
pathToAxis2KMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_KM.xml.j2'
pathToTenantAxis2KMXml='../repository/conf/axis2/tenant-axis2_KM.xml'
pathToTenantAxis2KMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2_KM.xml.j2'
pathToRegistryTM='../repository/conf/registry_TM.xml'
pathToRegistryTMTemplate='../repository/resources/conf/templates/repository/conf/registry_TM.xml.j2'
pathToAxis2XMLBackup='../repository/conf/axis2/axis2.xml.backup'
pathToAxis2TXmlTemplateBackup='../repository/resources/conf/templates/repository/conf/axis2/axis2.xml.j2.backup'
pathToTenantAxis2XMLBackup='../repository/conf/axis2/tenant-axis2.xml.backup'
pathToTenantAxis2TXmlTemplateBackup='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2.xml.j2.backup'
pathToRegistryBackup='../repository/conf/registryBackup.xml'
pathToRegistryTemplateBackup='../repository/resources/conf/templates/repository/conf/registry.backup'
pathToDeploymentConfigurationBackup='../repository/conf/deployment.toml.backup'
pathToDeploymentTemplates='../repository/resources/conf/deployment-templates'
timestamp=""
cd `dirname "$0"`

timeStamp() {
	timestamp=`date '+%Y-%m-%d %H:%M:%S' | sed 's/\(:[0-9][0-9][0-9]\)[0-9]*$/\1/' `
}

disableDataPublisher(){
	value=`xmllint --xpath '//DataPublisher/Enabled/text()' $pathToApiManagerXML`
	kernel=$(uname -s)
	if [ "$value" = "true" ]
	then
		if [ "$kernel" = "Darwin" ]
		then
			sed -i '' -e "/<DataPublisher>/,/<\/DataPublisher>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		else
			sed -i "/<DataPublisher>/,/<\/DataPublisher>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		fi
		timeStamp
		echo "[${timestamp}] INFO - Disabled the <DataPublisher> from api-manager.xml file"
	fi
}
disableBlockConditionRetriever(){
	value=`xmllint --xpath '//BlockCondition/Enabled/text()' $pathToApiManagerXML`
	kernel=$(uname -s)
	if [ "$value" = "true" ]
	then
		if [ "$kernel" = "Darwin" ]
		then
			sed -i '' -e "/<BlockCondition>/,/<\/BlockCondition>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		else
			sed -i "/<BlockCondition>/,/<\/BlockCondition>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		fi
		timeStamp
		echo "[${timestamp}] INFO - Disabled the <BlockCondition> from api-manager.xml file"
	fi
}

disableJMSConnectionDetails(){
	value=`xmllint --xpath '//JMSConnectionDetails/Enabled/text()' $pathToApiManagerXML`
	kernel=$(uname -s)
	if [ "$value" = "true" ]
	then
		if [ "$kernel" = "Darwin" ]
		then
			sed -i '' -e "/<JMSConnectionDetails>/,/<\/JMSConnectionDetails>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		else
			sed -i "/<JMSConnectionDetails>/,/<\/JMSConnectionDetails>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML	
		fi
		timeStamp
  	    	echo "[${timestamp}] INFO - Disabled the <JMSConnectionDetails> from api-manager.xml file"
	fi
}

disablePolicyDeployer(){
	value=`xmllint --xpath '//PolicyDeployer/Enabled/text()' $pathToApiManagerXML`
	kernel=$(uname -s)
	if [ "$value" = "true" ]
	then
		if [ "$kernel" = "Darwin" ]
		then
			sed -i '' -e "/<PolicyDeployer>/,/<\/PolicyDeployer>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		else
			sed -i "/<PolicyDeployer>/,/<\/PolicyDeployer>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML	
		fi
		timeStamp
		echo "[${timestamp}] INFO - Disabled the <PolicyDeployer> from api-manager.xml file"
	fi
}

disableTransportSenderWS(){
	value=`grep -E '"ws"' $pathToAxis2XML`
	kernel=$(uname -s)
	if [ -n "$value" ]
	then
		value=`grep -E '<!--.*"ws"' $pathToAxis2XML`
		if [ -z "$value" ]
		then
			if [ "$kernel" = "Darwin" ]
			then
				sed -i '' -e '/<transportSender name="ws" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
			else
				sed -i '/<transportSender name="ws" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML	
			fi
			timeStamp
			echo "[${timestamp}] INFO - Disabled the <transportSender name=\"ws\" class=\"org.wso2.carbon.websocket.transport.WebsocketTransportSender\"> from axis2.xml file"
		fi
	fi
}

disableTransportSenderWSS(){
	value=`grep -E '"wss"' $pathToAxis2XML`
	kernel=$(uname -s)
	if [ -n "$value" ]
	then
		value=`grep -E '<!--.*"wss"' $pathToAxis2XML`
		if [ -z "$value" ]
		then
			if [ "$kernel" = "Darwin" ]
			then
				sed -i '' -e '/<transportSender name="wss" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
			else
				sed -i '/<transportSender name="wss" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
			fi
			timeStamp
			echo "[${timestamp}] INFO - Disabled the <transportSender name=\"wss\" class=\"org.wso2.carbon.websocket.transport.WebsocketTransportSender\"> from axis2.xml file"
		fi
	fi
}

removeWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml
		timeStamp
		echo "[${timestamp}] INFO - Removed the WebSocketInboundEndpoint.xml file from $pathToInboundEndpoints"
	fi
}

removeSecureWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml
		timeStamp
		echo "[${timestamp}] INFO - Removed the SecureWebSocketInboundEndpoint.xml file from $pathToInboundEndpoints"
	fi
}

disableIndexingConfiguration(){
	value=`xmllint --xpath 'wso2registry/indexingConfiguration/startIndexing/text()' $pathToRegistry`
	if [ "$value" = "true" ]
	kernel=$(uname -s)
	then
		if [ "$kernel" = "Darwin" ]
		then
			sed -i '' -e "/<indexingConfiguration>/,/<\/indexingConfiguration>/ s/<startIndexing>true<\/startIndexing>/<startIndexing>false<\/startIndexing>/g;" $pathToRegistry
		else
			sed -i "/<indexingConfiguration>/,/<\/indexingConfiguration>/ s/<startIndexing>true<\/startIndexing>/<startIndexing>false<\/startIndexing>/g;" $pathToRegistry	
		fi
		timeStamp
		echo "[${timestamp}] INFO - Disabled the <indexingConfiguration> from registry.xml file"
	fi
}

removeSynapseConfigs(){
	for i in $(find $pathToSynapseConfigs -maxdepth 1 -type d | sed 1d ); do
		rm -r $i
		folder=`basename "$i"`
		timeStamp
		echo "[${timestamp}] INFO - Removed the $folder directory from $pathToSynapseConfigs"
	done

	for i in $(find $pathToSynapseConfigs -maxdepth 1 -type f -not -name 'synapse.xml'); do
		rm -r $i
		file=`basename "$i"`
		timeStamp
		echo "[${timestamp}] INFO - Removed the $file file from $pathToSynapseConfigs"
	done
}

replaceAxis2File(){
  pathToNewAxis2Xml=$1
  if [ -e $pathToAxis2XML ] && [ -e $pathToNewAxis2Xml ]
  then
    mv $pathToAxis2XML $pathToAxis2XMLBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToAxis2XML file as axis2.xml.backup"
		mv $pathToNewAxis2Xml $pathToAxis2XML
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToNewAxis2Xml file as axis2.xml"
	fi
}

replaceTenantAxis2File(){
  pathToNewAxis2Xml=$1
  if [ -e $pathToTenantAxis2XML ] && [ -e $pathToNewAxis2Xml ]
	then
		mv $pathToTenantAxis2XML $pathToTenantAxis2XMLBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToTenantAxis2XML file as tenant-axis2.xml.backup"
		mv $pathToNewAxis2Xml $pathToTenantAxis2XML
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToNewAxis2Xml file as tenant-axis2.xml"
	fi
}

removeAxis2BlockingClientXMLFile(){
  if [ -e $pathToAxis2BlockingClientXML ]
	then
		rm -r $pathToAxis2BlockingClientXML
		timeStamp
		echo "[${timestamp}] INFO - Removed the axis2_blocking_client.xml file from $pathToAxis2BlockingClientXML"
	fi
}

removeAxis2BlockingClientXMLTemplateFile(){
  if [ -e $pathToAxis2BlockingClientXMLTemplate ]
	then
		rm -r $pathToAxis2BlockingClientXMLTemplate
		timeStamp
		echo "[${timestamp}] INFO - Removed the axis2_blocking_client.xml.j2 file from $pathToAxis2BlockingClientXMLTemplate"
	fi
}

replaceRegistryXMLFile(){
    if [ -e $pathToRegistry ] && [ -e $pathToRegistryTM ]
	then
		mv $pathToRegistry $pathToRegistryBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToRegistry file as registryBackup.xml"
		mv $pathToRegistryTM $pathToRegistry
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToRegistryTM file as registry.xml"
	fi
}

replaceAxis2TemplateFile(){
	pathToNewAxis2TemplateXml=$1
	if [ -e $pathToAxis2XMLTemplate ] && [ -e $pathToNewAxis2TemplateXml ]
	then
		mv $pathToAxis2XMLTemplate $pathToAxis2TXmlTemplateBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToAxis2XMLTemplate file as axis2.xml.j2.backup"
		mv $pathToNewAxis2TemplateXml $pathToAxis2XMLTemplate
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToNewAxis2TemplateXml file as axis2.xml.j2"
	fi
}

replaceTenantAxis2TemplateFile(){
	pathToNewAxis2TemplateXml=$1
	if [ -e $pathToTenantAxis2XMLTemplate ] && [ -e $pathToNewAxis2TemplateXml ]
	then
		mv $pathToTenantAxis2XMLTemplate $pathToTenantAxis2TXmlTemplateBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToTenantAxis2XMLTemplate file as tenant-axis2.xml.j2.backup"
		mv $pathToNewAxis2TemplateXml $pathToTenantAxis2XMLTemplate
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToNewAxis2TemplateXml file as tenant-axis2.xml.j2"
	fi
}

replaceRegistryXMLTemplateFile(){
  if [ -e $pathToRegistryTemplate ] && [ -e $pathToRegistryTMTemplate ]
	then
	  mv $pathToRegistryTemplate $pathToRegistryTemplateBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToRegistryTemplate file as registry.backup"
		mv $pathToRegistryTMTemplate $pathToRegistryTemplate
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToRegistryTMTemplate file as registry.xml.j2"
	fi
}

replaceDeploymentConfiguration(){
    profileConfiguration=$pathToDeploymentTemplates/$1.toml
    if [ -e "$pathToDeploymentConfiguration" ] && [ -e "$profileConfiguration" ]
    then
        mv "$pathToDeploymentConfiguration" "$pathToDeploymentConfigurationBackup"
        timeStamp
        echo "[${timestamp}] INFO - Renamed the existing $pathToDeploymentConfiguration file as deployment.toml.backup"
        cp "$profileConfiguration" "$pathToDeploymentConfiguration"
        timeStamp
        echo "[${timestamp}] INFO - Renamed the existing $profileConfiguration file as deployment.toml"
    fi
}

#main
case $1 in
	-Dprofile=api-key-manager)
		echo "Starting to optimize API Manager for the Key Manager profile"
		replaceAxis2File $pathToAxis2KMXml
		replaceTenantAxis2File $pathToTenantAxis2KMXml
		removeAxis2BlockingClientXMLFile
		removeAxis2BlockingClientXMLTemplateFile
		replaceAxis2TemplateFile $pathToAxis2KMXmlTemplate
		replaceTenantAxis2TemplateFile $pathToTenantAxis2KMXmlTemplate
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		disableBlockConditionRetriever
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		replaceDeploymentConfiguration api-key-manager
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' -o -name 'throttle#data#v*.war' -o -name 'api#identity#consent-mgt#v*.war' \) ); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=`basename $file .war`
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# removing jaggeryapps which are not required for this profile
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=api-publisher)
		echo "Starting to optimize API Manager for the API Publisher profile"
		disableJMSConnectionDetails
		disableTransportSenderWS
		disableTransportSenderWSS
		disableBlockConditionRetriever
		replaceDeploymentConfiguration api-publisher
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' -o -name 'api#am#publisher#v*.war' \) ); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=`basename $file .war`
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# removing jaggeryapps which are not required for this profile
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d -not \( -name 'admin' -o -name 'publisher' \) | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=api-devportal)
		echo "Starting to optimize API Manager for the Developer Portal profile"
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		disableBlockConditionRetriever
		replaceDeploymentConfiguration api-devportal
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' -o -name 'api#am#store#v*.war' \) ); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=`basename $file .war`
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# removing jaggeryapps which are not required for this profile
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d -not -name 'devportal'| sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
        ;;
	-Dprofile=traffic-manager)
		echo "Starting to optimize API Manager for the Traffic Manager profile"
		replaceAxis2File $pathToAxis2TMXml
		replaceRegistryXMLFile
		replaceAxis2TemplateFile $pathToAxis2TMXmlTemplate
		replaceRegistryXMLTemplateFile
		disableIndexingConfiguration
		replaceDeploymentConfiguration traffic-manager
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=`basename $file .war`
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# removing jaggeryapps which are not required for this profile
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=gateway-worker)
		echo "Starting to optimize API Manager for the Gateway worker profile"
		disablePolicyDeployer
		disableIndexingConfiguration
     	replaceDeploymentConfiguration gateway-worker
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not -name 'am#sample#pizzashack#v*.war'); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=`basename $file .war`
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# removing jaggeryapps which are not required for this profile
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	*)
		echo "Profile is not specified properly, please try again"
		cd $userLocation
		exit
esac

echo Finished the optimizations
cd $userLocation
