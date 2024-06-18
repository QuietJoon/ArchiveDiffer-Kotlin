import archive.ArchiveAndStream
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem


class Archive (
      val realArchivePaths: Array<JointPath>
    , val ans: ArchiveAndStream
    , val itemID: ItemID
    , val archiveSetID: ArchiveSetID
) {
    val archiveID: ArchiveID
    val itemMap: ItemMap

    companion object {
        var serialCount = 1
    }

    init {
        archiveID = serialCount
        serialCount++

        itemMap = mutableMapOf()

        val simpleArchive = ans.inArchive.simpleInterface
        for (sItem in simpleArchive.archiveItems) {
            if (!sItem.isFolder)
                addNewItem(sItem)
        }
    }

    fun addNewItem(sItem: ISimpleInArchiveItem) {
        val anItem = sItem.makeItemFromArchiveItem(
            realArchivePaths[0]
            , itemID
            , archiveID
            , archiveSetID
        )
        anItem.fixCRC(ans,realArchivePaths[0].toString())
        if (theIgnoringList.match(anItem)) {
            print("Skip: ${anItem.path.last()}\n")
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
                print("[ERROR]<Archive.addNewItem>: Skip because completely same item: ${anItem.path.last()}\n")
                itemMap[aKey] = anItem
                break
            } else {
                aKey = aKey.copy(dupCount = aKey.dupCount + 1)
            }
        }
    }

    fun getThisIDs(): Array<ItemIndices> {
        val aList = mutableListOf<ItemIndices>()
        for ( itemPair in itemMap ) {
            val item = itemPair.value
            aList.add(Triple(item.parentArchiveID,item.idInArchive,archiveSetID))
        }
        return aList.toTypedArray()
    }

    fun getInArchive(): IInArchive = ans.inArchive
}

typealias ArchiveID = Int

data class MultiArchiveVolumeInfo(
    var isMissing: Boolean,
    val commonPath: String,
    val lastVolumeNumber: Int,
    val existingVolumes: MutableList<String>,
    val missingVolumes: MutableList<String>,
    val corruptedVolumes: MutableList<String>
)
