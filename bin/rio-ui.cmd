@echo off

rem
rem Launches the Rio UI
rem

title Rio UI
set command_line=%*

if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..
set rioVersion=5.0-M3
"%JAVA_HOME%\bin\java" -Djava.security.policy="%RIO_HOME%"\policy\policy.all -DRIO_HOME="%RIO_HOME%" -Djava.protocol.handler.pkgs=org.rioproject.url -jar "%RIO_HOME%/lib/rio-ui-%rioVersion%.jar" %command_line%

