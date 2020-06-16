#!/bin/sh

find_children() {
    for child in `ps ${psOptions} pid,ppid | grep $pid | awk '{ if ( $2 == "$pid" ) { print $1 }}'`
    do
      pids="$child $pids"
      pid=$child
      find_children
    done
}

pid=${pid}
find_children

pids="$pid $pids"
kill -9 $pids
rm $0
