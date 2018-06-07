#!/bin/bash

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
  	echo "[${timestamp}] INFO - Disable the DataPublisher from api-manager.xml"
	elif [ "$value" = "false" ]
	then
		timeStamp
  	echo "[${timestamp}] INFO - DataPublisher from api-manager.xml is already configured"
	fi
}

disableJMSConnectionDetails(){
	value=`xmllint --xpath '//JMSConnectionDetails/Enabled/text()' $pathToApiManagerXML`
	if [ "$value" = "true" ]
	then
		sed -i "/<JMSConnectionDetails>/,/<\/JMSConnectionDetails>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		timeStamp
  	echo "[${timestamp}] INFO - Disable the JMSConnectionDetails from api-manager.xml"
	elif [ "$value" = "false" ]
	then
		timeStamp
  	echo "[${timestamp}] INFO - JMSConnectionDetails from api-manager.xml is already configured"
	fi
}

disablePolicyDeployer(){
	value=`xmllint --xpath '//PolicyDeployer/Enabled/text()' $pathToApiManagerXML`
	if [ "$value" = "true" ]
	then
		sed -i "/<PolicyDeployer>/,/<\/PolicyDeployer>/ s/<Enabled>true<\/Enabled>/<Enabled>false<\/Enabled>/g;" $pathToApiManagerXML
		timeStamp
  	echo "[${timestamp}] INFO - Disable the PolicyDeployer from api-manager.xml"
	elif [ "$value" = "false" ]
	then
		timeStamp
  	echo "[${timestamp}] INFO - PolicyDeployer from api-manager.xml is already configured"
	fi
}

disableTransportSenderWS(){
	value=`grep -E '<!--.*"ws"' $pathToAxis2XML`
	if [ -z "$value" ]
	then
		sed -i '/<transportSender name="ws" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
		timeStamp
  	echo "[${timestamp}] INFO - Disable the TransportSenderWS from axis2.xml"
	else
		timeStamp
		echo "[${timestamp}] INFO - TransportSenderWS from axis2.xml is already commented"
	fi
}

disableTransportSenderWSS(){
	value=`grep -E '<!--.*"wss"' $pathToAxis2XML`
	if [ -z "$value" ]
	then
		sed -i '/<transportSender name="wss" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender">/,/<\/transportSender>/s/\(.*\)/<!--\1-->/' $pathToAxis2XML
		timeStamp
  	echo "[${timestamp}] INFO - Disable the TransportSenderWSS from axis2.xml"
	else
		timeStamp
  	echo "[${timestamp}] INFO - TransportSenderWSS from axis2.xml is already commented"
	fi
}

removeWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml
		timeStamp
  	echo "[${timestamp}] INFO - Removed WebSocketInboundEndpoint xml file"
	else
		timeStamp
  	echo "[${timestamp}] INFO - WebSocketInboundEndpoint xml file is already removed"
	fi
}

removeSecureWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml
		timeStamp
  	echo "[${timestamp}] INFO - Removed SecureWebSocketInboundEndpoint xml file"
	else
		timeStamp
  	echo "[${timestamp}] INFO - SecureWebSocketInboundEndpoint xml file is already removed"
	fi
}

disableIndexingConfiguration(){
	value=`xmllint --xpath 'wso2registry/indexingConfiguration/startIndexing/text()' $pathToRegistry`
	if [ "$value" = "true" ]
	then
		sed -i "/<indexingConfiguration>/,/<\/indexingConfiguration>/ s/<startIndexing>true<\/startIndexing>/<startIndexing>false<\/startIndexing>/g;" $pathToRegistry
		timeStamp
  	echo "[${timestamp}] INFO - Disable the indexingConfiguration from registry.xml"
	elif [ "$value" = "false" ]
	then
		timeStamp
  	echo "[${timestamp}] INFO - IndexingConfiguration from registry.xml is already configured"
	fi
}

