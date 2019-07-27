#!/usr/bin/env bash

#cd src/transaction
#make clean
#make server
#make client
#
## run
#make runregistry &
#make runtm &
#make runrmflights &
#make runrmrooms &
#make runrmcars &
#make runrmcustomers &
#make runwc &

cd src/test.part2
rm results/* -rf
export CLASSPATH=.:gnujaxp.jar
javac RunTests.java
java -DrmiPort=3345 RunTests MASTER.xml