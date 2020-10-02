import archive.printItemListByIDs
import net.sf.sevenzipjbinding.IInArchive
import util.dateFormatter
import util.getFullName
import util.regulating


fun IgnoringList.printIgnoringList() {

    print("   CRC    |    Size    |     Modified Date   | Filename\n")
    print("----------+------------+---------------------+---------\n")

    for (item in ignoringList) {
        print(
            String.format(
                " %08X | %10s | %19s | %s\n",
                item.itemCRC.datum,
                item.itemSize.datum,
                item.itemModifiedDate.datum.dateFormatter(),
                item.itemName.datum
            )
        )
    }
}

fun IgnoringList.printIgnoringListWithLevel() {

    print("     CRC     |      Size     |       Modified Date    |  Filename\n")
    print("-------------+---------------+------------------------+----------\n")

    for (item in ignoringList) {
        print(
            String.format(
                " %s %08X | %s %10s | %s %19s | %s %s\n",
                item.itemCRC.level.toShortString(),
                item.itemCRC.datum,
                item.itemSize.level.toShortString(),
                item.itemSize.datum,
                item.itemModifiedDate.level.toShortString(),
                item.itemModifiedDate.datum.dateFormatter(),
                item.itemName.level.toShortString(),
                item.itemName.datum
            )
        )
    }
}

fun getIDArrayWithoutIgnoringItem(inArchive: IInArchive, ignoringList: IgnoringList): IntArray {
    val simpleInArchive = inArchive.simpleInterface
    val idList = mutableListOf<Int>()

    simpleInArchive.archiveItems.forEachIndexed { idx, sItem ->
        val item: Item = sItem.makeItemFromArchiveItem(emptyArray(),0,0, -1)
        if (!ignoringList.match(item)) {
            idList.add(idx)
        }
    }

    return idList.toIntArray()
}

fun printItemMapOfArchiveSet(archiveSet: ArchiveSet, itemIDs: Array<ItemIndices>) {
    itemIDs.sortWith(Comparator { a, b ->
        if (a.first == b.first)
            a.second - b.second
        else a.first - b.first
    }
    )

    var lastArchiveID: ArchiveID? = null
    var inArchive: IInArchive? = null
    var itemIndexList: MutableList<Int> = mutableListOf()
    for (idPair in itemIDs) {
        val theArchiveID = idPair.first
        val theItemIndex = idPair.second
        if (lastArchiveID != theArchiveID) {
            if (inArchive != null)
                printItemListByIDs(inArchive, itemIndexList.toIntArray())
            lastArchiveID = theArchiveID
            inArchive = archiveSet.getInArchive(theArchiveID)
            itemIndexList = mutableListOf()
        }
        if (inArchive == null)
            error("[ERROR]<printItemMapOfArchiveSet>: inArchive($theArchiveID) found")
        itemIndexList.add(theItemIndex)
    }
    if (inArchive != null)
        printItemListByIDs(inArchive, itemIndexList.toIntArray())
}

fun TheTable.printStatus() {
    for (anArchiveSet in theArchiveSets)
        printItemMapOfArchiveSet(anArchiveSet, anArchiveSet.getThisIDs())

    for (anItemRecord in theItemTable) {
        print(anItemRecord.key.toString())
        print(anItemRecord.value.toString().plus("\n"))
    }
}


fun TheTable.printSameItemTable(len: Int, fullNameOnly: Boolean, relativePathOnly: Boolean) {
    for ( itemEntry in theItemTable ) {
        if (itemEntry.value.isFilled) {
            print(itemEntry.key.toString().plus("\n"))
            itemEntry.value.existence.forEach {
                if (it == null) error("[Error]<printSameItemTable>: No item when `isFilled = true`")
                else {
                    val theItem = theItemList[it.second] ?: error("[Error]<printSameItemTable>: No queried item from the key in existence: $itemEntry.key")
                    val thePath = when {
                        fullNameOnly -> theItem.path.last().getFullName()
                        relativePathOnly -> theItem.path.last()
                        else -> theItem.path.joinToString(separator = "|")
                    }
                    val regulatedPath = thePath.regulating(len)
                    print("$regulatedPath | ")
                }
            }
            print("\n")
        }
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
