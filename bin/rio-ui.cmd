@echo off

rem
rem Launches the Rio UI
rem

title Rio UI
set command_line=%*

if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..

"%JAVA_HOME%\bin\java" -DRIO_HOME="%RIO_HOME%" -Djava.protocol.handler.pkgs=org.rioproject.url -jar "%RIO_HOME%/lib/rio-ui.jar" %command_line%

