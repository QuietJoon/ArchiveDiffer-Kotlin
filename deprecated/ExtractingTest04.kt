/*
Copy from https://gist.github.com/borisbrodski/6120309
 */

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.ArrayList
import java.util.regex.Pattern

import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

import archive.*

class Extract04 internal constructor(
    private val archive: String,
    private val outputDirectory: String,
    private val test: Boolean,
    filter: String?
) {
    private var outputDirectoryFile: File? = null
    private val filterRegex: String?

    inner class ExtractCallback(private val inArchive: IInArchive) : IArchiveExtractCallback {
        private var index: Int = 0
        private var outputStream: OutputStream? = null
        private var file: File? = null
        private var extractAskMode: ExtractAskMode? = null
        private var isFolder: Boolean = false

        @Throws(SevenZipException::class)
        override fun setTotal(total: Long) {}

        @Throws(SevenZipException::class)
        override fun setCompleted(completeValue: Long) {}

        @Throws(SevenZipException::class)
        override fun getStream(
            index: Int,
            extractAskMode: ExtractAskMode
        ): ISequentialOutStream? {
            closeOutputStream()

            this.index = index
            this.extractAskMode = extractAskMode
            this.isFolder = inArchive.getProperty(
                index,
                PropID.IS_FOLDER
            ) as Boolean

            if (extractAskMode != ExtractAskMode.EXTRACT) {
                // Skipped files or files being tested
                return null
            }

            var path = inArchive.getProperty(index, PropID.PATH) as String
            path = path.replace("\\s*\\\\".toRegex(), "\\\\").trim { it <= ' ' }
            file = File(outputDirectoryFile, path)
            if (isFolder) {
                createDirectory(file!!)
                return null
            }

            createDirectory(file!!.parentFile)

            try {
                outputStream = FileOutputStream(file!!)
            } catch (e: FileNotFoundException) {
                throw SevenZipException("Error opening file: " + file!!.absolutePath, e)
            }

            return ISequentialOutStream { data ->
                try {
                    outputStream!!.write(data)
                } catch (e: IOException) {
                    throw SevenZipException("Error writing to file: " + file!!.absolutePath)
                }

                data.size // Return amount of consumed data
            }
        }

        @Throws(SevenZipException::class)
        private fun createDirectory(parentFile: File) {
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw SevenZipException("Error creating directory: " + parentFile.absolutePath)
                }
            }
        }

        @Throws(SevenZipException::class)
        private fun closeOutputStream() {
            if (outputStream != null) {
                try {
                    outputStream!!.close()
                    outputStream = null
                } catch (e: IOException) {
                    throw SevenZipException("Error closing file: " + file!!.absolutePath)
                }

            }
        }

        @Throws(SevenZipException::class)
        override fun prepareOperation(extractAskMode: ExtractAskMode) {

        }

        @Throws(SevenZipException::class)
        override fun setOperationResult(
            extractOperationResult: ExtractOperationResult
        ) {
            closeOutputStream()
            val path = inArchive.getProperty(index, PropID.PATH) as String
            if (extractOperationResult != ExtractOperationResult.OK) {
                throw SevenZipException("Invalid file: $path")
            }

            if (!isFolder) {
                when (extractAskMode) {
                    ExtractAskMode.EXTRACT -> println("Extracted $path")
                    ExtractAskMode.TEST -> println("Tested $path")
                    else ->error("[Error]<setOperationResult>: Unknown mode: $extractAskMode")
                }
            }
        }

    }

    init {
        this.filterRegex = filterToRegex(filter)
    }

    @Throws(ExtractionException::class)
    internal fun extract() {
        checkArchiveFile()
        prepareOutputDirectory()
        extractArchive()
    }

    @Throws(ExtractionException::class)
    private fun prepareOutputDirectory() {
        outputDirectoryFile = File(outputDirectory)
        if (!outputDirectoryFile!!.exists()) {
            outputDirectoryFile!!.mkdirs()
        } else {
            if (outputDirectoryFile!!.list()!!.isNotEmpty()) {
                throw ExtractionException("Output directory not empty: $outputDirectory")
            }
        }
    }

    @Throws(ExtractionException::class)
    private fun checkArchiveFile() {
        if (!File(archive).exists()) {
            throw ExtractionException("Archive file not found: $archive")
        }
        if (!File(archive).canRead()) {
            println("Can't read archive file: $archive")
        }
    }

    @Throws(ExtractionException::class)
    fun extractArchive() {
        val randomAccessFile: RandomAccessFile
        var ok = false
        try {
            randomAccessFile = RandomAccessFile(archive, "r")
        } catch (e: FileNotFoundException) {
            throw ExtractionException("File not found", e)
        }

        try {
            extractArchive(randomAccessFile)
            ok = true
        } finally {
            try {
                randomAccessFile.close()
            } catch (e: Exception) {
                if (ok) {
                    throw ExtractionException(
                        "Error closing archive file",
                        e
                    )
                }
            }

        }
    }

    @Throws(ExtractionException::class)
    private fun extractArchive(file: RandomAccessFile) {
        val inArchive: IInArchive
        var ok = false
        try {
            inArchive = SevenZip.openInArchive(
                null,
                RandomAccessFileInStream(file)
            )
        } catch (e: SevenZipException) {
            throw ExtractionException("Error opening archive", e)
        }

        try {

            var ids: IntArray? = null
            if (filterRegex != null) {
                ids = filterIds(inArchive, filterRegex)
            }
            inArchive.extract(ids, test, ExtractCallback(inArchive))
            ok = true
        } catch (e: SevenZipException) {
            val stringBuilder = StringBuilder()
            stringBuilder.append("Error extracting archive '")
            stringBuilder.append(archive)
            stringBuilder.append("': ")
            stringBuilder.append(e.message)
            if (e.cause != null) {
                stringBuilder.append(" (")
                stringBuilder.append(e.cause.toString())
                stringBuilder.append(')')
            }
            val message = stringBuilder.toString()

            throw ExtractionException(message, e)
        } finally {
            try {
                inArchive.close()
            } catch (e: SevenZipException) {
                if (ok) {
                    throw ExtractionException("Error closing archive", e)
                }
            }

        }
    }

    private fun filterToRegex(filter: String?): String? {
        return if (filter == null) {
            null
        } else "\\Q" + filter.replace("*", "\\E.*\\Q") + "\\E"
    }

    @Throws(SevenZipException::class)
    private fun filterIds(inArchive: IInArchive, regex: String): IntArray {
        val idList = ArrayList<Int>()

        val numberOfItems = inArchive.getNumberOfItems()

        val pattern = Pattern.compile(regex)
        for (i in 0 until numberOfItems) {
            val path = inArchive.getProperty(i, PropID.PATH) as String
            val fileName = File(path).name
            if (pattern.matcher(fileName).matches()) {
                idList.add(i)
            }
        }

        val result = IntArray(idList.size)
        for (i in result.indices) {
            result[i] = idList[i]
        }
        return result
    }
}


