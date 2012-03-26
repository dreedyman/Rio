@echo off

rem This script provides the command and control utility for starting
rem Rio services and the Rio command line interface

rem Use local variables
setlocal

rem Set local variables
if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..

set RIO_LIB=%RIO_HOME%\lib

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe
goto endOfJavaHome

:noJavaHome
set JAVACMD=java.exe
:endOfJavaHome

if "%JAVA_MEM_OPTIONS%" == "" set JAVA_MEM_OPTIONS="-XX:MaxPermSize=256m"

rem Parse command line
if "%1"=="" goto interactive
if "%1"=="start" goto start
if "%1"=="create-project" goto create-project

:interactive
rem set cliExt="%RIO_HOME%"\config\rio_cli.groovy
rem set cliExt=""
set command_line=%*
set launchTarget=org.rioproject.tools.cli.CLI
set classpath=-cp "%RIO_HOME%\lib\rio-cli.jar";"%RIO_HOME%\lib-dl\rio-api.jar";"%RIO_HOME%\lib-dl\jmx-lookup.jar";"%RIO_LIB%\jsk-lib.jar";"%RIO_LIB%\jsk-platform.jar";"%RIO_HOME%\lib\groovy-all.jar";
set props="-DRIO_HOME=%RIO_HOME%"
"%JAVACMD%" %classpath% -Xms256m -Xmx256m -Djava.protocol.handler.pkgs=org.rioproject.url -DRIO_HOME="%RIO_HOME%" -Djava.security.policy="%RIO_HOME%"\policy\policy.all %launchTarget% %cliExt% %command_line%
goto end

:create-project
mvn archetype:generate -DarchetypeGroupId=org.rioproject -DarchetypeGroupId=org.rioproject -DarchetypeRepository=http://www.rio-project.org/maven2 -DarchetypeVersion=5.0-SNAPSHOT
goto end

:start

rem Get the service starter
shift
if "%1"=="" goto noService
set service=%1
set starterConfig=%RIO_HOME%\config\start-%1.groovy
if not exist "%starterConfig%" goto noStarter
shift

rem Call the install script, do not assume that Groovy has been installed.
set groovyClasspath=-cp "%RIO_HOME%\lib\groovy-all.jar"
"%JAVA_HOME%\bin\java" %groovyClasspath% org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain "%RIO_HOME%\bin\install.groovy" "%JAVA_HOME%" "%RIO_HOME%"

echo starter config [%starterConfig%]
set RIO_LOG_DIR="%RIO_HOME%"\logs\
if "%RIO_NATIVE_DIR%" == "" set RIO_NATIVE_DIR="%RIO_HOME%"\lib\native
set PATH=%PATH%;"%RIO_NATIVE_DIR%"

set classpath=-cp "%RIO_HOME%\lib\rio-start.jar";"%RIO_HOME%\lib\start.jar";"%JAVA_HOME%\lib\tools.jar";"%RIO_HOME%\lib\groovy-all.jar";
set agentpath=-javaagent:"%RIO_HOME%\lib\rio-start.jar"

set launchTarget=com.sun.jini.start.ServiceStarter

set loggingConfig="%RIO_HOME%\config\logging\rio-logging.properties"

"%JAVA_HOME%\bin\java" -server %JAVA_MEM_OPTIONS% %classpath% %agentpath% -Djava.protocol.handler.pkgs=org.rioproject.url -Djava.util.logging.config.file=%loggingConfig% -Dorg.rioproject.service=%service% -Djava.security.policy="%RIO_HOME%"\policy\policy.all -Djava.library.path=%RIO_NATIVE_DIR% -DRIO_HOME="%RIO_HOME%" -Dorg.rioproject.home="%RIO_HOME%" -DRIO_NATIVE_DIR=%RIO_NATIVE_DIR% -DRIO_LOG_DIR=%RIO_LOG_DIR% -Drio.script.mainClass=%launchTarget% %launchTarget% "%starterConfig%"
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
