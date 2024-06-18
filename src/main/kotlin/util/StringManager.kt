package util

import java.io.File
import com.ibm.icu.lang.*
import com.googlecode.concurrenttrees.solver.LCSubstringSolver
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory

import ArchivePaths
import ArchiveSetPaths
import JointPath
import Path
import dateFormat
import directoryDelimiter
import minimumLCSLength
import java.nio.file.Paths
import MultiArchiveVolumeInfo


// TODO: Mixed with CLI printing code
fun generatePackagedFilePaths (packagedFilePaths: Array<ArchiveSetPaths>): String {
    val sb = StringBuilder()
    packagedFilePaths.forEachIndexed { sIdx, archiveSetPaths ->
        print("ArchiveSet $sIdx\n")
        archiveSetPaths.forEachIndexed { aIdx, archivePaths ->
            print("\tArchive $aIdx\n")
            sb.append(String.format("%4s %4s  %s\n", sIdx, aIdx, archivePaths[0].last()))
            for (aPath in archivePaths) {
                print("\t\t" + aPath.last() + "\n")
            }
        }
    }
    return sb.toString()
}

fun generateStringFromFileList (strings : List<File>): String {
    val internalString = strings.joinToString(separator = "\n") { it.toString().getFullName() }
    return arrayOf("<\n", internalString, "\n>").joinToString(separator = "")
}

fun Path.isFirstOrSingleArchivePath() : Boolean {
    if (this.isArchive()) {
        if (this.isSingleVolume()) {
            return true
        } else if (this.isFirstVolume()) {
            return true
        }
    }
    return false
}

fun getFirstOrSingleArchivePaths(paths: Array<Path>) : Array<Path> {
    val firstOrSingle: MutableList<String> = mutableListOf()
    for ( aPath in paths ) {
        if ( aPath.isFirstOrSingleArchivePath() ) {
            firstOrSingle.add(aPath)
        }
    }
    return firstOrSingle.toTypedArray()
}

fun groupingFilePaths(paths : List<Path>) : Map <Int, List<Path>>{
    val groupByDrive = paths.groupBy { Paths.get(it).root.toString() }

    var drive_count = 0

    val groupedByDrive = mutableMapOf<Int,List<Path>>()

    groupByDrive.forEach { (drive, paths) ->
        println("Drive: $drive")
        val aList = mutableListOf<Path>()
        paths.forEach {
            println("\t${it}")
            aList.add(it)
        }
        groupedByDrive[drive_count] = aList
        drive_count++
    }
    return groupedByDrive
}

fun checkMissingMultiVolume(paths: List<Path>): List<MultiArchiveVolumeInfo> {
    val sorted = paths.sorted()
    /*
    for (path in sorted) {
        println("Path: $path")
    }
    */
    // Filter only multipart archives
    val multiVolumePaths = sorted.filter { !it.isSingleVolume() }
    // Grouping by common name before .part
    val grouped = multiVolumePaths.groupBy { it.getCommonNameOfMultiVolume() }
    // For each grouped archives, check if there is any missing volume
    val volumeInfos = mutableListOf<MultiArchiveVolumeInfo>()
    for (group in grouped) {
        //println("Group： ${group.key}")
        val existenceVolumes = mutableListOf<String>()
        val missingVolumes = mutableListOf<String>()
        val corruptedVolumes = mutableListOf<String>()
        var lastVolume = 1
        for (path in group.value) {
            //println("Path: $path")
            val volumeNumStr = path.getFileName().maybePartNumberStr() ?: "0"
            //println("Volume: $volumeNumStr")
            val volumeNum = volumeNumStr.toInt()
            for (c in lastVolume..volumeNum) {
                //println("Checking volume: $c")
                if (c == volumeNum) {
                    //println("Exist volume: $c")
                    lastVolume = volumeNum+1
                    existenceVolumes.add(path)
                    break
                } else {
                    println("Missing volume: $c")
                    val name = path.getDirectory() + "\\" + path.getFileName()
                    val missingVolumeNumStr = c.toString().padStart(volumeNumStr.length, '0')
                    val newExt = if (path.getExtension() == "exe") {
                        "rar"
                    } else path.getExtension()
                    val missingVolume = name.substring(0, name.length - volumeNumStr.length) + "$missingVolumeNumStr." + newExt
                    missingVolumes.add(missingVolume)
                }
            }
        }
        lastVolume -= 1
        volumeInfos.add(MultiArchiveVolumeInfo(missingVolumes.isNotEmpty(), group.key, lastVolume, existenceVolumes, missingVolumes, corruptedVolumes))
    }
    for (info in volumeInfos) {
        println("Common: ${info.commonPath}")
        println("Last: ${info.lastVolumeNumber}")
        println("Exist: ${info.existingVolumes}")
        println("Missing: ${info.missingVolumes}")
    }
    return volumeInfos
}

