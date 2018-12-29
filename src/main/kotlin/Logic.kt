fun printStatus(theTable: TheTable) {
    for (anArchiveSet in theTable.theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for (anItemRecord in theTable.theItemTable) {
        print(anItemRecord.key.toString())
        println(anItemRecord.value.toString())
    }
}

fun printResult(theTable: TheTable) {

    println("Difference only")
    for (anItemEntry in theTable.theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            println(theString)
        }
    }

    println("Same")
    for (anItemEntry in theTable.theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            println(theString)
        }
    }
}

fun printFinalResult(theTable: TheTable): Pair<Int,MutableList<String>> {
    var count = 0
    val resultList = mutableListOf<String>()

    println("Difference only")
    for (anItemEntry in theTable.theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            count++
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.simpleString(theTable.theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            println(theString)
        }
    }
    println("Same")
    resultList.add("--------------------------------    Same    --------------------------------")
    for (anItemEntry in theTable.theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.simpleString(theTable.theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            println(theString)
        }
    }
    return Pair(count,resultList)
}
