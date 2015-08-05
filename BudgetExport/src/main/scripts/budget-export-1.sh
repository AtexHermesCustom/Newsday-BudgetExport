#!/bin/ksh
#############################################################################
#
# Wrapper script for Budget Export
#
# Modification History
# 20150805 jpm creation
#
#############################################################################

INSTALLDIR=`dirname $0`
test -z "$INSTALLDIR" && INSTALLDIR=.

# list of publications
PUBLIST="ND AM"

# date delta
DELTALIST="1 2"

for DELTA in $DELTALIST; do
	for PUB in $PUBLIST; do
		echo "Run Budget Export for pub=${PUB}, dateDelta=${DELTA}\n"
		$INSTALLDIR/run-budget-export.sh -l $PUB -e $DELTA
	done
done