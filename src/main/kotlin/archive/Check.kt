package archive

import ArchiveSetPaths
import Message
import MessageType

// TODO: Implement checking missing volume
fun checkArchiveVolume(packagedFilePaths: Array<ArchiveSetPaths>): Message {
    if (packagedFilePaths.size <= 1)
        return Pair(MessageType.Warning, "Only one\nArchiveSet")
    return Pair(MessageType.NoProblem, "No Problem\nwith Archive Volume")
}