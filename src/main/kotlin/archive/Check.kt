package archive

import ArchiveSetPaths
import Message
import MessageType
import util.checkMissingMultiVolumeFile
import directoryDelimiter
import Path

// TODO: Implement checking missing volume or integrity of archive
fun checkArchiveVolume(packagedFilePaths: Array<ArchiveSetPaths>): Message {
    if (packagedFilePaths.size <= 1)
        return Pair(MessageType.Warning, "Only one\nArchiveSet")
    val paths = mutableListOf<Path>()
    for (archiveSetPaths in packagedFilePaths) {
        for (archivePaths in archiveSetPaths) {
            for (aPath in archivePaths) {
                paths.add(aPath.joinToString(separator = directoryDelimiter))
            }
        }
    }
    val checks = checkMissingMultiVolumeFile(paths)
    val missings = mutableListOf<Path>()
    val corrupts = mutableListOf<Path>()
    var mcPaths = ""
    var isMissing = false
    for (check in checks) {
        if (check.isMissing) {
            isMissing = true
            for (missing in check.missingVolumes) {
                missings.add(missing)
                mcPaths += missing + "\n"
            }
            for (corrupted in check.corruptedVolumes) {
                corrupts.add(corrupted)
                mcPaths += corrupted + "\n"
            }
        }
    }
    if (isMissing) {
        return Pair(MessageType.Critical, "Missing volume\n${mcPaths}")
    }
    return Pair(MessageType.NoProblem, "No Problem\nwith Archive Volume")
}
