fun TheTable.printStatus() {
    for (anArchiveSet in theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for (anItemRecord in theItemTable) {
        print(anItemRecord.key.toString())
        print(anItemRecord.value.toString().plus("\n"))
    }
}

fun TheTable.printResult() {

    print("Difference only\n")
    for (anItemEntry in theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            print(theString.plus("\n"))
        }
    }

    print("Same\n")
    for (anItemEntry in theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.key.toString())
            stringBuilder.append(anItemEntry.value.toString())
            val theString = stringBuilder.toString()
            print(theString.plus("\n"))
        }
    }
}

fun TheTable.printFinalResult(): Pair<Int,MutableList<String>> {
    var count = 0
    val resultList = mutableListOf<String>()

    print("Difference only\n")
    for (anItemEntry in theItemTable) {
        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
            count++
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.value.managedString(theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            print(theString.plus("\n"))
        }
    }
    print("Same\n")
    resultList.add("--------------------------------    Same    --------------------------------")
    for (anItemEntry in theItemTable) {
        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(anItemEntry.value.managedString(theItemList))
            val theString = stringBuilder.toString()
            resultList.add(theString)
            print(theString.plus("\n"))
        }
    }
    return Pair(count,resultList)
}
