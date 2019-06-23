Data Flow
====

## Summary

Minimize data read/write

## On-memory data flow

### Purpose

When matching files, `path` data is not required.
In other words, the program does not copy and read `path` data on matching phase.

Instead, copy `path` data when generating `ResultView`.
Based on `path` data, the program generate:

* Same file name, but different directory path
* Infer file & directory structure
* etc.
