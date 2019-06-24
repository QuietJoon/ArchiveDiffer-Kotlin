package archive

import ArchiveSetPaths
import MessageType

// TODO: Not yet implemented
fun checkArchiveVolume(packagedFilePaths: Array<ArchiveSetPaths>): Pair<MessageType,String> {
    if (packagedFilePaths.size <= 1)
        return Pair(MessageType.Warning, "Only one\nArchiveSet")
    return Pair(MessageType.NoProblem, "No Problem\nwith Archive Volume")
}