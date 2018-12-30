import java.util.*
import java.io.File

import archive.*
import util.*


class TheTable constructor (archiveSets: Array<ArchiveSet>, defaultOutputDirectory: RealPath) {
    val theItemTable: ItemRecordTable = sortedMapOf()
    val theItemList: ItemList = mutableMapOf()
    val theArchiveSets: Array<ArchiveSet>
    val theArchiveMap: ArchiveMap = mutableMapOf()
    val archiveSetNum: Int
    val tableInstance: Int
    val rootOutputDirectory: RealPath

    companion object {
        var tableInstanceSerial = 1
    }

    init {
        tableInstance = tableInstanceSerial
        tableInstanceSerial++

        theArchiveSets = archiveSets
        archiveSetNum = archiveSets.size

        for (anArchiveSet in theArchiveSets) {
            for (anArchive in anArchiveSet.archiveMap)
                theArchiveMap[anArchive.key] = anArchive.value
                registerArchiveSetItemsRecord(anArchiveSet)
        }

        rootOutputDirectory = defaultOutputDirectory + directoryDelimiter + tableInstanceSerial
    }

    private fun registerArchiveSetItemsRecord(anArchiveSet: ArchiveSet) {
        for (anItem in anArchiveSet.itemMap) {
            val theItem: Item = anArchiveSet.itemMap[anItem.key] ?: error("[Error]<registerAnItemRecord>: No such item by ${anItem.key}")

            val theParentArchiveID = theItem.parentArchiveID
            val theArchiveSetID = anArchiveSet.archiveSetID
            var theKey = anItem.key
            val queryResult = queryInsensitive(theKey)
            theKey = queryResult.first
            val queryItemRecord: ItemRecord? = queryResult.second
            if (queryItemRecord == null) {
                val anItemRecord = theItem.makeItemRecordFromItem(archiveSetNum, theParentArchiveID, theArchiveSetID)
                theItemTable[theKey] = anItemRecord
            } else if (queryItemRecord.existence[theArchiveSetID] == null) {
                val newExistance = queryItemRecord.existence
                newExistance[theArchiveSetID] = Pair(theParentArchiveID, theItem.id)
                theItemTable[theKey]!!.existence = newExistance
            } else {
                println("[WARN]<registerAnItemRecord>: add again ${theItem.path.last()}")
            }

            if (theItemTable[theKey]!!.existence.isFilled())
                theItemTable[theKey]!!.isFilled = true

            theItemList[theItem.id] = theItem
        }
    }

    private fun registerAnItemRecordWithExistance(anArchiveSet: ArchiveSet, key: ItemKey, archiveSetIDs: MutableList<Int>) {
        val theItem: Item = anArchiveSet.itemMap[key] ?: error("[Error]<registerAnItemRecordWithExistance>: No such item by $key")
        if (theIgnoringList.match(theItem)) {
            println("Skip: ${theItem.path.last()}")
            return
        }

        val theParentArchiveID = theItem.parentArchiveID
        val theArchiveSetID = anArchiveSet.archiveSetID
        var theKey = key
        val queryResult = queryInsensitive(theKey)
        theKey = queryResult.first
        val queryItemRecord: ItemRecord? = queryResult.second
        if (queryItemRecord == null) {
            val anItemRecord = theItem.makeItemRecordFromItem(archiveSetNum, theParentArchiveID, theArchiveSetID)
            theItemTable[theKey] = anItemRecord
        }

        archiveSetIDs.forEach {
            theItemTable[theKey]!!.existence[it] =
                    Pair(theParentArchiveID, theItem.id)
        }

        if (theItemTable[theKey]!!.existence.isFilled())
            theItemTable[theKey]!!.isFilled = true

        theItemList[theItem.id] = theItem
    }

