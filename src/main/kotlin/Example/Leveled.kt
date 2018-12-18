import archive.openArchive

fun main (args: Array<String>) {
    val ans = openArchive("R:\\TestArchives\\BadPattern.2018.12.18.rar")

    var rawIgnoringList: MutableList<IgnoringItem> = mutableListOf()

    val sArchive = ans.inArchive.simpleInterface

    for (item in sArchive.archiveItems) {
        rawIgnoringList.add(makeItemFromRawItem(item))
    }
    val ignoringList = IgnoringList(rawIgnoringList.toList())
    printIgnoringList(ignoringList)

    val outputPath = "R:\\TestArchives\\IgnoringList.txt"
    writeIgnoringList(ignoringList, outputPath)


    val newIgnoringList = readIgnoringList(outputPath)

    printIgnoringListWithLevel(newIgnoringList)

    println("Modified")
    val modifiedPath = "R:\\TestArchives\\ModifiedIgnoringList.txt"
    val modifiedIgnoringList = readIgnoringList(modifiedPath)
    printIgnoringListWithLevel(modifiedIgnoringList)
}
