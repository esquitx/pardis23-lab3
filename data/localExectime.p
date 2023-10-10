# Compile this plot using
# 'gnuplot exectime.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'localExectime.png'

# Graphics title
set title "Execution Time for different scenarios"

# Set x and y label
set xlabel 'threads'
set ylabel 'time'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 2 is number of threads
# Column 3 is the execution time
plot "localExectimeForA.dat" using 2:3 with lines title 'A', \
     "localExectimeForB.dat" using 2:3 with lines title 'B', \
     

