// Originally comes from example code for SevenZip-JBindings

package archive

import java.io.*
import java.util.HashMap

import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

import util.*
import RealPath


class ArchiveAndStream(
    val inArchive: IInArchive,
    private var randomAccess: RandomAccessFile?,
    private var archiveCallback: ArchiveOpenVolumeCallback?
) {
    private fun isSingle() = randomAccess != null
    fun isMulti() = archiveCallback != null

    fun close() {
        try {
            this.inArchive.close()
        } catch (e: SevenZipException) {
            System.err.println("Error closing archive: $e")
            throw e
        }
        if (this.isSingle()) {
            try {
                this.randomAccess?.close()
            } catch (e: IOException) {
                System.err.println("Error closing file: $e")
                throw e
            }
        } else {
            try {
                this.archiveCallback?.close()
            } catch (e: IOException) {
                System.err.println("Error closing file: $e")
                throw e
            }
        }
    }
}


fun openArchive(aFilePath: RealPath): ArchiveAndStream? {
    if (!File(aFilePath).exists()) {
        throw ExtractionException("Archive file not found: $aFilePath")
    }
    if (!File(aFilePath).canRead()) {
        print("Can't read archive file: $aFilePath\n")
    }

    return if (aFilePath.isSingleVolume())
        openSingleVolumeArchive(aFilePath)
    else openMultiVolumeArchive(aFilePath)
}

private fun openSingleVolumeArchive(aFilePath: RealPath): ArchiveAndStream? {
    print("Open single volume with $aFilePath\n")

    val randomAccessFile: RandomAccessFile
    val inArchive: IInArchive
    val inStream: IInStream?
    try {
        randomAccessFile = RandomAccessFile(aFilePath, "r")
        inStream = RandomAccessFileInStream(randomAccessFile)
    } catch (e: Exception) {
        System.err.println("[Error]<openArchive>: Fail to open RandomAccessFile with $aFilePath")
        System.err.println(e.toString())
        throw e
    }
    try {
        inArchive = SevenZip.openInArchive(
            null,
            inStream
        )
    } catch (e: Exception) {
        randomAccessFile.close()
        System.err.println("[Error]<openArchive>: Fail to open InArchive with $aFilePath")
        System.err.println(e.toString())
        return null
    }

    if (!inArchive.archiveFormat.isAllowedArchives()) return null

    return ArchiveAndStream(inArchive, randomAccessFile, null)
}


private fun openMultiVolumeArchive(aFilePath: RealPath): ArchiveAndStream? {
    print("Open multi-volume with $aFilePath\n")

    val archiveOpenVolumeCallback: ArchiveOpenVolumeCallback
    val inArchive: IInArchive
    val inStream: IInStream?
    try {
        archiveOpenVolumeCallback = ArchiveOpenVolumeCallback()
        inStream = archiveOpenVolumeCallback.getStream(aFilePath)
    } catch (e: Exception) {
        System.err.println("[Error]<openMultiVolumeArchive>: Fail to open IInStream with $aFilePath")
        System.err.println(e.toString())
        throw e
    }
    try {
        inArchive = SevenZip.openInArchive(
            null, inStream,
            archiveOpenVolumeCallback
        )
    } catch (e: Exception) {
        archiveOpenVolumeCallback.close()
        System.err.println("[Error]<openArchive>: Fail to open InArchive with $aFilePath")
        System.err.println(e.toString())
        return null
    }

    if (!inArchive.archiveFormat.isAllowedArchives()) return null

    return ArchiveAndStream(inArchive, null, archiveOpenVolumeCallback)
}

private fun ArchiveFormat.isAllowedArchives() = theAllowedSet.contains(this)

class ArchiveOpenVolumeCallback : IArchiveOpenVolumeCallback, IArchiveOpenCallback {

    private val openedRandomAccessFileList = HashMap<RealPath, RandomAccessFile>()
    private var name: RealPath? = null

    @Throws(SevenZipException::class)
    override fun getProperty(propID: PropID) =
        when (propID) {
            PropID.NAME -> name
            else -> null
        }

    @Throws(SevenZipException::class)
    override fun getStream(filename: RealPath): IInStream? {
        try {
            // We use caching of opened streams, so check cache first
            var randomAccessFile: RandomAccessFile? = openedRandomAccessFileList[filename]
            if (randomAccessFile != null) { // Cache hit.
                // Move the file pointer back to the beginning
                // in order to emulating new stream
                randomAccessFile.seek(0)

                // Save current volume name in case getProperty() will be called
                name = filename

                return RandomAccessFileInStream(randomAccessFile)
            }

            // Nothing useful in cache. Open required volume.
            randomAccessFile = RandomAccessFile(filename, "r")
            // Put new stream in the cache
            openedRandomAccessFileList[filename] = randomAccessFile
            // Save current volume name in case getProperty() will be called
            name = filename

            return RandomAccessFileInStream(randomAccessFile)
        } catch (fileNotFoundException: FileNotFoundException) {
            return null
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    internal fun close() {
        for (file in openedRandomAccessFileList.values) {
            file.close()
        }
    }

    @Throws(SevenZipException::class)
    override fun setCompleted(files: Long?, bytes: Long?) {
    }

    @Throws(SevenZipException::class)
    override fun setTotal(files: Long?, bytes: Long?) {
    }
}
