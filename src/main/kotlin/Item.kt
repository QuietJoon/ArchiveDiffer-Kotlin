import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem

import util.*


class Item (
      val dataCRC: Int
    , val dataSize: DataSize
    , val modifiedDate: Date
    , val path: JointPath
    , val parentID: ItemID // Real parentArchiveID
    , val idInArchive: ItemIndex
    , val parentArchiveID: ArchiveID // Virtual parentArchiveID (Not real, but same archive)
    , val archiveSetID: ArchiveSetID
) {
    val id: ItemID

    companion object {
        var serialCount = 1
    }

    init {
        id = serialCount
        serialCount += 1
    }

    fun getFullName() = path.last().getFullName()

    fun generateItemKey() = ItemKey(path.last().isArchiveSensitively(),dataCRC, dataSize, 1)
    fun generateItemKey(dupCount: Int) = ItemKey(path.last().isArchiveSensitively(),dataCRC, dataSize, dupCount)


    fun makeItemRecordFromItem(archiveSetNum: Int, theArchiveID: ArchiveID, archiveSetID: ArchiveSetID): ItemRecord {
        val existence = arrayOfNulls<ExistenceMark>(archiveSetNum)
        existence[archiveSetID]=Pair(theArchiveID,id)
        return ItemRecord(
            dataCRC = dataCRC
            , dataSize = dataSize
            , modifiedDate = modifiedDate
            , path = path.last()
            , existence = existence
            , isFilled = false
            , isArchive = getFullName().isArchiveSensitively()
            , isExtracted = false
            , isFirstOrSingle = getFullName().isSingleVolume() || getFullName().isFirstVolume()
        )
    }

    private fun checkArchiveName(fullName: String): Boolean? =
        when {
            fullName.getExtension() == "exe" -> null // Make more logic
            fullName.isArchive() -> true
            else -> false
        }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Item
        return dataCRC == that.dataCRC &&
                dataSize == that.dataSize &&
                modifiedDate == that.modifiedDate &&
                path == that.path
    }

    fun equalsWithoutRealPath(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Item
        return dataCRC == that.dataCRC &&
                dataSize == that.dataSize &&
                modifiedDate == that.modifiedDate &&
                path.last() == that.path.last()
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = hash * hashPrime + dataCRC.hashCode()
        hash = hash * hashPrime + dataSize.hashCode()
        hash = hash * hashPrime + modifiedDate.hashCode()
        hash = hash * hashPrime + path.hashCode()
        hash = hash * hashPrime + parentID.hashCode()
        hash = hash * hashPrime + idInArchive.hashCode()
        hash = hash * hashPrime + parentArchiveID.hashCode()
        hash = hash * hashPrime + id.hashCode()
        return hash
    }
}

fun ISimpleInArchiveItem.makeItemFromArchiveItem(parentPath: JointPath, parentID: ItemID, parentArchiveID: ArchiveID, archiveSetID: ArchiveSetID): Item {

    val newPath = parentPath.plus(this.path)

    return Item (
          dataCRC = this.crc
        , dataSize = this.size
        , modifiedDate = this.lastWriteTime.time
        , path = newPath
        , parentID = parentID
        , idInArchive = this.itemIndex
        , parentArchiveID = parentArchiveID
        , archiveSetID = archiveSetID
    )
}

typealias ItemIndex = Int
typealias ItemID = Int
typealias Date = Long
typealias Name = String
typealias DataSize = Long
