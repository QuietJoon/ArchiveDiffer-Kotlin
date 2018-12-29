import archive.openArchive
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem


class ArchiveSet constructor (archiveSetID: ArchiveSetID, realArchivePathsArray: ArchiveSetPaths) {
    val archiveMap: MutableMap<ArchiveID,Archive>
    val itemMap: ItemMap
    val archiveSetID: ArchiveSetID

    init {
        this.archiveSetID = archiveSetID
        itemMap = mutableMapOf()
        archiveMap = mutableMapOf()

        for ( realPaths in realArchivePathsArray) {
            val anAns = openArchive(realPaths[0].last()) ?: error("[ERROR]<init<ArchiveSet>>: Couldn't open ${realPaths[0].last()}")
            val anArchive = Archive(realPaths, anAns, 0, archiveSetID)
            archiveMap[anArchive.archiveID] = anArchive

            val simpleArchive = anAns.inArchive.simpleInterface
            for (sItem in simpleArchive.archiveItems) {
                if (!sItem.isFolder) {
                    addNewItem(anArchive.realArchivePaths[0], anArchive.itemID, anArchive.archiveID, sItem)
                }
            }
        }
    }

    fun addNewItem(parentPath: JointPath, itemID: ItemID, archiveID: ArchiveID, sItem: ISimpleInArchiveItem) {
        val anItem = sItem.makeItemFromArchiveItem(parentPath, itemID, archiveID, archiveSetID)
        if (theIgnoringList.match(anItem)) {
            println("Skip: ${anItem.path.last()}")
            return
        }
        var aKey = anItem.generateItemKey()
        while (true) {
            val queryItem = itemMap[aKey]
            if (queryItem == null) {
                itemMap[aKey] = anItem
                break
            // This condition never satisfied
            } else if (queryItem.equalsWithoutRealPath(anItem)) {
                println("[ERROR]<ArchiveSet.addNewItem>: Skip because completely same item: ${anItem.path.last()}")
                itemMap[aKey] = anItem
                break
            } else {
                aKey = aKey.copy(dupCount = aKey.dupCount + 1)
            }
        }
    }

    fun addNewArchive(anArchive: Archive) {
        archiveMap[anArchive.archiveID] = anArchive

        val simpleArchive = anArchive.ans.inArchive.simpleInterface
        for (sItem in simpleArchive.archiveItems) {
            if (!sItem.isFolder) {
                addNewItem(anArchive.realArchivePaths[0], anArchive.itemID, anArchive.archiveID, sItem)
            }
        }
    }

    fun getInArchive(archiveID: ArchiveID): IInArchive {
        val anArchive: Archive = archiveMap[archiveID] ?: error("[ERROR]<getInArchive>: No such archive $archiveID")
        return anArchive.ans.inArchive
    }

    fun getThisIDs(): Array<ItemIndices> {
        val aList = mutableListOf<ItemIndices>()
        for ( itemPair in itemMap ) {
            val item = itemPair.value
            aList.add(Triple(item.parentArchiveID,item.idInArchive,archiveSetID))
        }
        return aList.toTypedArray()
    }
}

typealias ArchiveSetPaths = Array<ArchivePaths>
typealias ArchivePaths = Array<JointPath>
typealias ArchiveSetID = Int
typealias ItemIndices = Triple<ArchiveID,ItemIndex,ArchiveSetID>
typealias ItemMap = MutableMap<ItemKey,Item>
