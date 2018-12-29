fun main (args: Array<String>) {

    val theIgnoringListPath = "H:\\Kazuki\\AD\\IgnoringList.20181214.txt"
    initialize(theIgnoringListPath)

    /*
    val xArchivePaths: Array<RealPath> = arrayOf(
          "R:\\TestArchives\\XA.rar"
        , "R:\\TestArchives\\XB.rar"
        , "R:\\TestArchives\\XC.zip"
    )

    val xArchiveSetList = mutableListOf<ArchiveSet>()
    xArchivePaths.forEachIndexed { idx, archivePath ->
        val ans = openArchive(archivePath)
        val archiveSet = ArchiveSet(arrayOf(archivePath),idx,idx,ans.inArchive)
        xArchiveSetList.add(archiveSet)
    }

    var xTable = TheTable(xArchiveSetList.toTypedArray(), "R:\\Debug")

    println(xTable.archiveSetNum)
    println(xTable.theItemTable.size)
    println(xTable.theItemList.size)
    println(xTable.theArchiveSets[0].itemMap.size)
    for ( anArchiveSet in xTable.theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for ( anItemRecord in xTable.theItemTable ) {
        print(anItemRecord.key.toString())
        println(anItemRecord.value.toString())
    }

    for (anKey in xTable.theItemTable.keys)
        println(anKey.toString())
    */
    val theArchivePaths: Array<RealPath> = arrayOf(
          "R:\\TestArchives\\ZA0.rar"
        , "R:\\TestArchives\\ZA1.rar"
        , "R:\\TestArchives\\ZA2.zip"
        , "R:\\TestArchives\\ZA3.zip"
        , "R:\\TestArchives\\ZA4.rar"
    )
    /*
    val theArchivePaths: Array<RealPath> = arrayOf(
          "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA1.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA2.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA3.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA4.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA5.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA6.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA7.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA8.rar"
        , "H:\\Inad\\ARIA\\[DVDISO] -ARIA- (3 seasons+OVA+α)\\ARIA The NATURAL　vol.01～09\\ARIA2_NA9.rar"
    )
    */

    var theTable = makeTheTable(theArchivePaths, "R:\\Debug")

    println(theIgnoringList.ignoringList.size)
    println(theTable.archiveSetNum)
    println(theTable.theItemTable.size)
    println(theTable.theItemList.size)
    println(theTable.theArchiveSets[0].itemMap.size)
    for ( anArchiveSet in theTable.theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for ( anItemRecord in theTable.theItemTable ) {
        print(anItemRecord.key.toString())
        println(anItemRecord.value.toString())
    }

/*
    for (anKey in theTable.theItemTable.keys)
        println(anKey.toString())
*/
}
