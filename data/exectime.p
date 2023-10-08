# Compile this plot using
# 'gnuplot exectime.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'exectime.png'

# Graphics title
set title "Execution Time for different scenarios"

# Set x and y label
set xlabel 'threads'
set ylabel 'time'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 1 is number of threads
# Column 2, 3, 4, 5 & 6 are the speedup
plot "exectimeA1.dat" using 1:2 with lines title 'A1', \
     "exectimeA2.dat" using 1:2 with lines title 'A2', \
     "exectimeB1.dat" using 1:2 with lines title 'B1', \
     "exectimeB2.dat" using 1:2 with lines title 'B2', \

