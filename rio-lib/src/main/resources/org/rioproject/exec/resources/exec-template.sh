#!/bin/sh

if [ ! -x ${command} ]; then
    chmod +x ${command}
fi

export RIO_X; RIO_X="Exec'd by Rio"

${commandLine} &
echo $! > ${pidFile}