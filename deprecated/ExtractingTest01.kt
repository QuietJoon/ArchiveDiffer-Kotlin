import archive.*
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Arrays


fun main(args : Array<String>) {
    println("Multi-volume RAR Test")
    if (!jBindingChecker()) {
        println("7-Zip JBinding linking error")
    }

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
        ) ?: error("[Error]<ExtractingTest01>: Fail to open")

        // Getting simple interface of the archive inArchive
        val simpleInArchive = inArchive.simpleInterface

        println("   Hash   |    Size    | Filename")
        println("----------+------------+---------")

        for (item in simpleInArchive.archiveItems) {
            val hash = intArrayOf(0)
            if (!item.isFolder) {
                val result: ExtractOperationResult

                val sizeArray = LongArray(1)
                result = item.extractSlow { data ->
                    hash[0] = hash[0] xor Arrays.hashCode(data) // Consume data
                    sizeArray[0] += data.size.toLong()
                    data.size // Return amount of consumed data
                }

                println(
                    String.format(
                        "%9X | %10s | %s",
                        hash[0], sizeArray[0], item.path
                    )
                )
                if (result == ExtractOperationResult.OK) {

                } else {
                    System.err.println("Error extracting item: $result")
                }
            }
        }
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
