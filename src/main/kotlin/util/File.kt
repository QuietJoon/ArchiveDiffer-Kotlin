package util

/*
Copy from https://github.com/eugenp/tutorials/tree/master/core-kotlin/src/main/kotlin/com/baeldung/filesystem
 */

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.CRC32
import kotlin.system.exitProcess

import ArchiveSetPaths
import Message
import MessageType
import directoryDelimiter


//FIXME: Not sure do I need to declare variables{crc,in,buffer} as val or var
fun getCRC32Value(filename:String):Int {
    val crc = CRC32()
    try
    {
        val `in` = BufferedInputStream(FileInputStream(filename))
        val buffer = ByteArray(32768)
        var length = `in`.read(buffer)
        while (length >= 0) {
            crc.update(buffer, 0, length)
            length = `in`.read(buffer)
        }
        `in`.close()
    }
    catch (e: IOException) {
        System.err.println(e)
        exitProcess(2)
    }
    return crc.value.toInt()
}

fun readFileLineByLineUsingForEachLine(fileName: String) = File(fileName).forEachLine { println(it) }

fun readFileAsLinesUsingUseLines(fileName: String): List<String> = File(fileName)
    .useLines { it.toList() }

fun readFileAsLinesUsingBufferedReader(fileName: String): List<String> = File(fileName).bufferedReader().readLines()

fun readFileAsLinesUsingReadLines(fileName: String): List<String> = File(fileName).readLines()

fun readFileAsTextUsingInputStream(fileName: String) =
    File(fileName).inputStream().readBytes().toString(Charsets.UTF_8)

fun readFileDirectlyAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)


fun writeFileUsingPrintWriter(fileName: String, fileContent: String) =
    File(fileName).printWriter().use { out -> out.print(fileContent) }

fun writeFileUsingBufferedWriter(fileName: String, fileContent: String) =
    File(fileName).bufferedWriter().use { out -> out.write(fileContent) }

fun writeFileDirectly(fileName: String, fileContent: String) =
    File(fileName).writeText(fileContent)

fun writeFileDirectlyAsBytes(fileName: String, fileContent: String) =
    File(fileName).writeBytes(fileContent.toByteArray())


// TODO: Not yet implemented
fun checkArchiveExistence(packagedFilePaths: Array<ArchiveSetPaths>): Message {

    packagedFilePaths.forEach { archiveSetPaths ->
        archiveSetPaths.forEach { archivePaths ->
            for (aPath in archivePaths) {
                val thePath = aPath.joinToString(separator = directoryDelimiter)
                if (!File(thePath).exists())
                    return Pair(MessageType.Bad,"Can't access\n${aPath.last()}")
                else
                    println("Exist: $thePath")
            }
        }
    }
    return Pair(MessageType.NoProblem, "\n")
}
