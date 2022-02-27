@echo off
rem ----------------------------------------------------------------------------
rem  Copyright 2018 WSO2, Inc. http://www.wso2.org
rem
rem  Licensed under the Apache License, Version 2.0 (the "License");
rem  you may not use this file except in compliance with the License.
rem  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing, software
rem  distributed under the License is distributed on an "AS IS" BASIS,
rem  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem  See the License for the specific language governing permissions and
rem  limitations under the License.

set userLocation=%cd%
set pathToAxis2XMLTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2.xml.j2
set pathToRegistryTemplate=..\repository\resources\conf\templates\repository\conf\registry.xml.j2
set pathToInboundEndpoints=..\repository\deployment\server\synapse-configs\default\inbound-endpoints
set pathToInboundEndpointsTemplate=..\repository\resources\conf\templates\repository\deployment\server\synapse-configs\default\inbound-endpoints
set pathToWebapps=..\repository\deployment\server\webapps
set pathToJaggeryapps=..\repository\deployment\server\jaggeryapps
set pathToSynapseConfigs=..\repository\deployment\server\synapse-configs\default
set pathToAxis2TMXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_TM.xml.j2
set pathToAxis2KMXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_KM.xml.j2
set pathToAxis2PublisherXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_Publisher.xml.j2
set pathToAxis2ControlPlaneXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_ControlPlane.xml.j2
set pathToAxis2DevportalXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_Devportal.xml.j2
set pathToRegistryTMTemplate=..\repository\resources\conf\templates\repository\conf\registry_TM.xml.j2
set axis2XMLBackupTemplate=axis2.xml.j2.backup
set registryBackupTemplate=registry.xml.j2.backup
set axis2XMLTemplate=axis2.xml.j2
set registryXMLTemplate=registry.xml.j2
set pathToDeploymentTemplates=..\repository\resources\conf\deployment-templates
set pathToDeploymentConfiguration=..\repository\conf\deployment.toml
set deploymentConfigurationBackup=deployment.toml.backup
set passedSkipConfigOptimizationOption=false
set pathToAxis2BlockingClientXML=..\repository\conf\axis2\axis2_blocking_client.xml
set pathToAxis2BlockingClientXMLTemplate=..\repository\resources\conf\templates\repository\conf\axis2\axis2_blocking_client.xml.j2
set tenantAxis2TXmlTemplateBackup=tenant-axis2.xml.j2.backup
set pathToTenantAxis2XMLTemplate=..\repository\resources\conf\templates\repository\conf\axis2\tenant-axis2.xml.j2
set tenantAxis2XMLTemplate=tenant-axis2.xml.j2
set pathToTenantAxis2KMXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\tenant-axis2_KM.xml.j2
set pathToTenantAxis2PublisherXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\tenant-axis2_Publisher.xml.j2
set pathToTenantAxis2DevportalXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\tenant-axis2_Devportal.xml.j2
set pathToTenantAxis2ControlPlaneXmlTemplate=..\repository\resources\conf\templates\repository\conf\axis2\tenant-axis2_ControlPlane.xml.j2
set pathToTomcatCarbonWEBINFWebXmlTemplate=..\repository\resources\conf\templates\repository\conf\tomcat\carbon\WEB-INF\web.xml.j2
set pathToTomcatCarbonWEBINFWebXmlTemplateBackup=web.xml.j2.backup
set pathToTomcatCarbonWEBINFWebXmlTMTemplate=..\repository\resources\conf\templates\repository\conf\tomcat\carbon\WEB-INF\web_TM_GW.xml.j2
cd /d %~dp0

set profileConfigurationToml=%pathToDeploymentTemplates%\%2.toml
if "%3"=="--skipConfigOptimization" set passedSkipConfigOptimizationOption=true
if "%3"=="-skipConfigOptimization" set passedSkipConfigOptimizationOption=true
if "%3"=="skipConfigOptimization" set passedSkipConfigOptimizationOption=true

