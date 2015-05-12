#!/bin/ksh

DEBUGDIR_BUDGET=${HERMES}/local/h11export/debug/budget

# delete old files older than 5 days
find ${DEBUGDIR_BUDGET} -name "*.*" -mtime +5 -exec rm -f {} \;

exit
