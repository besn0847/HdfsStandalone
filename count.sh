#!/bin/sh

cat /dev/null > /tmp/$$.csv

for DIR in src-hdfs/org/apache/hadoop/hdfs/server/datanode/ src-hdfs/org/apache/hadoop/hdfs/server/namenode/ 
do
	DIRECTORY=`echo $DIR | sed -e 's/\//_/g' `
	cat /dev/null > /tmp/$DIRECTORY.csv

	for FILE in `find $DIR | egrep  '\.java$' | egrep -v "\.svn"`
	do
		CODELINE=`cat $FILE | wc -l`
		echo $CODELINE >> /tmp/$DIRECTORY.csv
		# echo "$FILE: $CODELINE" >> /tmp/$DIRECTORY-file.csv
	done
	
	LINES=0
	#cat /tmp/$DIRECTORY.csv | while read LINE
	for LINE in `cat /tmp/$DIRECTORY.csv`
	do
		LINES=`expr $LINES + $LINE`
	done

	echo "$DIRECTORY,$LINES" >> /tmp/$$.csv
done

