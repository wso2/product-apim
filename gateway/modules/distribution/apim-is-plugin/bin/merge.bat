@echo off
REM ---------------------------------------------------------------------------
REM        Copyright 2022 WSO2, Inc. http://www.wso2.org
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

REM merge.sh script copy the APIM-IS-PLUGIN artifacts on top of WSO2 IS
REM
REM merge.bat IS-HOME

if ""%1""=="""" (
	echo [ERROR] IS_HOME is not specified, please try again with correct arguments
	exit /b 1
)
set IS_HOME=%1
echo [INFO] Product IS home is: %IS_HOME%

FOR %%A IN ("%~dp0.") DO set APIM_IS_PLUGIN_HOME=%%~dpA
echo [INFO WSO2 IS-APIM Plugin home is: %APIM_IS_PLUGIN_HOME%

if exist %IS_HOME%\repository\components\ (
	echo [INFO] Valid carbon product path
) else (
	echo [ERROR] Specified product path is not a valid carbon product path
	exit /b 1
)

rem create the is-apim-plugin folder in product home, if not exist
set APIM_IS_PLUGIN_AUDIT=%IS_HOME%\is-apim-plugin
set APIM_IS_PLUGIN_AUDIT_BACKUP=%APIM_IS_PLUGIN_AUDIT%\backup

if exist %APIM_IS_PLUGIN_AUDIT% (
	echo [INFO] APIM-IS-PLUGIN audit folder is present at %APIM_IS_PLUGIN_AUDIT%
	if exist %APIM_IS_PLUGIN_AUDIT_BACKUP% (
	    del /q %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps\*
	    del /q %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins\*
	)
) else (
	mkdir %APIM_IS_PLUGIN_AUDIT_BACKUP%
	mkdir %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps
   	mkdir %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins
	echo [INFO] APIM-IS-PLUGIN audit folder %APIM_IS_PLUGIN_AUDIT% is created
)

echo [INFO] Backup original product files..
if exist %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps\keymanager-operations\ (
	rmdir /s /q %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps\keymanager-operations
	echo [INFO] Removed old keymanager-operations directory from %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps
)

if exist %IS_HOME%\repository\deployment\server\webapps\keymanager-operations\ (
	xcopy /i /e %IS_HOME%\repository\deployment\server\webapps\keymanager-operations %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps\keymanager-operations\
)

if exist %IS_HOME%\repository\deployment\server\webapps\keymanager-operations.war (
	copy %IS_HOME%\repository\deployment\server\webapps\keymanager-operations.war %APIM_IS_PLUGIN_AUDIT_BACKUP%\webapps
)

if exist %IS_HOME%\repository\components\dropins\wso2is.key.manager.core-1.0.16*.jar (
	copy %IS_HOME%\repository\components\dropins\wso2is.key.manager.core-1.0.16*.jar %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins
)
if exist %IS_HOME%\repository\components\dropins\wso2is.key.manager.core_1.0.16*.jar (
	copy %IS_HOME%\repository\components\dropins\wso2is.key.manager.core_1.0.16*.jar %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins
)

if exist %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers-1.0.16*.jar (
	copy %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers-1.0.16*.jar %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins
)
if exist %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers_1.0.16*.jar (
	copy %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers_1.0.16*.jar %APIM_IS_PLUGIN_AUDIT_BACKUP%\dropins
)

echo [INFO] Clean up extracted webapps..
if exist %IS_HOME%\repository\deployment\server\webapps\keymanager-operations\ (
	rmdir /s /q %IS_HOME%\repository\deployment\server\webapps\keymanager-operations\
)

echo [INFO] Clean up keymanager-operations.war
if exist %IS_HOME%\repository\deployment\server\webapps\keymanager-operations.war (
	del %IS_HOME%\repository\deployment\server\webapps\keymanager-operations.war
)

echo [INFO] Clean up key-manager jars from dropins..
if exist %IS_HOME%\repository\components\dropins\wso2is.key.manager.core-1.0.16*.jar (
	del %IS_HOME%\repository\components\dropins\wso2is.key.manager.core-1.0.16*.jar
)
echo [INFO] Clean up key-manager jars from dropins..
if exist %IS_HOME%\repository\components\dropins\wso2is.key.manager.core_1.0.16*.jar (
	del %IS_HOME%\repository\components\dropins\wso2is.key.manager.core_1.0.16*.jar
)

if exist %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers-1.0.16*.jar (
	del %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers-1.0.16*.jar
)
if exist %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers_1.0.16*.jar (
	del %IS_HOME%\repository\components\dropins\wso2is.notification.event.handlers_1.0.16*.jar
)

echo [INFO] Copying APIM Key Manager connector artifacts to dropins
echo ================================================
xcopy /i /s /y %APIM_IS_PLUGIN_HOME%dropins %IS_HOME%\repository\components\dropins\

echo [INFO] Copying APIM Key Manager connector artifacts to webapps
echo ================================================
copy %APIM_IS_PLUGIN_HOME%webapps\keymanager-operations.war %IS_HOME%\repository\deployment\server\webapps\

echo [INFO] Completed!
echo %date% - %USERNAME% - "WSO2 APIM-IS Plugin 4.1.0" >> %APIM_IS_PLUGIN_AUDIT%\merge_audit.log