rem ----- Process the input commands (two args only)-------------------------------------------
if ""%1""==""-Dprofile"" (
	if ""%2""==""control-plane"" 	goto controlPlane
	if ""%2""==""api-key-manager-deprecated"" 	goto keyManager
	if ""%2""==""api-publisher-deprecated"" 	goto publisher
	if ""%2""==""api-devportal-deprecated"" 	goto devportal
	if ""%2""==""traffic-manager"" 	goto trafficManager
	if ""%2""==""gateway-worker"" 	goto gatewayWorker
)
echo Profile is not specified properly, please try again
goto end

:keyManager
echo Starting to optimize API Manager for the Key Manager profile
call :removeAxis2BlockingClientXMLFile
call :removeAxis2BlockingClientXMLTemplateFile
call :replaceAxis2TemplateFile %pathToAxis2KMXmlTemplate%
call :replaceTenantAxis2TemplateFile %pathToTenantAxis2KMXmlTemplate%
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :removeSynapseConfigs
call :replaceDeploymentConfiguration

rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "client-registration#v.*war authenticationendpoint accountrecoveryendpoint oauth2.war throttle#data#v.*war api#identity#consent-mgt#v.*war api#identity#recovery#v.*war api#identity#user#v.*war api#identity#oauth2#dcr#v.*war api#identity#oauth2#v.*war keymanager-operations.war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
)
rem ---removing jaggeryapps which are not required for this profile--------
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:controlPlane
echo Starting to optimize API Manager for the Control Plane profile
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :replaceDeploymentConfiguration
call :replaceAxis2TemplateFile %pathToAxis2ControlPlaneXmlTemplate%
call :replaceTenantAxis2TemplateFile %pathToTenantAxis2ControlPlaneXmlTemplate%
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "api#am#publisher#v.*war api#am#publisher.war client-registration#v.*war authenticationendpoint accountrecoveryendpoint oauth2.war api#am#admin#v.*war api#am#admin.war internal#data#v.*war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
goto finishOptimization

:publisher
echo Starting to optimize API Manager for the API Publisher profile
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :replaceAxis2TemplateFile %pathToAxis2PublisherXmlTemplate%
call :replaceTenantAxis2TemplateFile %pathToTenantAxis2PublisherXmlTemplate%
call :replaceDeploymentConfiguration
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "api#am#publisher#v.*war api#am#publisher.war client-registration#v.*war authenticationendpoint accountrecoveryendpoint oauth2.war api#am#admin#v.*war api#am#admin.war internal#data#v.*war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
)
rem ---removing jaggeryapps which are not required for this profile--------
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b ^| findstr /v "publisher admin"') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:devportal
echo Starting to optimize API Manager for the Developer Portal profile
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :replaceDeploymentConfiguration
call :replaceAxis2TemplateFile %pathToAxis2DevportalXmlTemplate%
call :replaceTenantAxis2TemplateFile %pathToTenantAxis2DevportalXmlTemplate%
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "api#am#devportal#v.*war api#am#devportal.war client-registration#v.*war authenticationendpoint accountrecoveryendpoint oauth2.war api#am#admin#v.*war api#am#admin.war  api#identity#recovery#v.*war api#identity#user#v.*war api#identity#consent-mgt#v.*war internal#data#v.*war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
)
rem ---removing jaggeryapps which are not required for this profile--------
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b ^| findstr /v "devportal"') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:trafficManager
echo Starting to optimize API Manager for the Traffic Manager profile
call :replaceAxis2TemplateFile %pathToAxis2TMXmlTemplate%
call :replaceRegistryXMLTemplateFile
call :replaceTomcatCarbonWEBINFWebXmlTemplateFile
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :removeSynapseConfigs
call :replaceDeploymentConfiguration
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "internal#data#v.*war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
)
rem ---removing jaggeryapps which are not required for this profile--------
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:gatewayWorker
echo Starting to optimize API Manager for the Gateway worker profile
call :replaceDeploymentConfiguration
call :replaceTomcatCarbonWEBINFWebXmlTemplateFile
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /b ^| findstr /v "am#sample#pizzashack#v.*war api#am#gateway#v2.war"') do (
	del /f %pathToWebapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToWebapps%
	setlocal enableDelayedExpansion
	set folderName=%%i
	set folderName=!folderName:.war=!
	if exist %pathToWebapps%\!folderName!\ (
		rmdir /s /q %pathToWebapps%\!folderName!
		call :Timestamp value
		echo %value% INFO - Removed the !folderName! directory from %pathToWebapps%
	)
	endlocal
)
rem ---removing jaggeryapps which are not required for this profile--------
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:removeWebSocketInboundEndpoint
if exist %pathToInboundEndpoints%\WebSocketInboundEndpoint.xml (
	del /f %pathToInboundEndpoints%\WebSocketInboundEndpoint.xml
	call :Timestamp value
	echo %value% INFO - Removed the WebSocketInboundEndpoint.xml file from %pathToInboundEndpoints%
)
if exist %pathToInboundEndpointsTemplate%\WebSocketInboundEndpoint.xml.j2 (
	del /f %pathToInboundEndpointsTemplate%\WebSocketInboundEndpoint.xml.j2
	call :Timestamp value
	echo %value% INFO - Removed the WebSocketInboundEndpoint.xml.j2 file from %pathToInboundEndpointsTemplate%
)
EXIT /B 0