fun packageFilePathsWithoutGuide(paths: List<String>): Pair<Array<ArchiveSetPaths>, List<MultiArchiveVolumeInfo>> {
    val infos = checkMissingMultiVolumeFile(paths)
    val sorted = paths.sorted()
    val resultList = mutableListOf<ArchiveSetPaths>()
    var aList = mutableListOf<JointPath>()
    for ( path in sorted ) {
        if ( path.isSingleVolume() || path.isFirstVolume()) {
            if (aList.size != 0) resultList.add(arrayOf(aList.toTypedArray()))
            aList = mutableListOf()
            aList.add(arrayOf(path))
        } else {
            aList.add(arrayOf(path))
        }
    }
    resultList.add(arrayOf(aList.toTypedArray()))
    return resultList.toTypedArray() to infos
}

fun packageFilePathsForGrouped(paths: List<String>): Pair<ArchiveSetPaths, List<MultiArchiveVolumeInfo>> {
    val infos = checkMissingMultiVolumeFile(paths)
    val sorted = paths.sorted()
    val resultList = mutableListOf<ArchivePaths>()
    var aList = mutableListOf<JointPath>()
    for ( path in sorted ) {
        if ( path.isSingleVolume() || path.isFirstVolume()) {
            if (aList.size != 0) resultList.add(aList.toTypedArray())
            aList = mutableListOf()
            aList.add(arrayOf(path))
        } else {
            aList.add(arrayOf(path))
        }
    }
    resultList.add(aList.toTypedArray())
    return resultList.toTypedArray() to infos
}


fun filePathAnalyze(files: List<File>): Array<Path> {
    val pathArray = files.map{it.toString()}.toTypedArray()

    return getFirstOrSingleArchivePaths(pathArray)
}

fun String.getFullName(): String =
    this.substringAfterLast(directoryDelimiter)

fun String.getFileName(): String =
    this.substringAfterLast(directoryDelimiter).substringBeforeLast(".")

fun String.getExtension(): String =
    this.substringAfterLast(directoryDelimiter).substringAfterLast(".","")

fun String.getDirectory(): String =
    this.substringBeforeLast(directoryDelimiter,"")

fun String.dropMultiVolumeSuffix(): String {
    if (this.last().isDigit()) {
        if (this.dropLast(1).endsWith(".part")) {
            return this.dropLast(6)
        }
        else if (this.dropLast(2).endsWith(".part")) {
            return this.dropLast(7)
        }
    }
    return this
}
fun String.dropPrefixes(): String {
    var theStr = this
    if (theStr.startsWith("Maybe."))
        theStr = theStr.drop(6)
    if (theStr.startsWith("HaveDiff."))
        theStr = theStr.drop(9)
    if (theStr.startsWith("Maybe."))
        theStr = theStr.drop(6)
    return theStr
}

fun String.isArchive(): Boolean {
    val archiveExts: Array<String> = arrayOf("rar", "zip", "7z", "exe", "Rar", "Zip", "Exe", "RAR", "ZIP", "7Z", "EXE")
    for ( aExt in archiveExts ) {
        if ( this.getExtension() == aExt ) {
            return true
        }
    }
    return false
}

fun String.isArchiveSensitively(): Boolean? {
    val archiveExts: Array<String> = arrayOf("rar", "zip", "7z", "Rar", "Zip", "RAR", "ZIP", "7Z")
    for ( aExt in archiveExts ) {
        if ( this.getExtension() == aExt ) {
            return true
        }
    }
    if (this.getExtension() == "exe") return null
    return false
}

fun String.isEXE() = this.getExtension() == "exe"

/*
  * null -> SingleVolume
  * 1 -> First Volume
  * otherwise -> Not single nor first volume
 */
fun String.maybePartNumber(): Int? {
    val maybeNumberString = this.substringAfterLast(".part","")
    //println(String.format("<maybePartNumber>: %s",maybeNumberString))
    return maybeNumberString.toIntOrNull()
}

