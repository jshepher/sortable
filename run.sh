#!/bin/sh

maven=$(which mvn)
if [ -z "$maven" ]; then
    echo "Maven needs to be installed."
    echo Either "run apt-get install maven" or "yum install maven" depending on your distribution.
    exit 1
fi

wget=$(which wget)
if [ -z "$wget" ]; then
    echo "wget needs to be installed."
    echo Either run "apt-get install wget" or "yum install wget" depending on your distribution
fi

wget --quiet https://s3.amazonaws.com/sortable-public/challenge/challenge_data_20110429.tar.gz
tar zxf challenge_data_20110429.tar.gz
mvn install -q -e
mvn -q exec:java
rm challenge_data_20110429.tar.gz products.txt listings.txt

