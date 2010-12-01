@echo off

rem This script is a wrapper around the "rio" script, and provides the command
rem line instruction to start all required Rio services
rem (Cybernode, Monitor & Lookup)

title Rio
set command_line=%*
call "%~dp0\rio" start all %command_line%
