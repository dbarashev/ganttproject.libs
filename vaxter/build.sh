#! /bin/sh
mkdir bin
LIBS=""
for f in `find lib -name "*.jar"`; do LIBS=$LIBS":../"$f; done
echo $LIBS
cd src; javac -g -d ../bin -cp $LIBS `find . -name "*.java"`
jar cvf ../vaxter-src.jar *
cd ../bin; jar cvf ../vaxter.jar *
