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

set usertLocation=%cd%
set pathToApiManagerXML=..\repository\conf\api-manager.xml
set pathToAxis2XML=..\repository\conf\axis2\axis2.xml
set pathToRegistry=..\repository\conf\registry.xml
set pathToInboundEndpoints=..\repository\deployment\server\synapse-configs\default\inbound-endpoints
set pathToWebapps=..\repository\deployment\server\webapps
set pathToJaggeryapps=..\repository\deployment\server\jaggeryapps
set pathToSynapseConfigs=..\repository\deployment\server\synapse-configs\default
cd /d %~dp0

rem ----- Process the input commands (two args only)-------------------------------------------
if ""%1""==""-Dprofile"" (
	if ""%2""==""api-key-manager"" 	goto keyManager
	if ""%2""==""api-publisher"" 	goto publisher
	if ""%2""==""api-store"" 		goto store
	if ""%2""==""traffic-manager"" 	goto trafficManager
	if ""%2""==""gateway-worker"" 	goto gatewayWorker
)
echo Profile is not specifed properly, please try again
goto end

:keyManager
echo Starting to optimize API Manager for the Key Manager profile
call :disableDataPublisher
call :disableJMSConnectionDetails
call :disablePolicyDeployer
call :disableTransportSenderWS
call :disableTransportSenderWSS
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :removeSynapseConfigs
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /A:-D /b ^| findstr /v "client-registration#v.*war authenticationendpoint.war oauth2.war throttle#data#v.*war api#identity#consent-mgt#v.*war"') do (
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

:publisher
echo Starting to optimize API Manager for the API Publisher profile
call :disableDataPublisher
call :disableJMSConnectionDetails
call :disableTransportSenderWS
call :disableTransportSenderWSS
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /A:-D /b ^| findstr /v "api#am#publisher#v.*war"') do (
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

:store
echo Starting to optimize API Manager for the Developer Portal (API Store) profile
call :disableDataPublisher
call :disableJMSConnectionDetails
call :disablePolicyDeployer
call :disableTransportSenderWS
call :disableTransportSenderWSS
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /A:-D /b ^| findstr /v "api#am#store#v.*war"') do (
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
for /f %%i in ('dir "%pathToJaggeryapps%" /A:D /b ^| findstr /v "store"') do (
	rmdir /s /q %pathToJaggeryapps%\%%i
	call :Timestamp value
	echo %value% INFO - Removed the %%i directory from %pathToJaggeryapps%
)
goto finishOptimization

:trafficManager
echo Starting to optimize API Manager for the Traffic Manager profile
call :disableTransportSenderWS
call :disableTransportSenderWSS
call :removeWebSocketInboundEndpoint
call :removeSecureWebSocketInboundEndpoint
call :disableIndexingConfiguration
call :removeSynapseConfigs
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /A:-D /b') do (
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
call :disablePolicyDeployer
call :disableIndexingConfiguration
rem ---removing webbapps which are not required for this profile--------
for /f %%i in ('dir %pathToWebapps% /A:-D /b ^| findstr /v "am#sample#pizzashack#v.*war"') do (
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

:disableDataPublisher
for /f %%i in ('powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.DataPublisher.Enabled;"') do (
	if %%i==true (
		powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.DataPublisher.Enabled='false'; $xml.Save('%pathToApiManagerXML%');"
		call :Timestamp value
		echo %value% INFO - Disabled the ^<DataPublisher^> from api-manager.xml file
	)
)
EXIT /B 0

:disableJMSConnectionDetails
for /f %%i in ('powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.JMSConnectionDetails.Enabled;"') do (
	if %%i==true (
		powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.JMSConnectionDetails.Enabled='false'; $xml.Save('%pathToApiManagerXML%');"
		call :Timestamp value
		echo %value% INFO - Disabled the ^<JMSConnectionDetails^> from api-manager.xml file
	)
)
EXIT /B 0

:disablePolicyDeployer
for /f %%i in ('powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.PolicyDeployer.Enabled;"') do (
	if %%i==true (
		powershell -Command "$xml = [xml] (Get-Content %pathToApiManagerXML%); $xml.APIManager.ThrottlingConfigurations.PolicyDeployer.Enabled='false'; $xml.Save('%pathToApiManagerXML%');"
		call :Timestamp value
		echo %value% INFO - Disabled the ^<PolicyDeployer^> from api-manager.xml file
	)
)
EXIT /B 0

:disableTransportSenderWS
for /f %%i in ('powershell -Command "& {$xml = [xml] (Get-Content %pathToAxis2XML%); $xml.selectSingleNode('//transportSender[@name=\"ws\"]'); }" ') do (
	powershell -Command "& { $xml = [xml] (Get-Content %pathToAxis2XML%); $xml.selectNodes('//transportSender[@name=\"ws\"]') | ForEach-Object { $node = $_; $comment = $xml.CreateComment($node.OuterXml); $node=$node.ParentNode.ReplaceChild($comment, $node);}; $xml.Save('%pathToAxis2XML%');}"
	call :Timestamp value
	echo %value% INFO - Disabled the ^<transportSender name="ws" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender"^> from axis2.xml file
	goto skipLoop1
)
:skipLoop1
EXIT /B 0

:disableTransportSenderWSS
for /f %%i in ('powershell -Command "& {$xml = [xml] (Get-Content %pathToAxis2XML%); $xml.SelectSingleNode('//transportSender[@name=\"wss\"]'); }" ') do (
	powershell -Command "& { $xml = [xml] (Get-Content %pathToAxis2XML%); $xml.selectNodes('//transportSender[@name=\"wss\"]') | ForEach-Object { $node = $_; $comment = $xml.CreateComment($node.OuterXml); $node=$node.ParentNode.ReplaceChild($comment, $node);}; $xml.Save('%pathToAxis2XML%');}"
	call :Timestamp value
	echo %value% INFO - Disabled the ^<transportSender name="wss" class="org.wso2.carbon.websocket.transport.WebsocketTransportSender"^> from axis2.xml file
	goto skipLoop2
	)
:skipLoop2
EXIT /B 0

:disableIndexingConfiguration
for /f %%i in ('powershell -Command "$xml = [xml] (Get-Content %pathToRegistry%); $xml.wso2registry.indexingConfiguration.startIndexing;"') do (
	if %%i==true (
		powershell -Command "$xml = [xml] (Get-Content %pathToRegistry%); $xml.wso2registry.indexingConfiguration.startIndexing='false'; $xml.Save('%pathToRegistry%');"
		call :Timestamp value
		echo %value% INFO - Disabled the ^<indexingConfiguration^> from registry.xml file
	)
)
EXIT /B 0

:removeWebSocketInboundEndpoint
if exist %pathToInboundEndpoints%\WebSocketInboundEndpoint.xml (
	del /f %pathToInboundEndpoints%\WebSocketInboundEndpoint.xml
	call :Timestamp value
	echo %value% INFO - Removed the WebSocketInboundEndpoint.xml file from %pathToInboundEndpoints%
)
EXIT /B 0

:removeSecureWebSocketInboundEndpoint
if exist %pathToInboundEndpoints%\SecureWebSocketInboundEndpoint.xml (
	del /f %pathToInboundEndpoints%\SecureWebSocketInboundEndpoint.xml
	call :Timestamp value
	echo %value% INFO - Removed the SecureWebSocketInboundEndpoint.xml file from %pathToInboundEndpoints%
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

:Timestamp
set "%~1=[%date:~10,14%-%date:~4,2%-%date:~7,2% %time%]"
EXIT /B 0

:finishOptimization
echo Finished the optimizations
goto end

:end
cd /d %usertLocation%