removeSynapseConfigs(){
	if [ "$(find ${pathToSynapseConfigs} -type d -or -type f -not -name 'synapse.xml' | wc -l)" -gt 1 ]
	then
		if [ "$(find ${pathToSynapseConfigs} -type d | wc -l)" -gt 1 ]
		then
			rm -R ${pathToSynapseConfigs}/*/
		fi

		if [ -e ${pathToSynapseConfigs}/registry.xml ]
		then
			rm -r ${pathToSynapseConfigs}/registry.xml
		fi
		timeStamp
  	echo "[${timestamp}] INFO - Removed the SynapseConfigs files"
	else
		timeStamp
  	echo "[${timestamp}] INFO - SynapseConfigs are already removed"
	fi
}

#main
case $1 in
	-Dprofile=api-key-manager)
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		#remove unnecessary webapps
		if [ "$(find $pathToWebapps -type f -not \( -name 'client-registration#v0.12.war' -o -name 'authenticationendpoint.war' -o -name 'oauth2.war' -o -name 'throttle#data#v1.war' -o -name 'api#identity#consent-mgt#v1.0.war' \) | wc -l)" -gt 0 ]
		then
			find $pathToWebapps -type f -not \( -name 'client-registration#v0.12.war' -o -name 'authenticationendpoint.war' -o -name 'oauth2.war' -o -name 'throttle#data#v1.war' -o -name 'api#identity#consent-mgt#v1.0.war' \)  -delete
			timeStamp
	  	echo "[${timestamp}] INFO - Removed the unnecessary webapps for key-manager profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary webapps for key-manager profile are already removed"
		fi
		#remove jaggeryapps
		if [ -d $pathToJaggeryapps ]
		then
			rm -r $pathToJaggeryapps
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary jaggeryapps for key-manager profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary jaggeryapps for key-manager profile are already removed"
		fi
		;;
	-Dprofile=api-publisher)
		disableDataPublisher
		disableJMSConnectionDetails
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		#remove unnecessary webapps
		if [ "$(find $pathToWebapps -type f -not -name 'api#am#publisher#v0.12.war' | wc -l)" -gt 0 ]
		then
			find $pathToWebapps -type f -not -name 'api#am#publisher#v0.12.war' -delete
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary webapps for publisher profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary webapps for publisher profile are already removed"
		fi
		#remove unnecessary Jaggeryapps
		if [ -d ${pathToJaggeryapps}/store ]
		then
			rm -r ${pathToJaggeryapps}/store
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary jaggeryapps for publisher profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary jaggeryapps for publisher profile are already removed"
		fi
		;;
	-Dprofile=api-store)
		disableDataPublisher
		disableJMSConnectionDetails
		disablePolicyDeployer
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		#remove unnecessary webapps
		if [ "$(find $pathToWebapps -type f -not -name 'api#am#store#v0.12.war' | wc -l)" -gt 0 ]
		then
			find $pathToWebapps -type f -not -name 'api#am#store#v0.12.war' -delete
			timeStamp
	  	echo "[${timestamp}] INFO - Removed the unnecessary webapps for store profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary webapps for store profile are already removed"
		fi
		#remove unnecessary jaggeryapps
		if [ -d ${pathToJaggeryapps}/admin ]
		then
			rm -r ${pathToJaggeryapps}/admin
			timeStamp
	  	echo "[${timestamp}] INFO - Removed admin Jaggeryapp for store profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Admin jaggeryapp for store profile is already removed"
		fi
		if [ -d ${pathToJaggeryapps}/publisher ]
		then
			rm -r ${pathToJaggeryapps}/publisher
			timeStamp
	  	echo "[${timestamp}] INFO - Removed publisher Jaggeryapp for store profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Publisher jaggeryapp for store profile is already removed"
		fi
		;;
	-Dprofile=traffic-manager)
		disableTransportSenderWS
		disableTransportSenderWSS
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		disableIndexingConfiguration
		removeSynapseConfigs
		#remove webapps
		if [ -d $pathToWebapps ]
		then
			rm -r $pathToWebapps
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary webapps for traffic-manager profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary webapps for traffic-manager profile are already removed"
		fi
		#remove jaggeryapps
		if [ -d $pathToJaggeryapps ]
		then
			rm -r $pathToJaggeryapps
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary jaggeryapps for traffic-manager profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary jaggeryapps for traffic-manager profile are already removed"
		fi
		;;
	-Dprofile=gateway-worker)
		disablePolicyDeployer
		disableIndexingConfiguration
		#remove unnecessary webapps
		if [ "$(find $pathToWebapps -type f -not -name 'am#sample#pizzashack#v1.war' | wc -l)" -gt 0 ]
		then
			find $pathToWebapps -type f -not -name 'am#sample#pizzashack#v1.war' -delete
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary Webapps for gateway-worker profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary Webapps for gateway-worker profile are already removed"
		fi
		#remove jaggeryapps
		if [ -d $pathToJaggeryapps ]
		then
			rm -r $pathToJaggeryapps
			timeStamp
	  	echo "[${timestamp}] INFO - Removed unnecessary jaggeryapps for gateway-worker profile"
		else
			timeStamp
	  	echo "[${timestamp}] INFO - Unnecessary jaggeryapps for gateway-worker profile are already removed"
		fi
		;;
	*)
		echo "Profile is not specifed properly, please try again"
		exit
esac