fun String.maybePartNumberStr(): String? {
    val maybeNumberString = this.substringAfterLast(".part","")
    //println(String.format("<maybePartNumber>: %s",maybeNumberString))
    if (maybeNumberString.toIntOrNull() == null) return null
    return maybeNumberString
}

fun String.isSingleVolume(): Boolean = getFileName().maybePartNumber() == null

fun String.isFirstVolume(): Boolean = getFileName().maybePartNumber() == 1

fun String.getCommonNameOfMultiVolume(): String = getFileName().substringBeforeLast(".part")

fun String.trimming(width: Int, suffix: String, suffixLength: Int): String {
    var result = ""
    var currWidth= 0
    for ( chr in this){
        val chrWidth = chr.getCharWidth()
        when {
            currWidth+chrWidth == width-suffixLength -> return result + chr + suffix.repeat(suffixLength)
            currWidth+chrWidth >  width-suffixLength -> return result + suffix.repeat(suffixLength+1)
        }
        result += chr
        currWidth += chrWidth
    }
    error("[ERROR]<trimming>: Can't be reached")
}

fun String.trimmingFromEnd(width: Int, suffix: String, suffixLength: Int): String {
    var result = ""
    var currWidth= 0
    var idx = this.length
    while (idx != 0) {
        idx--
        val chr = this[idx]
        val chrWidth = chr.getCharWidth()
        when {
            currWidth+chrWidth == width-suffixLength -> return suffix.repeat(suffixLength) + chr + result
            currWidth+chrWidth >  width-suffixLength -> return suffix.repeat(suffixLength+1) + result
        }
        result = chr + result
        currWidth += chrWidth
    }
    error("[ERROR]<trimmingFromEnd>: Can't be reached")
}

fun String.regulating(width: Int): String {
    val suffix="."
    val prefix=" "
    val thisWidth = this.getWidth()
    return when {
        thisWidth < width -> this+prefix.repeat(width-thisWidth)
        thisWidth > width -> this.trimming(width,suffix,2)
        else -> this
    }
}

fun String.regulatingFromEnd(width: Int): String {
    val suffix="."
    val prefix=" "
    val thisWidth = this.getWidth()
    return when {
        thisWidth < width -> prefix.repeat(width-thisWidth)+this
        thisWidth > width -> this.trimmingFromEnd(width,suffix,2)
        else -> this
    }
}

fun String.getWidth(): Int {
    var width = 0
    this.forEach {
        width += it.getCharWidth()
    }
    return width
}

fun Char.getCharWidth(): Int {
    val width = UCharacter.getIntPropertyValue(this.code, UProperty.EAST_ASIAN_WIDTH)
    return when (width) {
        UCharacter.EastAsianWidth.NARROW -> 1
        UCharacter.EastAsianWidth.NEUTRAL -> 1
        UCharacter.EastAsianWidth.HALFWIDTH -> 1
        UCharacter.EastAsianWidth.FULLWIDTH -> 2
        UCharacter.EastAsianWidth.WIDE -> 2
        else -> 1
    }
}

fun Long?.dateFormatter(): String {
    return if (this == null)
        "yyyy/MM/dd HH:mm:ss"
    else
        dateFormat.format(java.util.Date(this))
}

fun MutableList<String>.getSame(): Pair<Boolean,List<Pair<String,String>>> {
    val pairList = mutableListOf<Pair<String,String>>()
    for (str in this)
        // TODO: Want to do more efficiently, but I couldn't find something like `splitAt` or `breakOnEnd` in Haskell
        pairList.add(Pair(str.getDirectory(),str.getFullName()))
    val head = pairList[0].second
    for (strPair in pairList.drop(1)) {
        if (head != strPair.second) return Pair(false, pairList)
    }
    return Pair(true, pairList)
}

fun Array<ArchiveSetPaths>.getCommonFileName(): String {
    val firstPaths = mutableListOf<String>()
    for (path in this) {
        firstPaths.add(path[0][0][0].getFileName().dropMultiVolumeSuffix().dropPrefixes())
    }
    val theLCS = getLCS(firstPaths).trim()
    return if (theLCS.length >= minimumLCSLength) theLCS else ""
}

fun getLCS(strings: List<String>):String {
    val solver = LCSubstringSolver(DefaultCharSequenceNodeFactory())
    for (s in strings)
        solver.add(s)
    return solver.longestCommonSubstring.toString()
}
