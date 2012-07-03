#!/bin/sh

cd src
javac -g -d ../bin `find -name "*.java"`
cd ../bin
jar cvf ../commons-csv.jar *
