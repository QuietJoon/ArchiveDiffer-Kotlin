package util

import java.io.File
import com.ibm.icu.lang.*

import ArchivePaths
import ArchiveSetPaths
import dateFormat
import directoryDelimiter
import JointPath
import Path


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

fun getFirstOrSingleArchivePaths(paths: Array<Path>) : Array<Path> {
    val firstOrSingle: MutableList<String> = mutableListOf()
    for ( aPath in paths ) {
        if ( aPath.isArchive() ) {
            // I knew this can be replaced by single if by using `maybePartNumber`
            // But I want to leave this structure for easy reading
            if (aPath.isSingleVolume()) {
                firstOrSingle.add(aPath)
            } else if (aPath.isFirstVolume()) {
                firstOrSingle.add(aPath)
            }
        }
    }
    return firstOrSingle.toTypedArray()
}

fun packageFilePathsWithoutGuide(paths: List<String>): Array<ArchiveSetPaths> {
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
    return resultList.toTypedArray()
}

fun packageFilePathsForGrouped(paths: List<String>): ArchiveSetPaths {
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
    return resultList.toTypedArray()
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

fun String.isArchive(): Boolean {
    val archiveExts: Array<String> = arrayOf("rar", "zip", "7z", "exe")
    for ( aExt in archiveExts ) {
        if ( this.getExtension() == aExt ) {
            return true
        }
    }
    return false
}

fun String.isArchiveSensitively(): Boolean? {
    val archiveExts: Array<String> = arrayOf("rar", "zip", "7z")
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
    val width = UCharacter.getIntPropertyValue(this.toInt(), UProperty.EAST_ASIAN_WIDTH)
    return when (width) {
        UCharacter.EastAsianWidth.NARROW -> 1
        UCharacter.EastAsianWidth.NEUTRAL -> 1
        UCharacter.EastAsianWidth.HALFWIDTH -> 1
        UCharacter.EastAsianWidth.FULLWIDTH -> 2
        UCharacter.EastAsianWidth.WIDE -> 2
        else -> 1
    }
}

fun Long.dateFormatter(): String {
    return dateFormat.format(java.util.Date(this))
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
