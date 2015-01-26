@echo off

REM ---------------------------------------------------------------------------
REM        Copyright 2005-2009 WSO2, Inc. http://www.wso2.org
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

rem ---------------------------------------------------------------------------
rem Main Script for WSO2 Carbon
rem
rem Environment Variable Prequisites
rem
rem   CARBON_HOME   Home of CARBON installation. If not set I will  try
rem                   to figure it out.
rem
rem   JAVA_HOME       Must point at your Java Development Kit installation.
rem
rem   JAVA_OPTS       (Optional) Java runtime options used when the commands
rem                   is executed.
rem ---------------------------------------------------------------------------

rem ----- if JAVA_HOME is not set we're not happy ------------------------------
:checkJava

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto checkServer

:noJavaHome
echo "You must set the JAVA_HOME variable before running CARBON."
goto end

rem ----- Only set CARBON_HOME if not already set ----------------------------
:checkServer
rem %~sdp0 is expanded pathname of the current script under NT with spaces in the path removed
if "%CARBON_HOME%"=="" set CARBON_HOME=%~sdp0..
SET curDrive=%cd:~0,1%
SET wsasDrive=%CARBON_HOME:~0,1%
if not "%curDrive%" == "%wsasDrive%" %wsasDrive%:

rem find CARBON_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if not exist "%CARBON_HOME%\bin\version.txt" goto noServerHome

set AXIS2_HOME=%CARBON_HOME%
goto updateClasspath

:noServerHome
echo CARBON_HOME is set incorrectly or CARBON could not be located. Please set CARBON_HOME.
goto end

rem ----- update classpath -----------------------------------------------------
:updateClasspath

setlocal EnableDelayedExpansion
cd %CARBON_HOME%
set CARBON_CLASSPATH=
FOR %%C in ("%CARBON_HOME%\bin\*.jar") DO set CARBON_CLASSPATH=!CARBON_CLASSPATH!;".\bin\%%~nC%%~xC"

set CARBON_CLASSPATH="%JAVA_HOME%\lib\tools.jar";%CARBON_CLASSPATH%;

FOR %%D in ("%CARBON_HOME%\lib\commons-lang*.jar") DO set CARBON_CLASSPATH=!CARBON_CLASSPATH!;".\lib\%%~nD%%~xD"

rem ----- Process the input command -------------------------------------------

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).


:setupArgs
if ""%1""=="""" goto doneStart

if ""%1""==""-run""     goto commandLifecycle
if ""%1""==""--run""    goto commandLifecycle
if ""%1""==""run""      goto commandLifecycle

if ""%1""==""-restart""  goto commandLifecycle
if ""%1""==""--restart"" goto commandLifecycle
if ""%1""==""restart""   goto commandLifecycle

if ""%1""==""debug""    goto commandDebug
if ""%1""==""-debug""   goto commandDebug
if ""%1""==""--debug""  goto commandDebug

if ""%1""==""version""   goto commandVersion
if ""%1""==""-version""  goto commandVersion
if ""%1""==""--version"" goto commandVersion

shift
goto setupArgs

rem ----- commandVersion -------------------------------------------------------
:commandVersion
shift
type "%CARBON_HOME%\bin\version.txt"
type "%CARBON_HOME%\bin\wso2carbon-version.txt"
goto end

rem ----- commandDebug ---------------------------------------------------------
:commandDebug
shift
set DEBUG_PORT=%1
if "%DEBUG_PORT%"=="" goto noDebugPort
if not "%JAVA_OPTS%"=="" echo Warning !!!. User specified JAVA_OPTS will be ignored, once you give the --debug option.
set JAVA_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%DEBUG_PORT%
echo Please start the remote debugging client to continue...
goto findJdk

:noDebugPort
echo Please specify the debug port after the --debug option
goto end

rem ----- commandLifecycle -----------------------------------------------------
:commandLifecycle
goto findJdk

:doneStart
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem ---------- Handle the SSL Issue with proper JDK version --------------------
rem find the version of the jdk
:findJdk

set CMD=RUN %*

:checkJdk16
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr /r "1.[6|7]" >NUL
IF ERRORLEVEL 1 goto unknownJdk
goto jdk16

:unknownJdk
echo Starting WSO2 Carbon (in unsupported JDK)
echo [ERROR] CARBON is supported only on JDK 1.6 and 1.7
goto jdk16

:jdk16
goto runServer

rem ----------------- Execute The Requested Command ----------------------------

:runServer
cd %CARBON_HOME%

rem ---------- Add jars to classpath ----------------

set CARBON_CLASSPATH=.\lib;%CARBON_CLASSPATH%

set JAVA_ENDORSED=".\lib\endorsed";"%JAVA_HOME%\jre\lib\endorsed";"%JAVA_HOME%\lib\endorsed"

set CMD_LINE_ARGS=-Xbootclasspath/a:%CARBON_XBOOTCLASSPATH% -Xms256m -Xmx1024m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%CARBON_HOME%\repository\logs\heap-dump.hprof"  -Dcom.sun.management.jmxremote -classpath %CARBON_CLASSPATH% %JAVA_OPTS% -Djava.endorsed.dirs=%JAVA_ENDORSED% -Dcarbon.registry.root=/ -Dcarbon.home="%CARBON_HOME%" -Dwso2.server.standalone=true -Djava.command="%JAVA_HOME%\bin\java" -Djava.opts="%JAVA_OPTS%" -Djava.io.tmpdir="%CARBON_HOME%\tmp" -Dcatalina.base="%CARBON_HOME%\lib\tomcat" -Dwso2.carbon.xml=%CARBON_HOME%\repository\conf\carbon.xml -Dwso2.registry.xml="%CARBON_HOME%\repository\conf\registry.xml" -Dwso2.user.mgt.xml="%CARBON_HOME%\repository\conf\user-mgt.xml" -Dwso2.transports.xml="%CARBON_HOME%\repository\conf\mgt-transports.xml" -Djava.util.logging.config.file="%CARBON_HOME%\repository\conf\etc\logging-bridge.properties" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Dcarbon.config.dir.path="%CARBON_HOME%\repository\conf" -Dcarbon.logs.path="%CARBON_HOME%\repository\logs" -Dcomponents.repo="%CARBON_HOME%\repository\components" -Dconf.location="%CARBON_HOME%\repository\conf" -Dcom.atomikos.icatch.file="%CARBON_HOME%\lib\transactions.properties" -Dcom.atomikos.icatch.hide_init_file_path="true" -Dorg.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER=true -Dcom.sun.jndi.ldap.connect.pool.authentication=simple -Dcom.sun.jndi.ldap.connect.pool.timeout=3000 -Dorg.terracotta.quartz.skipUpdateCheck=true -Dcarbon.classpath=%CARBON_CLASSPATH% -Dfile.encoding=UTF8 -DapplyPatches

:runJava
echo JAVA_HOME environment variable is set to %JAVA_HOME%
echo CARBON_HOME environment variable is set to %CARBON_HOME%
"%JAVA_HOME%\bin\java" %CMD_LINE_ARGS% org.wso2.carbon.bootstrap.Bootstrap %CMD%
if "%ERRORLEVEL%"=="121" goto runJava
:end
goto endlocal

:endlocal

:END
