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

::
:: Launches the Rio UI
::

title Rio UI
set command_line=%*

if "%RIO_HOME%" == "" set RIO_HOME=%~dp0..
set rioVersion=@rioV@
"%JAVA_HOME%\bin\java" -Djava.security.policy="%RIO_HOME%"\policy\policy.all -Drio.home="%RIO_HOME%" -Djava.rmi.server.useCodebaseOnly=false -Djava.protocol.handler.pkgs=org.rioproject.url -jar "%RIO_HOME%/lib/rio-ui-%rioVersion%.jar" %command_line%

