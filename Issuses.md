Issues
====

## Trouble with Full-width character

Even I use ICU4J, I couldn't resolve Full-width character viewing problem.

## Serial numbers

Archive's and Item's serial number system is not thread-safe.
However, a single serial number appears one time only for each TheTable.
Therefore, this does not make any problems.

### Archive's serial number

This may have a problem.
Because it's extracting working directory may conflict.
