# Compile this plot using
# 'gnuplot locLockExectime.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'locLockExectime.png'

# Graphics title
set title "Execution Time for different scenarios"

# Set x and y label
set xlabel 'threads'
set ylabel 'time'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 2 is number of threads
# Column 3 is the execution time
plot "locLockExectimeForA1.dat" using 2:3 with lines title 'A1', \
     "locLockExectimeForA2.dat" using 2:3 with lines title 'A2', \
     "locLockExectimeForB1.dat" using 2:3 with lines title 'B1', \
     "locLockExectimeForB2.dat" using 2:3 with lines title 'B2', \

