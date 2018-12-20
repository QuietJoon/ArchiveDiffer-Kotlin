import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem

import util.*


class Item (
      val dataCRC: Int
    , val dataSize: DataSize
    , val modifiedDate: Date
    , val path: JointPath
    , val parentID: ItemID
    , val idInArchive: ItemIndex
    , val parentArchiveSetID: ArchiveSetID
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


    fun makeItemRecordFromItem(archiveSetNum: Int, rootArchiveSetID: ArchiveSetID,theArchiveSetID: ArchiveSetID): ItemRecord {
        val existence = arrayOfNulls<ExistanceMark>(archiveSetNum)
        existence[rootArchiveSetID]=Pair(theArchiveSetID,id)
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
        if ( fullName.getExtension() == "exe" ) null // Make more logic
        else if ( fullName.isArchive() ) true
        else false

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
        hash = hash * hashPrime + parentArchiveSetID.hashCode()
        hash = hash * hashPrime + id.hashCode()
        return hash
    }
}

fun ISimpleInArchiveItem.makeItemFromArchiveItem(parentPath: JointPath, parentID: ItemID, parentArchiveSetID: ArchiveSetID): Item {

    val newPath = parentPath.plus(this.path)

    return Item (
          dataCRC = this.crc
        , dataSize = this.size
        , modifiedDate = this.lastWriteTime.time
        , path = newPath
        , parentID = parentID
        , idInArchive = this.itemIndex
        , parentArchiveSetID = parentArchiveSetID
    )
}

typealias ItemIndex = Int
typealias ItemID = Int
typealias Date = Long
typealias Name = String
typealias DataSize = Long
