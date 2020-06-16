#!/bin/sh

if [ ! -x ${command} ]; then
    chmod +x ${command}
fi

export RIO_EXEC; RIO_EXEC="rio.exec"

exec ${commandLine} &
echo $! > ${pidFile}