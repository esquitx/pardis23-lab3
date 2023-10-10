#!/bin/bash

echo "Compiling ..."
cd ..
cd src
javac -d ../bin TimeMeasurement.java
cd ..


echo "Script initiated at `date` on `hostname`"

## declare num of variables
declare -a numThreads=(1 2 4 8 16 32 64 96)

for threadCount in "${numThreads[@]}"
do
srun java -cp ./bin TimeMeasurement "A1" $threadCount 1000000 1000000 1000000 >> data/pdcExectimeForA.dat
srun java -cp ./bin TimeMeasurement "A2" $threadCount 1000000 1000000 1000000 >> data/pdcExectimeForB.dat
done
echo "Script finished at `date` on `hostname`"
