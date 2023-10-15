#!/bin/bash
# Set the allocation to be charged for this job
# not required if you have set a default allocation
#SBATCH -A edu23.dd2443
# The name of the script is myjob
#SBATCH -J measurePDC
# 10 minutes wall-clock time will be given to this job
#SBATCH -t 01:00:00
# The partition
#SBATCH -p main


echo "Compiling ..."
cd ..
cd src
javac -d ../bin Measurement.java
cd ..


echo "Script initiated at `date` on `hostname`"

## declare variable types
types=("A1" "A2" "B1" "B2")

for type in "${types[@]}"
do
## get filepath
filepath=data/pdcExectimeFor${type}.dat

## declare num of variables
numThreads=(1 2 4 8 16 32 64 96)

## thread loop
for threadCount in "${numThreads[@]}"
do
srun java -cp ./bin Measurement "basic" "$type" $threadCount 1000000 1000000 1000000 >> $filepath
done
##

done
echo "Script finished at `date` on `hostname`"