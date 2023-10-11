#!/bin/bash

echo "Compiling ..."
cd ..
cd src
javac -d ../bin Measurement.java
cd ..


echo "Script initiated at `date` on `hostname`"

## declare num of variables
declare -a numThreads=(1 2 4 8)

for threadCount in "${numThreads[@]}"
do
java -cp ./bin Measurement "basic" "A1" $threadCount 100000 100000 100000 >> data/localExectimeForA.dat
java -cp ./bin Measurement "basic" "A2" $threadCount 100000 100000 100000 >> data/localExectimeForB.dat
done
echo "Script finished at `date` on `hostname`"
