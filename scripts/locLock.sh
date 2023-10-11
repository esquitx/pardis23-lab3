#!/bin/bash

echo "Compiling ..."
cd ..
cd src
javac -d ../bin Measurement.java
cd ..

echo "Script initiated at `date` on `hostname`"

## declare variable types
types=("A1" "A2" "B1" "B2")

for type in "${!types[@]}"
do
## get filepath
filepath=data/locLockExectimeFor${type}.dat

## declare num of variables
numThreads=(1 2 4 8 16 32 64 96)

for threadCount in "${numThreads[@]}"
do
java -cp ./bin Measurement "locLock" "$type" $threadCount 1000000 1000000 1000000 >> $filepath
done
echo "Script finished at `date` on `hostname`"