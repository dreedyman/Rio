@echo off

rem This script is a wrapper around the "rio" script, and provides the command
rem line instruction to start the Rio Provision Monitor

title Rio Provision Monitor
set command_line=%*
call "%~dp0"\rio start monitor %command_line%