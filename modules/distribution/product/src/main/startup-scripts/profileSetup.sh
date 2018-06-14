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

pathToApiManagerXML='../repository/conf/api-manager.xml'
pathToAxis2XML='../repository/conf/axis2/axis2.xml'
pathToRegistry='../repository/conf/registry.xml'
pathToInboundEndpoints='../repository/deployment/server/synapse-configs/default/inbound-endpoints/'
pathToWebapps='../repository/deployment/server/webapps'
pathToJaggeryapps='../repository/deployment/server/jaggeryapps'
pathToSynapseConfigs='../repository/deployment/server/synapse-configs/default'
timestamp=""

timeStamp() {
	timestamp=`date '+%Y-%m-%d %H:%M:%S'`
}

disableDataPublisher(){
	value=`xmllint --xpath '//DataPublisher/Enabled/text()' $pathToApiManagerXML`
	if [ "$value" = "true" ]
	then
		sed -i "/<DataPublisher>/,/<\/DataPublisher>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <DataPublisher> from api-manager.xml file"
	fi
}

disableJMSConnectionDetails(){
	value=`xmllint --xpath '//JMSConnectionDetails/Enabled/text()' $pathToApiManagerXML`
	if [ "$value" = "true" ]
	then
		sed -i "/<JMSConnectionDetails>/,/<\/JMSConnectionDetails>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <JMSConnectionDetails> from api-manager.xml file"
	fi
}

disablePolicyDeployer(){
	value=`xmllint --xpath '//PolicyDeployer/Enabled/text()' $pathToApiManagerXML`
	if [ "$value" = "true" ]
	then
		sed -i "/<PolicyDeployer>/,/<\/PolicyDeployer>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <PolicyDeployer> from api-manager.xml file"
	fi
}

disableTransportSenderWS(){
	value=`grep -E '<!--.*"ws"' $pathToAxis2XML`
	if [ -z "$value" ]
	then
		sed -i '/<transportSender name="ws" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <transportSender name=\"ws\" class=\"org.wso2.carbon.websocket.transport.WebsocketTransportSender\"> from axis2.xml file"
	fi
}

disableTransportSenderWSS(){
	value=`grep -E '<!--.*"wss"' $pathToAxis2XML`
	if [ -z "$value" ]
	then
		sed -i '/<transportSender name="wss" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <transportSender name=\"wss\" class=\"org.wso2.carbon.websocket.transport.WebsocketTransportSender\"> from axis2.xml file"
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
	then
		sed -i "/<indexingConfiguration>/,/<\/indexingConfiguration>/ s/<startIndexing>true<\/startIndexing>/<startIndexing>false<\/startIndexing>/g;" $pathToRegistry
		timeStamp
  		echo "[${timestamp}] INFO - Disabled the <indexingConfiguration> from registry.xml file"
	fi
}

removeSynapseConfigs(){
	for i in $(find $pathToSynapseConfigs -maxdepth 1 -type d -or -type f -not -name 'synapse.xml' | sed 1d ); do
		rm -r $i
		file=`basename "$i"`
		timeStamp
		echo "[${timestamp}] INFO - Removed the $file from $pathToSynapseConfigs"
	done
}

#main
case $1 in
	-Dprofile=api-key-manager)
		echo "Starting to optimize API Manager for the Key Manager profile"
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		# remove unnecessary webapps
		for i in $(find $pathToWebapps -type f -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint.war' -o -name 'oauth2.war' -o -name 'throttle#data#v*.war' -o -name 'api#identity#consent-mgt#v*.war' \) ); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
	  		echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=$(echo "$file" | cut -f 1 -d '.')
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# remove unnecessary jaggeryapps
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=api-publisher)
		echo "Starting to optimize API Manager for the API Publisher profile"
		disableDataPublisher
		disableJMSConnectionDetails
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		# remove unnecessary webapps
		for i in $(find $pathToWebapps -type f -not -name 'api#am#publisher#v*.war'); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
	  		echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=$(echo "$file" | cut -f 1 -d '.')
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# remove unnecessary jaggeryapps
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d -not \( -name 'admin' -o -name 'publisher' \) | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=api-store)
		echo "Starting to optimize API Manager for the Developer Portal (API Store) profile"
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		# remove unnecessary webapps
		for i in $(find $pathToWebapps -type f -not -name 'api#am#store#*.war'); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
	  		echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=$(echo "$file" | cut -f 1 -d '.')
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# remove unnecessary jaggeryapps
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d -not -name 'store'| sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	-Dprofile=traffic-manager)
		echo "Starting to optimize API Manager for the Traffic Manager profile"
		disableTransportSenderWS
		disableTransportSenderWSS
		disableIndexingConfiguration
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		# remove unnecessary webapps
		for i in $(find $pathToWebapps -type f ); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
	  		echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=$(echo "$file" | cut -f 1 -d '.')
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# remove unnecessary jaggeryapps
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
		# remove unnecessary webapps
		for i in $(find $pathToWebapps -type f -not -name 'am#sample#pizzashack#v*.war'); do
			rm -r $i
			file=`basename "$i"`
			timeStamp
	  		echo "[${timestamp}] INFO - Removed the $file file from ${pathToWebapps}"
			folder=$(echo "$file" | cut -f 1 -d '.')
			if [ -d ${pathToWebapps}/$folder ]
			then
				rm -r ${pathToWebapps}/$folder
				timeStamp
				echo "[${timestamp}] INFO - Removed $folder directory from ${pathToWebapps}"
			fi
		done
		# remove unnecessary jaggeryapps
		for i in $(find ${pathToJaggeryapps} -maxdepth 1 -type d | sed 1d); do
			rm -r $i
			folder=`basename "$i"`
			timeStamp
			echo "[${timestamp}] INFO - Removed $folder directory from ${pathToJaggeryapps}"
		done
		;;
	*)
		echo "Profile is not specifed properly, please try again"
		exit
esac

echo Finished the optimizations