fun main(args: Array<String>) {
    /*
    val argList = ArrayList(Arrays.asList(*args))
    if (argList.size > 0 && argList[0] == "-t") {
        argList.removeAt(0)
        test = true
    }
    if (argList.size != 2 && argList.size != 3) {
        println("Usage:")
        println("java -cp ... example.ExtractExample [-t] <archive> <output-dir> [filter]")
        System.exit(1)
    }
    if (argList.size == 3) {
        filter = argList[2]
    }
    */
    var test = false
    var filter: String? = null
    val theArchivePaths = arrayOf(
        "R:\\TestArchives\\WhereIs.rar"
        , "R:\\TestArchives\\MultiVolume.part1.rar"
        , "R:\\TestArchives\\SingleVolume.rar"
        , "R:\\TestArchives\\SingleVolume.zip"
    )
    var inputList = mutableListOf<Pair<String,String>>()
    theArchivePaths.forEachIndexed { index, aPath ->
        inputList.add(Pair(aPath,"R:\\TestArchives\\Output\\"+index.toString()))
    }

    for (aPair in inputList) {
        try {
            println(aPair.first)
            Extract04(aPair.first, aPair.second, test, filter).extract()
            println("Extraction successful")

        } catch (e: ExtractionException) {
            System.err.println("[ERROR]<Main>: " + e.localizedMessage)
            e.printStackTrace()
        }
    }
}