:removeSecureWebSocketInboundEndpoint
if exist %pathToInboundEndpoints%\SecureWebSocketInboundEndpoint.xml (
	del /f %pathToInboundEndpoints%\SecureWebSocketInboundEndpoint.xml
	call :Timestamp value
	echo %value% INFO - Removed the SecureWebSocketInboundEndpoint.xml file from %pathToInboundEndpoints%
)
if exist %pathToInboundEndpointsTemplate%\SecureWebSocketInboundEndpoint.xml.j2 (
	del /f %pathToInboundEndpointsTemplate%\SecureWebSocketInboundEndpoint.xml.j2
	call :Timestamp value
	echo %value% INFO - Removed the SecureWebSocketInboundEndpoint.xml.j2 file from %pathToInboundEndpointsTemplate%
)
EXIT /B 0

:removeSynapseConfigs
rem ----removing directories if exists ----
for /f %%i in ('dir "%pathToSynapseConfigs%" /A:D /b') do (
	rmdir /s /q %pathToSynapseConfigs%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToSynapseConfigs%
)
rem ----removing the files if exists ------
for /f %%i in ('dir "%pathToSynapseConfigs%" /A:-D /b ^| find /v "synapse.xml"') do (
	del /f %pathToSynapseConfigs%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i file from %pathToSynapseConfigs%
)
EXIT /B 0

:replaceAxis2TemplateFile
set pathToNewAxis2TemplateXml=%~1
if exist %pathToAxis2XMLTemplate% (
	if exist %pathToNewAxis2TemplateXml% (
		ren %pathToAxis2XMLTemplate% %axis2XMLBackupTemplate%
		call :Timestamp value
		echo %value% INFO - Rename the existing %pathToAxis2XMLTemplate% file as %axis2XMLBackupTemplate%
		copy %pathToNewAxis2TemplateXml% %pathToAxis2XMLTemplate%
		call :Timestamp value
		echo %value% INFO - Rename the existing %pathToAxis2TMXmlTemplate% file as %axis2XMLTemplate%
	)
)
EXIT /B 0

