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
pathToDeploymentConfiguration='../repository/conf/deployment.toml'
pathToAxis2XMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2.xml.j2'
pathToTenantAxis2XMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2.xml.j2'
pathToAxis2BlockingClientXML='../repository/conf/axis2/axis2_blocking_client.xml'
pathToAxis2BlockingClientXMLTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_blocking_client.xml.j2'
pathToRegistryTemplate='../repository/resources/conf/templates/repository/conf/registry.xml.j2'
pathToInboundEndpoints='../repository/deployment/server/synapse-configs/default/inbound-endpoints/'
pathToInboundEndpointsTemplate='../repository/resources/conf/templates/repository/deployment/server/synapse-configs/default/inbound-endpoints/'
pathToWebapps='../repository/deployment/server/webapps'
pathToJaggeryapps='../repository/deployment/server/jaggeryapps'
pathToSynapseConfigs='../repository/deployment/server/synapse-configs/default'
pathToAxis2TMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_TM.xml.j2'
pathToAxis2KMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_KM.xml.j2'
pathToTenantAxis2KMXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2_KM.xml.j2'
pathToAxis2PublisherXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_Publisher.xml.j2'
pathToTenantAxis2PublisherXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2_Publisher.xml.j2'
pathToAxis2DevportalXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_Devportal.xml.j2'
pathToTenantAxis2DevportalXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2_Devportal.xml.j2'
pathToAxis2ControlPlaneXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/axis2_ControlPlane.xml.j2'
pathToTenantAxis2ControlPlaneXmlTemplate='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2_ControlPlane.xml.j2'
pathToRegistryTMTemplate='../repository/resources/conf/templates/repository/conf/registry_TM.xml.j2'
pathToAxis2TXmlTemplateBackup='../repository/resources/conf/templates/repository/conf/axis2/axis2.xml.j2.backup'
pathToTenantAxis2TXmlTemplateBackup='../repository/resources/conf/templates/repository/conf/axis2/tenant-axis2.xml.j2.backup'
pathToRegistryTemplateBackup='../repository/resources/conf/templates/repository/conf/registry.backup'
pathToDeploymentConfigurationBackup='../repository/conf/deployment.toml.backup'
pathToDeploymentTemplates='../repository/resources/conf/deployment-templates'
pathToTomcatCarbonWEBINFWebXmlTemplate='../repository/resources/conf/templates/repository/conf/tomcat/carbon/WEB-INF/web.xml.j2'
pathToTomcatCarbonWEBINFWebXmlTemplateBackup='../repository/resources/conf/templates/repository/conf/tomcat/carbon/WEB-INF/web.xml.j2.backup'
pathToTomcatCarbonWEBINFWebXmlTMTemplate='../repository/resources/conf/templates/repository/conf/tomcat/carbon/WEB-INF/web_TM_GW.xml.j2'
timestamp=""
cd `dirname "$0"`

timeStamp() {
	timestamp=`date '+%Y-%m-%d %H:%M:%S' | sed 's/\(:[0-9][0-9][0-9]\)[0-9]*$/\1/' `
}

removeWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}WebSocketInboundEndpoint.xml
		timeStamp
		echo "[${timestamp}] INFO - Removed the WebSocketInboundEndpoint.xml file from $pathToInboundEndpoints"
	fi

    if [ -e ${pathToInboundEndpointsTemplate}WebSocketInboundEndpoint.xml.j2 ]
	then
		rm -r ${pathToInboundEndpointsTemplate}WebSocketInboundEndpoint.xml.j2
		timeStamp
		echo "[${timestamp}] INFO - Removed the WebSocketInboundEndpoint.xml.j2 file from $pathToInboundEndpointsTemplate"
	fi
}

