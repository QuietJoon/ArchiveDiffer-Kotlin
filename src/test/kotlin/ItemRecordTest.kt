import archive.*
import util.packageFilePathsWithoutGuide


fun main (args: Array<String>) {

    val theArchivePaths: List<RealPath> = listOf(
          "R:\\TestArchives\\Source.zip"
        , "R:\\TestArchives\\SourceMultiRAR4.part1.rar"
        , "R:\\TestArchives\\SourceMultiRAR5.part1.rar"
        , "R:\\TestArchives\\SourceRAR4.rar"
        , "R:\\TestArchives\\SourceRAR5.rar"
    )

    val archiveSetList = mutableListOf<ArchiveSet>()

    packageFilePathsWithoutGuide(theArchivePaths).forEachIndexed { idx, archiveSetPaths ->
        val archiveSet = ArchiveSet(idx,archiveSetPaths)
        archiveSetList.add(archiveSet)
    }

    val theIgnoringListPath = "U:\\Kazuki\\AD\\IgnoringList.20181214.txt"
    val theIgnoringList = readIgnoringList(theIgnoringListPath)
    printIgnoringListWithLevel(theIgnoringList)

    for ( anArchiveSet in archiveSetList) {
        for ( anArchive in anArchiveSet.archiveMap) {
            val notIgnoringItemIDArray = getIDArrayWithoutIgnoringItem(anArchive.value.ans.inArchive, theIgnoringList)
            printItemListByIDs(anArchive.value.ans.inArchive, notIgnoringItemIDArray)
        }
    }
}
