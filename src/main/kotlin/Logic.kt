fun TheTable.printStatus() {
    for (anArchiveSet in theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for (anItemRecord in theItemTable) {
        print(anItemRecord.key.toString())
        println(anItemRecord.value.toString())
    }
}

fun TheTable.printResult() {

    println("Difference only")
    for (anItemEntry in theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            println(theString)
        }
    }

    println("Same")
    for (anItemEntry in theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            println(theString)
        }
    }
}

fun TheTable.printFinalResult(): Pair<Int,MutableList<String>> {
    var count = 0
    val resultList = mutableListOf<String>()

    println("Difference only")
    for (anItemEntry in theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            count++
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.simpleString(theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            println(theString)
        }
    }
    println("Same")
    resultList.add("--------------------------------    Same    --------------------------------")
    for (anItemEntry in theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.simpleString(theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            println(theString)
        }
    }
    return Pair(count,resultList)
}
