package archive

import net.sf.sevenzipjbinding.IInArchive

import util.*


fun printItemList(inArchive: IInArchive) {
    val simpleInArchive = inArchive.simpleInterface
    val theSize = simpleInArchive.archiveItems.size

    print("Archive item size: $theSize\n")

    print(String.format("Archive Format: %s\n", inArchive.archiveFormat.toString()))

    print("  ID  |   CRC    |     Size     |   Compr.Sz.  |    Modified Date    | Filename\n")
    print("-----------------+--------------+--------------+---------------------+---------\n")

    for (item in simpleInArchive.archiveItems) {
        print(
            String.format(
                " %4d | %08X | %12s | %12s | %19s | %s\n",
                item.itemIndex,
                item.crc,
                item.size,
                item.packedSize,
                item.lastWriteTime.time.dateFormatter(),
                item.path
            )
        )
    }
}


fun printItemListByIDs(inArchive: IInArchive, ids: IntArray) {
    if ( ids.isNotEmpty() ) {
        val simpleInArchive = inArchive.simpleInterface

        print("  ID  |   CRC    |     Size     |   Compr.Sz.  |    Modified Date    | Filename\n")
        print("-----------------+--------------+--------------+---------------------+---------\n")

        for (idx in ids) {
            val item = simpleInArchive.getArchiveItem(idx)
            print(
                String.format(
                    " %4d | %08X | %12s | %12s | %19s | %s\n",
                    item.itemIndex,
                    item.crc,
                    item.size,
                    item.packedSize,
                    item.lastWriteTime?.time.dateFormatter(),
                    item.path
                )
            )
        }
    } else {
        print("No Item for listing\n")
    }
}

fun getNestedArchivesIDArray(inArchive: IInArchive): IntArray {
    val simpleInArchive = inArchive.simpleInterface
    val idList = mutableListOf<Int>()

    for (item in simpleInArchive.archiveItems) {
        if (item.path.toString().isArchive()) {
            idList.add(item.itemIndex)
        }
    }

    return idList.toIntArray()
}
