#!/bin/bash

echo "Compiling ..."
cd ..
cd src
javac -d ../bin Measurement.java
cd ..


echo "Script initiated at `date` on `hostname`"

## declare num of variables
numThreads=(1 2 4 8)

## thread loop
for threadCount in "${numThreads[@]}"
do
java -cp ./bin Measurement "basic" "A1" $threadCount 100000 100000 100000 >> data/locLockExectimeForA.dat
java -cp ./bin Measurement "basic" "A2" $threadCount 100000 100000 100000 >> data/locLockExectimeForB.dat
done
##

echo "Script finished at `date` on `hostname`"
