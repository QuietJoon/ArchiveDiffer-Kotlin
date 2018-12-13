import archive.ArchiveAndStream
import archive.closeArhiveAndStream
import archive.listItems
import java.io.File

import archive.openArchive
import util.*

data class RawFileAnalyzed (
    val paths : String
    , val colorName: String
    , val firstOrSinglePaths: Array<String>)

fun rawFileAnalyze(files: List<File>): RawFileAnalyzed {
    val paths = generateStringFromFileList(files)
    var colorName = if (files.size == 1) "Yellow" else "Green"
    val pathArray = files.map{it.toString()}.toTypedArray()
    val firstOrSinglePaths = getFirstOrSingleArchivePaths(pathArray)
    var anANS: ArchiveAndStream

    for ( aPath in firstOrSinglePaths ) {
        try {
            println("<firstPhase>: opening $aPath")
            anANS = openArchive(aPath)
            listItems(anANS)
            closeArhiveAndStream(anANS)
        } catch (e: Exception) {
            println("[Error]<FirstPhase>: Seems to fail opening")
            colorName = "Red"
        }
    }

    return RawFileAnalyzed (paths, colorName, firstOrSinglePaths)
}
