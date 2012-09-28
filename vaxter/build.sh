#! /bin/sh
mkdir bin
LIBS=""
for f in `find lib -name "*.jar"`; do LIBS=$LIBS":../"$f; done
echo $LIBS
cd src; javac -d ../bin -cp $LIBS `find . -name "*.java"`
cd ../bin; jar cvf ../vaxter.jar *
