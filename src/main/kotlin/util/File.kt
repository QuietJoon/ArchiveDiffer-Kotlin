package util

/*
Copy from https://github.com/eugenp/tutorials/tree/master/core-kotlin/src/main/kotlin/com/baeldung/filesystem
 */

import java.io.File

import ArchiveSetPaths
import MessageType
import directoryDelimiter


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
fun checkArchiveExistence(packagedFilePaths: Array<ArchiveSetPaths>): Pair<MessageType,String> {

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
