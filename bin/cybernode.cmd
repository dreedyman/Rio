@echo off

rem This script is a wrapper around the "rio" script, and provides the command
rem line instruction to start the Rio Cybernode

title Rio Cybernode
set command_line=%*
call "%~dp0"\rio start cybernode %command_line%
