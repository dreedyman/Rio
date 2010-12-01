@rem /*
@rem 
@rem Copyright 2005 Sun Microsystems, Inc.
@rem 
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem 
@rem 	http://www.apache.org/licenses/LICENSE-2.0
@rem 
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem 
@rem */
@echo off
rem This script sets the environment needed to run commands in this 
rem Rio distribution.
rem
rem Instructions
rem -------------
rem Set JAVA_HOME to the location where Java is installed
rem Set JINI_HOME to the directory where Jini is installed
rem Set RIO_HOME to the directory where Rio is installed.
rem
rem Run this command file :
rem      > setenv.bat

set JINI_HOME=%{JINI_HOME}
set RIO_HOME=%{INSTALL_PATH}
set RIO_UTILS=%{INSTALL_PATH}
set RIO_SUBSTRATES_HOME=%{INSTALL_PATH}
