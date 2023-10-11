#!/bin/bash

echo "Compiling ..."
cd ..
cd src
javac -d ../bin Measurement.java
cd ..


echo "Script initiated at `date` on `hostname`"

## get filepath
type=$1
filepath=data/globLockExectimeFor${type}.dat

## declare num of variables
numThreads=(1 2 4 8 16 32 64 96)

## thread loop
for threadCount in "${numThreads[@]}"
do
java -cp ./bin Measurement "globlock" "$type" $threadCount 1000000 1000000 1000000 >> $filepath
done
##

echo "Script finished at `date` on `hostname`"