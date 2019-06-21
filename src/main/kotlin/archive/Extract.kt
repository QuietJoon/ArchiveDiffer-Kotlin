// Originally comes from example code for SevenZip-JBindings

package archive

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList
import java.util.regex.Pattern

import net.sf.sevenzipjbinding.*

import RealPath


class Extract internal constructor(
    private val archive: RealPath,
    private val outputDirectory: RealPath,
    private val test: Boolean,
    filter: String?
) {
    private lateinit var outputDirectoryFile: File
    private val filterRegex: String?

    inner class ExtractCallback(private val inArchive: IInArchive) : IArchiveExtractCallback {
        private var index: Int = 0
        private var outputStream: OutputStream? = null
        private lateinit var file: File
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

            var path = inArchive.getProperty(index, PropID.PATH) as RealPath
            path = path.replace("\\s*\\\\".toRegex(), "\\\\").trim { it <= ' ' }
            file = File(outputDirectoryFile, path)

            if (isFolder) {
                createDirectory(file)
                return null
            }

            createDirectory(file.parentFile)

            try {
                outputStream = FileOutputStream(file)
            } catch (e: FileNotFoundException) {
                throw SevenZipException("Error opening file: " + file.absolutePath, e)
            }

            return ISequentialOutStream { data ->
                try {
                    outputStream!!.write(data)
                } catch (e: IOException) {
                    throw SevenZipException("Error writing to file: " + file.absolutePath)
                }

                data.size
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
                    throw SevenZipException("Error closing file: " + file.absolutePath)
                }

            }
        }

        @Throws(SevenZipException::class)
        override fun prepareOperation(extractAskMode: ExtractAskMode) {}

        @Throws(SevenZipException::class)
        override fun setOperationResult(
            extractOperationResult: ExtractOperationResult
        ) {
            closeOutputStream()
            val path = inArchive.getProperty(index, PropID.PATH) as RealPath
            if (extractOperationResult != ExtractOperationResult.OK) {
                throw SevenZipException("Invalid file: $path")
            }

            if (!isFolder) {
                when (extractAskMode) {
                    ExtractAskMode.EXTRACT -> print("Extracted $path\n")
                    ExtractAskMode.TEST -> print("Tested $path\n")
                    else -> print("Unknown mode $path\n")
                }
            }
        }

    }

    init {
        this.filterRegex = filterToRegex(filter)
    }

    @Throws(ExtractionException::class)
    internal fun extractEverything() {
        val anAns = openArchive(archive)
        prepareOutputDirectory()
        if (anAns == null) error("[ERROR]<extractEverything>: ----")
        extractEverything(anAns.inArchive)
        anAns.close()
    }

    @Throws(ExtractionException::class)
    fun prepareOutputDirectory() {
        outputDirectoryFile = File(outputDirectory)
        if (!outputDirectoryFile.exists()) {
            outputDirectoryFile.mkdirs()
        } else {
            /*
            if (outputDirectoryFile!!.list()!!.isNotEmpty()) {
                throw ExtractionException("Output directory not empty: $outputDirectory")
            }
            */
        }
    }

    @Throws(ExtractionException::class)
    private fun extractEverything(inArchive: IInArchive) {
        try {
            var ids: IntArray? = null
            if (filterRegex != null) {
                ids = filterIds(inArchive, filterRegex)
            }
            inArchive.extract(ids, test, ExtractCallback(inArchive))
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
        }
    }

    @Throws(ExtractionException::class)
    fun extractSomething(inArchive: IInArchive, ids: IntArray) {
        prepareOutputDirectory()
        try {
            inArchive.extract(ids, test, ExtractCallback(inArchive))
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
            val path = inArchive.getProperty(i, PropID.PATH) as RealPath
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
