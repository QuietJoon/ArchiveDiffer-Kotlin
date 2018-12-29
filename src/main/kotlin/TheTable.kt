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
            for (anItem in anArchiveSet.itemMap)
                registerAnItemRecord(anArchiveSet, anItem.key)
        }

        rootOutputDirectory = defaultOutputDirectory + directoryDelimiter + tableInstanceSerial
    }

    fun registerAnItemRecord(anArchiveSet: ArchiveSet, key: ItemKey) {
        val anItem: Item = anArchiveSet.itemMap[key] ?: error("[Error]<registerAnItemRecord>: No such item by $key")
        if (theIgnoringList.match(anItem)) {
            println("Skip: ${anItem.path.last()}")
            return
        }

        val idPair = Triple(anItem.parentArchiveID,anItem.id,anArchiveSet.archiveSetID)
        var aKey = anItem.generateItemKey()
        val queryResult = queryInsensitive(aKey)
        val queryItemRecord: ItemRecord? = queryResult.first
        aKey = queryResult.second
        if (queryItemRecord == null) {
            val anItemRecord = anItem.makeItemRecordFromItem(archiveSetNum, idPair.first, anArchiveSet.archiveSetID)
            theItemTable[aKey] = anItemRecord
        } else if (queryItemRecord.existence[idPair.third] == null) {
            val newExistance = queryItemRecord.existence
            newExistance[idPair.third] = Pair(idPair.first,idPair.second)
            theItemTable[aKey]!!.existence = newExistance
        } else {
            println("[WARN]<registerAnItemRecord>: add again ${anItem.path.last()}")
        }
        if (theItemTable[aKey]!!.existence.isFilled())
            theItemTable[aKey]!!.isFilled = true

        theItemList[anItem.id] = anItem
    }

    fun registerAnItemRecordWithExistance(anArchiveSet: ArchiveSet, key: ItemKey, archiveSetIDs: IntArray) {
        val anItem: Item = anArchiveSet.itemMap[key] ?: error("[Error]<registerAnItemRecordWithExistance>: No such item by $key")
        if (theIgnoringList.match(anItem)) {
            println("Skip: ${anItem.path.last()}")
            return
        }

        val idPair = Triple(anItem.parentArchiveID,anItem.id,anArchiveSet.archiveSetID)
        var aKey = anItem.generateItemKey()
        val queryResult = queryInsensitive(aKey)
        val queryItemRecord: ItemRecord? = queryResult.first
        aKey = queryResult.second
        if (queryItemRecord == null) {
            val anItemRecord = anItem.makeItemRecordFromItem(archiveSetNum, idPair.first, anArchiveSet.archiveSetID)
            theItemTable[aKey] = anItemRecord
        }

        archiveSetIDs.forEach {
            theItemTable[aKey]!!.existence[it] =
                    Pair(anItem.parentArchiveID, anItem.id)
        }

        if (theItemTable[aKey]!!.existence.isFilled())
            theItemTable[aKey]!!.isFilled = true

        theItemList[anItem.id] = anItem
    }

    fun queryInsensitive(aKey: ItemKey): Pair<ItemRecord?,ItemKey> {
        val aRecord = theItemTable[aKey]
        if (aRecord != null) return Pair(aRecord,aKey)

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
        return if (newRecord == null) Pair(aRecord,aKey)
                else Pair(newRecord,newKey)
    }

    fun mergeExistance(a:ExistanceBoard, b:ExistanceBoard): ExistanceBoard {
        val new = arrayOfNulls<ExistanceMark>(a.size)
        for (i in 0.until(a.size)) {
            if (a[i] != null) {
                new[i] = a[i]
            } else if (b[i] != null) {
                new[i] = b[i]
            }
        }
        return new
    }

    fun ExistanceBoard.isFilled(): Boolean {
        this.forEach{ if(it==null) return false }
        return true
    }

    fun getFirstItemKey(): ItemKey? {
        theItemTable.forEach {
            if (!it.value.isFilled)
                if (it.key.isArchive != false)
                    if (theItemTable[it.key]!!.isFirstOrSingle
                        && !theItemTable[it.key]!!.isExtracted) return it.key
        }
        return null
    }

    fun modifyKeyOfTheItemTable(oldKey: ItemKey, newKey: ItemKey) {
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
                        val thePath = if (fullNameOnly) theItem.path.last().getFullName()
                        else if (relativePathOnly) theItem.path.last()
                        else theItem.path.joinToString(separator = "|")
                        val regulatedPath = thePath.regulating(len)
                        print(regulatedPath + " | ")
                    }
                }
                println()
            }
        }
    }

    fun findMultiVolumes(path: RelativePath, archiveSetID: ArchiveID): IntArray {
        val commonName = path.getCommonNameOfMultiVolume()
        val idList = mutableListOf<Int>()
        for ( itemEntry in theItemTable ) {
            val existence = itemEntry.value.existence[archiveSetID]
            if (existence != null)
                if (!itemEntry.value.isFirstOrSingle)
                    if (theItemList[existence.second]!!.path.last().getCommonNameOfMultiVolume() == commonName)
                        idList.add(existence.second)
        }
        return idList.toIntArray()
    }

    fun runOnce(): Boolean {
        var theKey = getFirstItemKey()
        if (theKey != null) {
            var theItemRecord = theItemTable[theKey] ?: error("[Error]<runOnce>: No such item by $theKey")
            val idx = theItemRecord.getAnyID()
            val parentArchive: Archive = theArchiveMap[idx.first] ?: error("[Error]<runOnce>: No such Archive ${idx.first}")
            // Actually, this is not good enough. However, we assume that even the item is different, same key means same contents.
            val anArchivePath = theItemList[idx.second]!!.path.last()
            val idxs: IntArray = findMultiVolumes(anArchivePath,parentArchive.archiveSetID)
            val simpleArchive = parentArchive.ans.inArchive.simpleInterface


            val anArchiveRealPath = rootOutputDirectory + directoryDelimiter + anArchivePath

            val idsList = mutableListOf<Int>()

            for ( id in idxs.plus(idx.second))
                idsList.add(theItemList[id]!!.idInArchive)

            val paths: List<Path> = idsList.map { rootOutputDirectory + directoryDelimiter + simpleArchive.getArchiveItem(it).path }
            val jointPaths = paths.map {parentArchive.realArchivePaths[0].plus(it) }.toTypedArray()

            Extract( parentArchive.realArchivePaths.last().last(), rootOutputDirectory, false, null)
                .extractSomething(parentArchive.ans.inArchive, idsList.toIntArray())

            var anANS = openArchive(anArchiveRealPath)
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

                var anArchive = Archive(jointPaths,anANS, idx.second, parentArchive.archiveSetID)
                theArchiveMap[anArchive.archiveID] = anArchive
                theArchiveSets[anArchive.archiveSetID].archiveMap[anArchive.archiveID] = anArchive

                val anSimpleArchive = anANS.inArchive.simpleInterface
                for (sItem in anSimpleArchive.archiveItems) {
                    if (!sItem.isFolder) {
                        theArchiveSets[anArchive.archiveSetID].addNewItem(anArchive.realArchivePaths[0],anArchive.itemID, anArchive.archiveID, sItem)
                    }
                }

                val aExistance = mutableListOf<Int>()
                theItemRecord.existence.forEachIndexed{ eIdx, eV -> if (eV != null) aExistance.add(eIdx) }

                for ( anIdx in anArchive.itemMap.keys)
                    registerAnItemRecordWithExistance(theArchiveSets[anArchive.archiveSetID],anIdx,aExistance.toIntArray())

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

    fun run() {
        while (true) {
            if (runOnce()) break
        }
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
        for(i in existence)
            stringBuilder.append(if (i==null) "    -     " else String.format(" %3d-%-5d",i.first,i.second))
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
        for(i in existence)
            stringBuilder.append(if (i==null) "    -     " else String.format(" %3d-%-5d",i.first,i.second))
        for(i in existence) {
            if (i != null) {
                stringBuilder.append(" || ")
                stringBuilder.append(theItemList[i.second]!!.path.last().getFullName())
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
