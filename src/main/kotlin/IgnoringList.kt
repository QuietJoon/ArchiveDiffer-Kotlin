import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem

import util.*


class IgnoringItem(
    val itemCRC: Leveled<Int>,
    val itemSize: Leveled<DataSize>,
    val itemName: Leveled<Name>,
    val itemModifiedDate: Leveled<Date>
) {
    fun match(item: Item): Boolean {
        var result = true
        var checked = false
        if (itemCRC.level == Level.SURE) {
            checked = true
            result = result && (itemCRC.datum == item.dataCRC)
        }
        if (itemSize.level == Level.SURE) {
            checked = true
            result = result && (itemSize.datum == item.dataSize)
        }
        if (itemName.level == Level.SURE) {
            checked = true
            result = result && (itemName.datum == item.path.last().getFullName())
        }
        if (itemModifiedDate.level == Level.SURE) {
            checked = true
            result = result && (itemModifiedDate.datum == item.modifiedDate)
        }
        return if (checked) result else false
    }
}

fun makeItemFromRawItem(item: ISimpleInArchiveItem): IgnoringItem {
    return IgnoringItem(
        itemCRC = Leveled(Level.NOTYET, item.crc),
        itemSize = Leveled(Level.NOTYET, item.size),
        itemName = Leveled(Level.NOTYET, item.path.getFullName()),
        itemModifiedDate = Leveled(Level.NOTYET, item.lastWriteTime.time)
    )
}

class IgnoringList(
    val ignoringList: List<IgnoringItem>
) {
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        for (item in ignoringList) {
            stringBuilder.append(item.itemCRC.level.toString())
            stringBuilder.append("|")
            stringBuilder.append(String.format("%08X", item.itemCRC.datum))
            stringBuilder.append("|")
            stringBuilder.append(item.itemSize.level.toString())
            stringBuilder.append("|")
            stringBuilder.append(String.format("%12s", item.itemSize.datum))
            stringBuilder.append("|")
            stringBuilder.append(item.itemModifiedDate.level.toString())
            stringBuilder.append("|")
            stringBuilder.append(item.itemModifiedDate.datum.toString())
            stringBuilder.append("|")
            stringBuilder.append(item.itemName.level.toString())
            stringBuilder.append("|")
            stringBuilder.append(item.itemName.datum)
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun match(item: Item): Boolean {
        for (ignoringItem in ignoringList) {
            if (ignoringItem.match(item)) {
                return true
            }
        }
        return false
    }
}

fun ignoringListFromString(content: List<String>): IgnoringList {
    val rawIgnoringList = mutableListOf<IgnoringItem>()
    for (line in content) {
        val tokens = line.split("|")

        val itemCRCL = Level.valueOf(tokens[0].trim())
        val itemCRCV = tokens[1].toLong(16).toInt()
        val itemSIZEL = Level.valueOf(tokens[2].trim())
        val itemSIZEV = tokens[3].trim().toLong()
        val itemModifiedDateL = Level.valueOf(tokens[4].trim())
        val itemModifiedDateV = tokens[5].toLong()
        val itemNameL = Level.valueOf(tokens[6].trim())
        val itemNameV = tokens[7]
        rawIgnoringList.add(
            IgnoringItem(
                Leveled(itemCRCL, itemCRCV),
                Leveled(itemSIZEL, itemSIZEV),
                Leveled(itemNameL, itemNameV),
                Leveled(itemModifiedDateL, itemModifiedDateV)
            )
        )
    }

    return IgnoringList(rawIgnoringList.toList())
}

fun readIgnoringList(inputPath: RealPath): IgnoringList {
    return ignoringListFromString(readFileAsLinesUsingBufferedReader(inputPath))
}

fun writeIgnoringList(ignoringList: IgnoringList, outputPath: RealPath) {
    val content = ignoringList.toString()
    writeFileUsingBufferedWriter(outputPath, content)
}
