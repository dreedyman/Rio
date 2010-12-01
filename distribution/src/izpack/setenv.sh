#!/bin/sh
#/*
# 
# Copyright 2005 Sun Microsystems, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
# 	http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
#*/
# This script sets the environent needed to run commands in this Rio distribution.
#
# Instructions 
# ------------
# Set JAVA_HOME to the location where Java is installed
# Set JINI_HOME to the directory where Jini is installed
# Set RIO_HOME to the directory where Rio is installed.
#
# Run this script:
#	$ . setenv.sh

export JINI_HOME=%{JINI_HOME}
export RIO_HOME=%{INSTALL_PATH}
export RIO_UTILS=%{INSTALL_PATH}
export RIO_SUBSTRATES_HOME=%{INSTALL_PATH}