#!/bin/sh

this_script=$0
delay=3
pid=${pid}
process_status_file=${process_status_file}
ps -p$pid | grep $pid 2>&1 > /dev/null

# Grab the status of the ps | grep command 
status=$?
echo "$status" > $process_status_file

# A value of 0 means that it was found running
if [ "$status" = "0" ]; then
    while [ "$status" = "0" ]
        do
            sleep $delay
            ps -p$pid | grep $pid 2>&1 > /dev/null
            status=$?
            echo "$status" > $process_status_file
        done
fi

rm $this_script