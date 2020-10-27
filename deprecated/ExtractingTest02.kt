import java.io.IOException
import java.io.RandomAccessFile
import java.util.Arrays

import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

class MyExtractCallback02(private val inArchive: IInArchive) : IArchiveExtractCallback {
    private var hash = 0
    private var size = 0
    private var index: Int = 0
    private var skipExtraction: Boolean = false

    @Throws(SevenZipException::class)
    override fun getStream(
        index: Int,
        extractAskMode: ExtractAskMode
    ): ISequentialOutStream? {
        this.index = index
        skipExtraction = inArchive
            .getProperty(index, PropID.IS_FOLDER) as Boolean
        return if (skipExtraction || extractAskMode != ExtractAskMode.EXTRACT) {
            null
        } else ISequentialOutStream { data ->
            hash = hash xor Arrays.hashCode(data)
            size += data.size
            data.size // Return amount of proceed data
        }
    }

    @Throws(SevenZipException::class)
    override fun prepareOperation(extractAskMode: ExtractAskMode) {
    }

    @Throws(SevenZipException::class)
    override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
        if (skipExtraction) {
            return
        }
        if (extractOperationResult != ExtractOperationResult.OK) {
            System.err.println("Extraction error")
        } else {
            println(
                String.format(
                    "%9X | %10s | %s", hash, size, //
                    inArchive.getProperty(index, PropID.PATH)
                )
            )
            hash = 0
            size = 0
        }
    }

    @Throws(SevenZipException::class)
    override fun setCompleted(completeValue: Long) {
    }

    @Throws(SevenZipException::class)
    override fun setTotal(total: Long) {
    }
}

fun main(args: Array<String>) {
    //val thePath = "R:\\TestArchives\\MultiVolume.part1.rar"
    //val thePath = "R:\\TestArchives\\SingleVolume.rar"
    val thePath = "R:\\TestArchives\\WhereIs.rar"

    var randomAccessFile: RandomAccessFile? = null
    lateinit var inArchive: IInArchive
    try {
        randomAccessFile = RandomAccessFile(thePath, "r")
        inArchive = SevenZip.openInArchive(
            null, // autodetect archive type
            RandomAccessFileInStream(randomAccessFile)
        )

        println("   Hash   |    Size    | Filename")
        println("----------+------------+---------")

        val `in` = IntArray(inArchive.numberOfItems)
        for (i in `in`.indices) {
            `in`[i] = i
        }
        inArchive.extract(
            `in`, false, // Non-test mode
            MyExtractCallback02(inArchive)
        )
    } catch (e: Exception) {
        System.err.println("Error occurs: $e")
    } finally {
        try {
            inArchive.close()
        } catch (e: SevenZipException) {
            System.err.println("Error closing archive: $e")
        }

        if (randomAccessFile != null) {
            try {
                randomAccessFile.close()
            } catch (e: IOException) {
                System.err.println("Error closing file: $e")
            }

        }
    }
}
