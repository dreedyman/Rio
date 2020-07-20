@echo off
::
:: Copyright to the original author or authors.
::
:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::      http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
::

:: This script provides the command and control utility for starting
:: Rio services and the Rio command line interface

:: Use local variables
setLocal EnableDelayedExpansion

:: Set local variables
if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..

set SLF4J_CLASSPATH="%RIO_HOME%\lib\logging\*";"%RIO_HOME%\config\logging"
set RIO_LIB=%RIO_HOME%\lib

:: Set Versions
set rioVersion=@rioV@
set groovyVersion=@groovyV@
set riverVersion=@riverV@

set logbackConfig="%RIO_HOME%\config\logging\logback.groovy"
set loggingConfig="%RIO_HOME%\config\logging\logging.properties"

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe
goto endOfJavaHome

:noJavaHome
set JAVACMD=java.exe
:endOfJavaHome

if "%JAVA_MEM_OPTIONS%" == "" set JAVA_MEM_OPTIONS="-XX:MaxPermSize=256m"

:: Parse command line
if "%1"=="" goto interactive
if "%1"=="start" goto start
if "%1"=="browser" goto browser

:interactive
:: set cliExt="%RIO_HOME%"\config\rio_cli.groovy
:: set cliExt=""
set command_line=%*
set launchTarget=org.rioproject.tools.cli.CLI
set classpath=-cp "%RIO_HOME%\lib\rio-cli-%rioVersion%.jar";"%SLF4J_CLASSPATH%";

set urlProp="-Djava.protocol.handler.pkgs=org.rioproject.url"
set rmiProps="-Djava.rmi.server.useCodebaseOnly=false"
set secProps="-Djava.security.policy="%RIO_HOME%"\policy\policy.all"
set serialFilter="org.rioproject.**;net.jini.**;com.sun.**"
set serialProps="-Djdk.serialFilter=%serialFilter% -Dsun.rmi.registry.registryFilter=%serialFilter% -Dsun.rmi.transport.dgcFilter=%serialFilter%"
set ipv4="-Djava.net.preferIPv4Stack=true"

"%JAVACMD%" %classpath% -Xms256m -Xmx256m -Djava.protocol.handler.pkgs=org.rioproject.url -Drio.home="%RIO_HOME%" %urlProp% %rmiProps% %secProps% %ipv4% %serialProps% %launchTarget% %cliExt% %command_line%
goto end

:browser
set RIO_LIB="%RIO_HOME%\lib
set RIO_LIB-DL="%RIO_HOME%\lib-dl
set urlProp="-Djava.protocol.handler.pkgs=org.rioproject.url"
set classpath=-cp "%RIO_LIB%/rio-start-%rioVersion%.jar:%RIO_LIB%/browser-1.0.jar:%RIO_LIB%/rio-lib-%rioVersion%.jar:%RIO_LIB%/groovy-all-%groovyVersion%.jar:%SLF4J_CLASSPATH%:%RIO_LIB-DL%/serviceui-%riverVersion%.jar"
"%JAVACMD%" %classpath% %logbackConfig% %loggingConfig -Djava.security.policy="%RIO_HOME%\policy\policy.all" -Drio.home=%RIO_HOME%  %urlProp% org.apache.river.examples.browser.Browser
goto end

:start

:: Get the service starter
shift
if "%1"=="" goto noService
set service=%1
set starterConfig="%RIO_HOME%\config\start-%1.groovy"
if not exist "%starterConfig%" goto noStarter
shift

:: Call the install script, do not assume that Groovy has been installed.
set groovyClasspath=-cp "%RIO_HOME%\lib\groovy-all-%groovyVersion%.jar"
"%JAVA_HOME%\bin\java" %groovyClasspath% org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain "%RIO_HOME%\bin\install.groovy" "%JAVA_HOME%" "%RIO_HOME%"

echo starter config [%starterConfig%]
set RIO_LOG_DIR="%RIO_HOME%"\logs\
if "%RIO_NATIVE_DIR%" == "" set RIO_NATIVE_DIR="%RIO_HOME%"\lib\native
set PATH=%PATH%;"%RIO_NATIVE_DIR%"

set classpath=-cp "%RIO_HOME%\lib\rio-start-%rioVersion%.jar";"%JAVA_HOME%\lib\tools.jar";"%RIO_HOME%\lib\groovy-all-%groovyVersion%.jar";"%SLF4J_CLASSPATH%";
set agentpath=-javaagent:"%RIO_HOME%\lib\rio-start-%rioVersion%.jar"

set launchTarget=org.rioproject.start.ServiceStarter

"%JAVA_HOME%\bin\java" -server %JAVA_MEM_OPTIONS% %classpath% %agentpath% -Djava.protocol.handler.pkgs=org.rioproject.url -Djava.rmi.server.useCodebaseOnly=false -Dlogback.configurationFile=%logbackConfig% -Djava.util.logging.config.file=%loggingConfig% -Dorg.rioproject.service=%service% %USER_OPTS% -Djava.security.policy="%RIO_HOME%"\policy\policy.all -Djava.library.path=%RIO_NATIVE_DIR% -Drio.home="%RIO_HOME%" -Dorg.rioproject.home="%RIO_HOME%" -Drio.native.dir=%RIO_NATIVE_DIR% -Drio.log.dir=%RIO_LOG_DIR% -Drio.script.mainClass=%launchTarget% %launchTarget% "%starterConfig%"
goto end

:noStarter
echo Cannot locate expected service starter file [start-%1.config] in [%RIO_HOME%\config], exiting"
goto exitWithError

:noService
echo "A service to start is required, exiting"

:exitWithError
exit /B 1

:end
endlocal
title Command Prompt
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal
exit /B 0
