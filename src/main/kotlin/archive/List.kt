package archive

import net.sf.sevenzipjbinding.IInArchive

import util.*


fun printItemList(inArchive: IInArchive) {
    val simpleInArchive = inArchive.simpleInterface
    val theSize = simpleInArchive.archiveItems.size

    println("Archive item size: $theSize")

    println(String.format("Archive Format: %s", inArchive.archiveFormat.toString()))

    println("  ID  |   CRC    |     Size     |   Compr.Sz.  |    Modified Date    | Filename")
    println("-----------------+--------------+--------------+---------------------+---------")

    for (item in simpleInArchive.archiveItems) {
        println(
            String.format(
                " %4d | %08X | %12s | %12s | %19s | %s",
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
    if ( !ids.isEmpty() ) {
        val simpleInArchive = inArchive.simpleInterface

        println("  ID  |   CRC    |     Size     |   Compr.Sz.  |    Modified Date    | Filename")
        println("-----------------+--------------+--------------+---------------------+---------")

        for (idx in ids) {
            val item = simpleInArchive.getArchiveItem(idx)
            println(
                String.format(
                    " %4d | %08X | %12s | %12s | %19s | %s",
                    item.itemIndex,
                    item.crc,
                    item.size,
                    item.packedSize,
                    item.lastWriteTime.time.dateFormatter(),
                    item.path
                )
            )
        }
    } else {
        println("No Item for listing")
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
