@echo off
rem
rem Copyright to the original author or authors.
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem

rem
rem Launches the Rio UI
rem

title Rio UI
set command_line=%*

if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..
set rioVersion=5.0-M3
"%JAVA_HOME%\bin\java" -Djava.security.policy="%RIO_HOME%"\policy\policy.all -DRIO_HOME="%RIO_HOME%" -Djava.protocol.handler.pkgs=org.rioproject.url -jar "%RIO_HOME%/lib/rio-ui-%rioVersion%.jar" %command_line%

