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

echo "`date +'%Y%m%d%H%M%S'` START" >> $LOGFILE

# list of publications
PUBLIST="ND AM"

# date delta range
# 1:11 = 1 to 11 days from current date 
DELTARANGE="1:11"

for PUB in $PUBLIST; do
	OUTFILE="budget_${PUB}.xml"
	echo "*** RUN Budget Export: pub=${PUB}, dateDeltaRange=${DELTARANGE}, outFile=${OUTFILE}" >> $LOGFILE
	$INSTALLDIR/run-budget-export.sh -l $PUB -e $DELTARANGE -o $OUTFILE
done

echo "`date +'%Y%m%d%H%M%S'` END" >> $LOGFILE

exit