@echo off

rem This script is a wrapper around the "rio" script, and provides the command
rem line instruction to start the Rio Webster HTTP server

title Webster
set command_line=%*
call %~dp0\rio start webster %command_line%

