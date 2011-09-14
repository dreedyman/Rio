@echo off

rem
rem Launches the Rio UI
rem

title Rio UI
set command_line=%*

"%JAVA_HOME%\bin\java" -Djava.protocol.handler.pkgs=org.rioproject.url -jar "%~dp0../lib/rio-ui.jar" %command_line%