    private fun queryInsensitive(aKey: ItemKey): Pair<ItemKey,ItemRecord?> {
        val aRecord = theItemTable[aKey]
        if (aRecord != null) return Pair(aKey,aRecord)

        var newKey: ItemKey
        var newRecord: ItemRecord?
        if (aKey.isArchive == null) {
            newKey = aKey.copy(isArchive = true)
            newRecord = theItemTable[newKey]
            if (newRecord == null) {
                newKey = aKey.copy(isArchive = false)
                newRecord = theItemTable[newKey]
            }
        }
        else {
            newKey = aKey.copy(isArchive = null)
            newRecord = theItemTable[newKey]
        }
        return if (newRecord == null) Pair(aKey, aRecord)
                else Pair(newKey, newRecord)
    }

    private fun ExistanceBoard.isFilled(): Boolean {
        this.forEach{ if(it==null) return false }
        return true
    }

    private fun getFirstItemKey(): ItemKey? {
        theItemTable.forEach {
            if (!it.value.isFilled)
                if (it.key.isArchive != false)
                    if (theItemTable[it.key]!!.isFirstOrSingle
                        && !theItemTable[it.key]!!.isExtracted) return it.key
        }
        return null
    }

    private fun modifyKeyOfTheItemTable(oldKey: ItemKey, newKey: ItemKey) {
        val queriedValue = theItemTable[oldKey]

        if (queriedValue == null) {
            error("[ERROR]<modifyKey>: No such ItemRecord with $oldKey")
        } else {
            theItemTable.remove(oldKey)
            theItemTable.put(newKey,queriedValue)
        }
    }

    fun printSameItemTable(len: Int, fullNameOnly: Boolean, relativePathOnly: Boolean) {
        for ( itemEntry in theItemTable ) {
            if (itemEntry.value.isFilled) {
                println(itemEntry.key)
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
                        print(regulatedPath + " | ")
                    }
                }
                println()
            }
        }
    }

    // TODO: May files having commonName appears in physically different archives
    private fun findMultiVolumes(path: RelativePath, archiveSetID: ArchiveID): List<ItemID> {
        val commonName = path.getCommonNameOfMultiVolume()
        val idList = mutableListOf<Int>()
        for ( itemEntry in theItemTable ) {
            val existence = itemEntry.value.existence[archiveSetID]
            if (existence != null)
                if (!(itemEntry.value.isFirstOrSingle || itemEntry.key.isArchive == false))
                    if (theItemList[existence.second]!!.path.last().getCommonNameOfMultiVolume() == commonName)
                        idList.add(existence.second)
        }
        return idList
    }

    fun runOnce(): Boolean {
        var theKey = getFirstItemKey()
        if (theKey != null) {
            val theItemRecord = theItemTable[theKey] ?: error("[Error]<runOnce>: No such item by $theKey")
            val idx = theItemRecord.getAnyID()
            val theParentArchive: Archive = theArchiveMap[idx.first] ?: error("[Error]<runOnce>: No such Archive ${idx.first}")
            val anArchivePath = theItemList[idx.second]!!.path.last()
            val idxs: List<ItemID> = findMultiVolumes(anArchivePath,theParentArchive.archiveSetID)
            val anArchiveRealPath = rootOutputDirectory + directoryDelimiter + anArchivePath
            val idsList: List<ItemID> = idxs.plus(idx.second)
            val iidsList: IntArray = idsList.map{theItemList[it]!!.idInArchive}.sorted().toIntArray()
            // Not sure when the first archive is exe file
            val paths: List<Path> = idsList.map { rootOutputDirectory + directoryDelimiter + theItemList[it]!!.path.last() }.sorted()
            val jointPaths: List<JointPath> = paths.map {theParentArchive.realArchivePaths[0].plus(it)}

            Extract( theParentArchive.realArchivePaths.last().last(), rootOutputDirectory, false, null)
                .extractSomething(theParentArchive.ans.inArchive, iidsList)

            val anANS = openArchive(anArchiveRealPath)
            if (anANS != null) {
                if (theItemRecord.isArchive == null) {
                    val newKey = theKey.copy(isArchive = true)
                    theItemTable[theKey]!!.isArchive = true
                    modifyKeyOfTheItemTable(theKey,newKey)
                    theKey = newKey
                }

                theItemTable[theKey]!!.isExtracted = true
                for (id in idxs) {
                    val anKey = theItemList[id]!!.generateItemKey()
                    theItemTable[anKey]!!.isExtracted = true
                }

                val anArchive = Archive(jointPaths.toTypedArray(),anANS, idx.second, theParentArchive.archiveSetID)
                theArchiveMap[anArchive.archiveID] = anArchive
                theArchiveSets[anArchive.archiveSetID].addNewArchive(anArchive)

                val aExistance = mutableListOf<Int>()
                theItemRecord.existence.forEachIndexed{ eIdx, eV -> if (eV != null) aExistance.add(eIdx) }

                for ( anIdx in anArchive.itemMap.keys)
                    registerAnItemRecordWithExistance(theArchiveSets[anArchive.archiveSetID],anIdx,aExistance)

            } else {
                if (theItemRecord.isArchive == null) {
                    theItemTable[theKey]!!.isArchive = false
                    modifyKeyOfTheItemTable(theKey,theKey.copy(isArchive = false))
                } else {
                    error("[ERROR]<runOnce>: Fail to open an Archive ${theItemRecord.path}")
                }
            }
            return false
        }
        else return true
    }

    fun closeAllArchiveSets() {
        theArchiveMap.forEach{ it.value.ans.close() }
    }

    fun removeAllArchiveSets() {
        File(rootOutputDirectory).deleteRecursively()
    }

    fun prepareWorkingDirectory() {
        val theDirectory = File(rootOutputDirectory)
        if ( !theDirectory.exists() ) {
            println("<prepareWorkingDirectory>: Does not exist")

            File(rootOutputDirectory).mkdirs()
            if ( !theDirectory.mkdirs() ) {
                println("[ERROR]<prepareWorkingDirectory>: Fail to make directory")
            } else {
                println("<prepareWorkingDirectory>: Seems to be made")
            }
            if (!theDirectory.exists()) {
                println("[ERROR]<prepareWorkingDirectory>: Can't be")
            }
        }
    }
}

