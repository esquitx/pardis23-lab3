# Compile this plot using
# 'gnuplot noLockDiscrepancies.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'noLockDiscrepancies.png'

# Graphics title
set title "Discrepancies for different execution types"

# Set x and y label
set xlabel 'threads'
set ylabel '# discrepancies'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 2 is number of threads
# Column 4 is the execution time
plot "noLockExectimeForA1.dat" using 2:4 with lines title 'A1', \
     "noLockExectimeForA2.dat" using 2:4 with lines title 'A2', \
     "noLockExectimeForB1.dat" using 2:4 with lines title 'B1', \
     "noLockExectimeForB2.dat" using 2:4 with lines title 'B2', \

