#!/bin/sh
set -e

maven=$(which mvn 2>/dev/null)
if [ -z "$maven" ]; then
    echo "Maven needs to be installed."
    echo Either "run apt-get install maven" or "yum install maven" depending on your distribution.
    exit 1
fi

curl=$(which curl 2>/dev/null)
wget=$(which wget 2>/dev/null)
if [ -z "$wget" -a -z "$curl" ]; then
    echo "wget or curl needs to be installed."
    echo Either run "apt-get install wget" or "yum install wget" depending on your distribution
    exit 1
fi

if [ ! -z "$curl" ]; then
    curl --silent https://s3.amazonaws.com/sortable-public/challenge/challenge_data_20110429.tar.gz > challenge_data_20110429.tar.gz
else 
    wget --quiet https://s3.amazonaws.com/sortable-public/challenge/challenge_data_20110429.tar.gz
fi

tar zxf challenge_data_20110429.tar.gz
mvn install -q -e
mvn -q exec:java
rm challenge_data_20110429.tar.gz products.txt listings.txt