:replaceRegistryXMLTemplateFile
if exist %pathToRegistryTemplate% (
	if exist %pathToRegistryTMTemplate% (
        ren %pathToRegistryTemplate% %registryBackupTemplate%
        call :Timestamp value
        echo %value% INFO - Rename the existing %pathToRegistryTemplate% file as %registryBackupTemplate%
        copy  %pathToRegistryTMTemplate% %pathToRegistryTemplate%
        call :Timestamp value
        echo %value% INFO - Rename the existing %pathToRegistryTMTemplate% file as %registryXMLTemplate%
	)
)
EXIT /B 0

:replaceTomcatCarbonWEBINFWebXmlTemplateFile
if exist %pathToTomcatCarbonWEBINFWebXmlTemplate% (
	if exist %pathToTomcatCarbonWEBINFWebXmlTMTemplate% (
        ren %pathToTomcatCarbonWEBINFWebXmlTemplate% %pathToTomcatCarbonWEBINFWebXmlTemplateBackup%
        call :Timestamp value
        echo %value% INFO - Rename the existing %pathToTomcatCarbonWEBINFWebXmlTemplate% file as %pathToTomcatCarbonWEBINFWebXmlTemplateBackup%
        copy  %pathToTomcatCarbonWEBINFWebXmlTMTemplate% %pathToTomcatCarbonWEBINFWebXmlTemplate%
        call :Timestamp value
        echo %value% INFO - Rename the existing %pathToTomcatCarbonWEBINFWebXmlTMTemplate% file as %pathToTomcatCarbonWEBINFWebXmlTemplate%
	)
)
EXIT /B 0

:replaceDeploymentConfiguration
if %passedSkipConfigOptimizationOption%==true (
    call :Timestamp value
    echo %value% INFO - Config optimizations in deployment.toml skipped since the option 'skipConfigOptimization' is passed
) else (
    if exist %pathToDeploymentConfiguration% (
        if exist %profileConfigurationToml% (
            ren %pathToDeploymentConfiguration% %deploymentConfigurationBackup%
            call :Timestamp value
            echo %value% INFO - Renamed the existing %pathToDeploymentConfiguration% file as %deploymentConfigurationBackup%
            copy %profileConfigurationToml% %pathToDeploymentConfiguration%
            call :Timestamp value
            echo %value% INFO - Copied the existing %profileConfigurationToml% file as %pathToDeploymentConfiguration%
        )
    )
)
EXIT /B 0

:removeAxis2BlockingClientXMLFile
    if exist %pathToAxis2BlockingClientXML% (
        del /f %pathToAxis2BlockingClientXML%
        call :Timestamp value
        echo %value% INFO - Removed the file %pathToAxis2BlockingClientXML%
    )
)
EXIT /B 0

:removeAxis2BlockingClientXMLTemplateFile
    if exist %pathToAxis2BlockingClientXMLTemplate% (
        del /f %pathToAxis2BlockingClientXMLTemplate%
        call :Timestamp value
        echo %value% INFO - Removed the file %pathToAxis2BlockingClientXMLTemplate% file.
    )
)
EXIT /B 0

:replaceTenantAxis2TemplateFile
set pathToNewTenantAxis2TemplateXml=%~1
if exist %pathToTenantAxis2XMLTemplate% (
	if exist %pathToNewTenantAxis2TemplateXml% (
		ren %pathToTenantAxis2XMLTemplate% %tenantAxis2TXmlTemplateBackup%
		call :Timestamp value
		echo %value% INFO - Renamed the existing %pathToTenantAxis2XMLTemplate% file as %tenantAxis2TXmlTemplateBackup%
		copy %pathToNewTenantAxis2TemplateXml% %pathToTenantAxis2XMLTemplate%
		call :Timestamp value
		echo %value% INFO - Renamed the existing %pathToNewTenantAxis2TemplateXml% file as %tenantAxis2XMLTemplate%
	)
)
EXIT /B 0

:Timestamp
set "%~1=[%date:~10,14%-%date:~4,2%-%date:~7,2% %time%]"
EXIT /B 0

:finishOptimization
echo Finished the optimizations
goto end

:end
cd /d %userLocation%
