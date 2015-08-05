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

### Defaults
##PUB="ND"
##PUBDATE="`date +%Y%m%d`"

# Input arguments
while getopts l:d:e:o:t argswitch
do
	case $argswitch in
		l) PUB=$OPTARG;;
		d) PUBDATE=$OPTARG;;
		e) DATEDELTA=$OPTARG;;
		o) OUTPUTFILENAME=$OPTARG;;
		t) TESTFLAG=1;;
		\?) printf "Usage: %s -l publication [-d pubDate | -e daysFromToday] [-o outputFilename]\n" `basename $0`
			exit 2;;
	esac
done

# Export arguments
if [[ ! -z $PUB && (! -z $PUBDATE || ! -z $DATEDELTA) ]]; then
	XARGS="-c $BATCH_USR:$BATCH_PWD -l $PUB"
	
	if [[ ! -z $PUBDATE ]]; then
		XARGS="$XARGS -d $PUBDATE"
	elif [[ ! -z $DATEDELTA ]]; then
		XARGS="$XARGS -e $DATEDELTA"
	fi
	
	if [[ ! -z $OUTPUTFILENAME ]]; then
		XARGS="$XARGS -o $OUTPUTFILENAME"
	fi
	
else
	printf "Usage: %s -l publication [-d pubDate | -e daysFromToday] [-o outputFilename]\n" `basename $0`
	exit 2
fi

# set config files
PROPS=budget-export.properties
if [[ $TESTFLAG -eq 1 ]]; then
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
