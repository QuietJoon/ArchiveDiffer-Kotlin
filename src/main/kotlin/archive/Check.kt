package archive

import ArchiveSetPaths
import Message
import MessageType

// TODO: Implement checking integrity of archive
fun checkArchiveIntegrity(packagedFilePaths: Array<ArchiveSetPaths>): Message {
    return Pair(MessageType.NoProblem, "No Problem\nwith Archive")
}