removeSecureWebSocketInboundEndpoint(){
	if [ -e ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml ]
	then
		rm -r ${pathToInboundEndpoints}SecureWebSocketInboundEndpoint.xml
		timeStamp
		echo "[${timestamp}] INFO - Removed the SecureWebSocketInboundEndpoint.xml file from $pathToInboundEndpoints"
	fi

	if [ -e ${pathToInboundEndpointsTemplate}SecureWebSocketInboundEndpoint.xml.j2 ]
	then
		rm -r ${pathToInboundEndpointsTemplate}SecureWebSocketInboundEndpoint.xml.j2
		timeStamp
		echo "[${timestamp}] INFO - Removed the SecureWebSocketInboundEndpoint.xml.j2 file from $pathToInboundEndpointsTemplate"
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

removeAxis2BlockingClientXMLFile(){
  if [ -e $pathToAxis2BlockingClientXML ]
	then
		rm -r $pathToAxis2BlockingClientXML
		timeStamp
		echo "[${timestamp}] INFO - Removed the file $pathToAxis2BlockingClientXML"
	fi
}

removeAxis2BlockingClientXMLTemplateFile(){
  if [ -e $pathToAxis2BlockingClientXMLTemplate ]
	then
		rm -r $pathToAxis2BlockingClientXMLTemplate
		timeStamp
		echo "[${timestamp}] INFO - Removed the file $pathToAxis2BlockingClientXMLTemplate"
	fi
}

replaceAxis2TemplateFile(){
	pathToNewAxis2TemplateXml=$1
	if [ -e $pathToAxis2XMLTemplate ] && [ -e $pathToNewAxis2TemplateXml ]
	then
		mv $pathToAxis2XMLTemplate $pathToAxis2TXmlTemplateBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToAxis2XMLTemplate file as axis2.xml.j2.backup"
		cp $pathToNewAxis2TemplateXml $pathToAxis2XMLTemplate
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
		cp $pathToNewAxis2TemplateXml $pathToTenantAxis2XMLTemplate
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
		cp $pathToRegistryTMTemplate $pathToRegistryTemplate
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToRegistryTMTemplate file as registry.xml.j2"
	fi
}

replaceTomcatCarbonWEBINFWebXmlTemplateFile(){
  if [ -e $pathToTomcatCarbonWEBINFWebXmlTemplate ] && [ -e $pathToTomcatCarbonWEBINFWebXmlTMTemplate ]
	then
	  mv $pathToTomcatCarbonWEBINFWebXmlTemplate $pathToTomcatCarbonWEBINFWebXmlTemplateBackup
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToTomcatCarbonWEBINFWebXmlTemplate file as web.xml.j2.backup"
		cp $pathToTomcatCarbonWEBINFWebXmlTMTemplate $pathToTomcatCarbonWEBINFWebXmlTemplate
		timeStamp
		echo "[${timestamp}] INFO - Renamed the existing $pathToTomcatCarbonWEBINFWebXmlTMTemplate file as web.xml.j2"
	fi
}

replaceDeploymentConfiguration(){
    if [ "$passedSkipConfigOptimizationOption" = true ]; then
       timeStamp
       echo "[${timestamp}] INFO - Config optimizations in deployment.toml skipped since the option --skipConfigOptimization is passed"
    else
        echo "[${timestamp}] INFO - Starting to optimize configs in deployment.toml"
        profileConfiguration=$pathToDeploymentTemplates/$1.toml
        if [ -e "$pathToDeploymentConfiguration" ] && [ -e "$profileConfiguration" ];then
            mv "$pathToDeploymentConfiguration" "$pathToDeploymentConfigurationBackup"
            timeStamp
            echo "[${timestamp}] INFO - Renamed the existing $pathToDeploymentConfiguration file as deployment.toml.backup"
            cp "$profileConfiguration" "$pathToDeploymentConfiguration"
            timeStamp
            echo "[${timestamp}] INFO - Copied the existing $profileConfiguration file as $pathToDeploymentConfiguration"
        fi
    fi
}

passedSkipConfigOptimizationOption=false
for option in $*
do
  if [ "$option" = "--skipConfigOptimization" ] || [ "$option" = "-skipConfigOptimization" ] ||   [ "$option" = "skipConfigOptimization" ]
  then
    passedSkipConfigOptimizationOption=true
    timeStamp
    echo "[${timestamp}] INFO - Passed 'skipConfigOptimization' Option: $passedSkipConfigOptimizationOption. So going \
to run Profile Optimization without doing the config optimizations"
  fi
done

#main
case $1 in
	-Dprofile=api-key-manager-deprecated)
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the Key Manager profile"
		removeAxis2BlockingClientXMLFile
		removeAxis2BlockingClientXMLTemplateFile
		replaceAxis2TemplateFile $pathToAxis2KMXmlTemplate
		replaceTenantAxis2TemplateFile $pathToTenantAxis2KMXmlTemplate
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		replaceDeploymentConfiguration api-key-manager-deprecated $passedSkipConfigOptimizationOption
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name \
		'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' \
		-o -name 'api#identity#consent-mgt#v*.war' -o -name 'api#identity#recovery#v*.war' -o -name \
		'api#identity#user#v*.war' -o -name 'api#identity#oauth2#dcr#v*.war' -o -name 'api#identity#oauth2#v*.war' \
		-o -name 'keymanager-operations.war' \) ); do
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
	-Dprofile=api-publisher-deprecated)
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the API Publisher profile"
		replaceDeploymentConfiguration api-publisher-deprecated $passedSkipConfigOptimizationOption
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
    replaceAxis2TemplateFile $pathToAxis2PublisherXmlTemplate
		replaceTenantAxis2TemplateFile $pathToTenantAxis2PublisherXmlTemplate
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' -o -name 'api#am#publisher#v*.war' -o -name 'api#am#publisher.war' -o -name 'api#am#admin#v*.war' -o -name 'api#am#admin.war' -o -name 'api#identity#consent-mgt#v*.war'  -o -name 'internal#data#v*.war' \) ); do
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
	-Dprofile=api-devportal-deprecated)
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the Developer Portal profile"
		replaceDeploymentConfiguration api-devportal-deprecated $passedSkipConfigOptimizationOption
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
    replaceAxis2TemplateFile $pathToAxis2DevportalXmlTemplate
		replaceTenantAxis2TemplateFile $pathToTenantAxis2DevportalXmlTemplate
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'client-registration#v*.war' -o -name 'authenticationendpoint' -o -name 'accountrecoveryendpoint' -o -name 'oauth2.war' -o -name 'api#am#devportal#v*.war' -o -name 'api#am#devportal.war' -o -name 'api#am#admin#v*.war' -o -name 'api#am#admin.war' -o -name 'api#identity#consent-mgt#v*.war' -o -name 'api#identity#recovery#v*.war' -o -name 'api#identity#user#v*.war' -o -name 'internal#data#v*.war' \) ); do
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
	-Dprofile=control-plane)
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the Control Plane profile"
		replaceDeploymentConfiguration control-plane $passedSkipConfigOptimizationOption
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		replaceAxis2TemplateFile $pathToAxis2ControlPlaneXmlTemplate
		replaceTenantAxis2TemplateFile $pathToTenantAxis2ControlPlaneXmlTemplate
		 # removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 \( -name 'api#am#gateway#v2.war' \)); do
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
        ;;
	-Dprofile=traffic-manager)
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the Traffic Manager profile"
		replaceAxis2TemplateFile $pathToAxis2TMXmlTemplate
		replaceRegistryXMLTemplateFile
		replaceTomcatCarbonWEBINFWebXmlTemplateFile
		replaceDeploymentConfiguration traffic-manager $passedSkipConfigOptimizationOption
		removeWebSocketInboundEndpoint
		removeSecureWebSocketInboundEndpoint
		removeSynapseConfigs
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not -name 'internal#data#v*.war'); do
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
		timeStamp
		echo "[${timestamp}] INFO - Starting to optimize API Manager for the Gateway worker profile"
     	replaceDeploymentConfiguration gateway-worker $2
		  replaceTomcatCarbonWEBINFWebXmlTemplateFile
		# removing webbapps which are not required for this profile
		for i in $(find $pathToWebapps -maxdepth 1 -mindepth 1 -not \( -name 'am#sample#pizzashack#v*.war' -o -name 'api#am#gateway#v2.war' \)); do
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
