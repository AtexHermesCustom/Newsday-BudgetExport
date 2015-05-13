#!/bin/ksh
#############################################################################
#
# Starter script for Budget Export
#
# Modification History
# 20150510 jpm creation
#
#############################################################################

INSTALLDIR=`dirname $0`
LIBDIR=$INSTALLDIR/lib
CONFDIR=$INSTALLDIR/conf

test -z "$INSTALLDIR" && INSTALLDIR=.

# Defaults
PUB="ND"
PUBDATE="`date +%Y%m%d`"

# Input arguments
while getopts l:d:t argswitch
do
	case $argswitch in
		l)	PUB=$OPTARG;;
		d)	PUBDATE=$OPTARG;;
		t)	TESTFLAG=1;;
		\?)	printf "Usage: %s: [-l publication -d pubDate]\n" `basename $0`
			exit 2;;
	esac
done

# Export arguments
if [ ! -z "$PUB" -a ! -z "$PUBDATE" ]; then
	XARGS="-l $PUB -d $PUBDATE -c $BATCH_USR:$BATCH_PWD"
else
	printf "Usage: %s: [-l publication -p pubDate]\n" `basename $0`
	exit 2
fi

# set config files
PROPS=budget-export.properties
if [ "$TESTFLAG" = "1" ]; then
    PROPS=budget-export-test.properties
fi
LOGPROPS=budget-log.properties

# set class path
CLASSPATH=$INSTALLDIR
for j in `find $HERMES/classes -type f -name '*'.jar -print`
do
	CLASSPATH="$CLASSPATH:$j"
done
for j in `find $LIBDIR -type f -name '*'.jar -print`
do
	CLASSPATH="$CLASSPATH:$j"
done
export CLASSPATH

# initiate export
COMMAND="$JAVA_HOME/bin/java 
	-Djava.security.policy=$CONFDIR/app.policy -Djava.security.manager -Djava.security.auth.login.config=$CONFDIR/auth.conf 
	-Djndi.properties=$CONFDIR/jndi.properties -Djavax.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl
	-Djava.util.logging.config.file=$CONFDIR/$LOGPROPS
	com.atex.h11.custom.newsday.export.budget.Main -p $CONFDIR/$PROPS $XARGS"
echo $COMMAND
exec $COMMAND
