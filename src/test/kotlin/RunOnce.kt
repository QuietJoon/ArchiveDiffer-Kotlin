fun main (args: Array<String>) {

    val theIgnoringListPath = "H:\\Kazuki\\AD\\IgnoringList.20181214.txt"
    initialize(theIgnoringListPath)
    val theArchivePaths: Array<RealPath> = arrayOf(
        "R:\\TestArchives\\ZA0.rar"
        , "R:\\TestArchives\\ZA1.rar"
        , "R:\\TestArchives\\ZA2.zip"
        , "R:\\TestArchives\\ZA3.zip"
        , "R:\\TestArchives\\ZA4.rar"
    )

    println("Size of IgnoringList: ${theIgnoringList.ignoringList.size}")

    var theTable = makeTheTable(theArchivePaths, theDebugDirectory)
    theTable.prepareWorkingDirectory()

    println("Number of ArchiveSet: ${theTable.archiveSetNum}")

    println("Phase #0")

    println(theTable.theItemMap.size)
    println(theTable.theItemList.size)
    println(theTable.theArchiveSets[0].itemMap.size)

    for ( anArchiveSet in theTable.theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for ( anItemRecord in theTable.theItemMap ) {
        print(anItemRecord.key.toString())
        println(anItemRecord.value.toString())
    }

    println("Difference only")
    var count = 0
    var resultList = mutableListOf<String>()
    for (anItemEntry in theTable.theItemMap) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            count++
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            resultList.add(theString)
            println(theString)
        }
    }


    var runCount = 1
    while(true) {
        println("Phase #$runCount")
        if (theTable.runOnce()) break

        for ( anArchiveSet in theTable.theArchiveSets)
            printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

        for ( anItemRecord in theTable.theItemMap ) {
            print(anItemRecord.key.toString())
            println(anItemRecord.value.toString())
        }

        println("Difference only")
        count = 0
        resultList = mutableListOf<String>()
        for (anItemEntry in theTable.theItemMap) {
            if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
                count++
                val stringBuilder = StringBuilder()
                stringBuilder.append(anItemEntry.key.toString())
                stringBuilder.append(anItemEntry.value.toString())
                val theString = stringBuilder.toString()
                resultList.add(theString)
                println(theString)
            }
        }
        if (runCount > 10) break
        runCount++
    }
}
