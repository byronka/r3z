#!/bin/sh

cd $R3Z_HOME

# kill the timekeeping application by reading the process id from the "pid"
# file and "kill"-ing it (regular kill is nice.  we're not kill -9'ing here!)
kill $(cat pid)

# get rid of the now obsolete process id file
rm pid
