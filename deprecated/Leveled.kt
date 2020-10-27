import archive.openArchive

fun main (args: Array<String>) {
    val ans = openArchive("U:\\Kazuki\\BadPattern\\BadPattern.rar") ?: error("[Error]<Leveled>: Fail to open")

    var rawIgnoringList: MutableList<IgnoringItem> = mutableListOf()

    val sArchive = ans.inArchive.simpleInterface

    for (item in sArchive.archiveItems) {
        rawIgnoringList.add(makeItemFromRawItem(item))
    }
    val ignoringList = IgnoringList(rawIgnoringList.toList())
    ignoringList.printIgnoringList()

    val outputPath = "U:\\Kazuki\\BadPattern\\IgnoringList.txt"
    writeIgnoringList(ignoringList, outputPath)


    val newIgnoringList = readIgnoringList(outputPath)

    newIgnoringList.printIgnoringListWithLevel()

    println("Modified")
    val modifiedPath = "R:\\TestArchives\\ModifiedIgnoringList.txt"
    val modifiedIgnoringList = readIgnoringList(modifiedPath)
    modifiedIgnoringList.printIgnoringListWithLevel()
}
