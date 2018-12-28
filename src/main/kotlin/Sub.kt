fun initialize(ignoringListConfigPath: RealPath) {
    // Set IgnoringList
    theIgnoringList = readIgnoringList(ignoringListConfigPath)
}

fun makeTheTable(theArchiveSetPaths: Array<ArchiveSetPaths>, rootOutputDirectory: String): TheTable {
    val archiveSetList = mutableListOf<ArchiveSet>()
    theArchiveSetPaths.forEachIndexed { idx, anArchiveSetPaths ->
        val anArchiveSet = ArchiveSet(idx, anArchiveSetPaths)

        archiveSetList.add(anArchiveSet)
    }

    return TheTable(archiveSetList.toTypedArray(), rootOutputDirectory)
}