data class ItemKey (
      val isArchive: Boolean?
    , val dataCRC: Int
    , val dataSize: DataSize
    , val dupCount: Int
) : Comparable<ItemKey> {
    companion object {
        val comparatorKey =
            compareByDescending(ItemKey::dataSize)
                .thenBy(ItemKey::dataCRC)
                .thenBy(ItemKey::dupCount)
    }
    override fun compareTo(other: ItemKey): Int =
        if (this.isArchive == other.isArchive) {
            comparatorKey.compare(this,other)
        }
        else when (this.isArchive) {
            true -> -1
            false -> 1
            null -> if (other.isArchive!!) 1 else -1
        }
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(if (isArchive==null) "? " else if (isArchive) "A " else "F ")
        stringBuilder.append(String.format("%08X", this.dataCRC))
        stringBuilder.append("  ")
        stringBuilder.append(String.format("%10d", this.dataSize))
        stringBuilder.append("  ")
        stringBuilder.append(String.format("%2d", this.dupCount))
        stringBuilder.append("  ")
        return stringBuilder.toString()
    }
}

data class ItemRecord (
      val dataCRC: Int
    , val dataSize: DataSize
    , val modifiedDate: Date
    , val path: RelativePath
    , var existence: ExistanceBoard
    , var isFilled: Boolean
    , var isArchive: Boolean? // null when exe is not sure
    , var isExtracted: Boolean
    , var isFirstOrSingle: Boolean
) {
    fun getFullName() = path.getFullName()

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(if (isFilled) "O " else "X ")
        stringBuilder.append(if (isArchive == true) (if (isExtracted) "E " else "N ") else "- ")
        stringBuilder.append(if (isArchive==null) "? " else if (isArchive!!) "A " else "F ")
        stringBuilder.append(if (isArchive==false) "  " else if (isFirstOrSingle) "S " else "M ")
        existence.forEachIndexed { i, em ->
            stringBuilder.append(if (em == null) "    $i     " else String.format(" %3d-%-5d", em.first, em.second))
        }
        stringBuilder.append(" | ")
        stringBuilder.append(String.format("%08X", this.dataCRC))
        stringBuilder.append("  ")
        stringBuilder.append(String.format("%10d", this.dataSize))
        stringBuilder.append("  ")
        stringBuilder.append(this.modifiedDate.dateFormatter())
        stringBuilder.append("  ")
        stringBuilder.append(path)
        return stringBuilder.toString()
    }

    fun simpleString(theItemList: ItemList): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(if (isFilled) "O " else "X ")
        stringBuilder.append(if (isArchive == true) (if (isExtracted) "E " else "N ") else "- ")
        stringBuilder.append(if (isArchive==null) "? " else if (isArchive!!) "A " else "F ")
        stringBuilder.append(if (isArchive==false) "  " else if (isFirstOrSingle) "S " else "M ")
        existence.forEachIndexed { i, em ->
            stringBuilder.append(if (em == null) "    $i     " else String.format(" %3d-%-5d", em.first, em.second))
        }
        for(i in existence) {
            if (i != null) {
                stringBuilder.append(" || ")
                stringBuilder.append(theItemList[i.second]!!.path.last().getFullName())
            }
        }
        return stringBuilder.toString()
    }

    fun managedString(theItemList: ItemList): String {
        val dirLen = 16
        val nameLen = 64
        val stringBuilder = StringBuilder()
        stringBuilder.append(String.format("%08X", this.dataCRC))
        stringBuilder.append("  ")
        stringBuilder.append(String.format("%10d", this.dataSize))
        stringBuilder.append("  ")
        stringBuilder.append(if (isFilled) "O " else "X ")
        stringBuilder.append(if (isArchive == true) (if (isExtracted) "E " else "N ") else "- ")
        stringBuilder.append(if (isArchive==null) "? " else if (isArchive!!) "A " else "F ")
        stringBuilder.append(if (isArchive==false) "  " else if (isFirstOrSingle) "S " else "M ")

        existence.forEachIndexed { i, em ->
            stringBuilder.append(if (em == null) " - " else " $i ")
        }
        val pathList = mutableListOf<String>()
        for(i in existence) {
            if (i == null)
                pathList.add("")
            else
                pathList.add(theItemList[i.second]!!.path.last())
        }
        val result = pathList.getSame()
        if (result.first) {
            stringBuilder.append(" <> ")
            stringBuilder.append(result.second[0].second.regulating(nameLen*2))
            stringBuilder.append(" <> ")
            result.second.forEach { pairStr ->
                stringBuilder.append(pairStr.first.regulating(dirLen))
                stringBuilder.append(" || ")
            }
        } else {
            result.second.forEach { pairStr ->
                stringBuilder.append(" || ")
                stringBuilder.append(pairStr.first.regulatingFromEnd(dirLen))
                stringBuilder.append(directoryDelimiter)
                stringBuilder.append(pairStr.second.regulating(nameLen))
            }
        }
        return stringBuilder.toString()
    }

    fun getAnyID(): ExistanceMark {
        existence.forEach {
            if (it != null) return it
        }
        error("[ERROR]<getAnyItemID>: Can't be reached - No items in existence")
    }
}

typealias ItemRecordTable = SortedMap<ItemKey, ItemRecord>
typealias ItemList = MutableMap<ItemID,Item>
typealias ArchiveMap = MutableMap<Int,Archive>
typealias ExistanceMark = Pair<ArchiveID,ItemID>
typealias ExistanceBoard = Array<ExistanceMark?>
