val isWindows : Boolean = System.getProperty("os.name").startsWith("Windows")
val directoryDelimiter = if (isWindows) { "\\"} else { "/" }
