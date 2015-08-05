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

LOGFILENAME="`basename $0 | cut -d. -f1`.log"
LOGFILE=$LOGDIR/$LOGFILENAME

echo "`date +'%Y%m%d%H%M%S` START" >> $LOGFILE

# list of publications
PUBLIST="ND AM"

# date deltas to process
DELTALIST="3 4 5 6 7 8 9 10 11"

for DELTA in $DELTALIST; do
	for PUB in $PUBLIST; do
		OUTFILE="budget_${PUB}_${DELTA}.xml"
		echo "*** RUN Budget Export: pub=${PUB}, dateDelta=${DELTA}, outFile=${OUTFILE}" >> $LOGFILE
		$INSTALLDIR/run-budget-export.sh -l $PUB -e $DELTA -o $OUTFILE >> $LOGFILE 2>&1
	done
done

echo "`date +'%Y%m%d%H%M%S` END" >> $LOGFILE

exit