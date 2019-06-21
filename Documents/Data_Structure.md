Data Structure
====

## Concept and Type Definitions

* Date - Long
* DataSize - Long
* Name - String
* Item
* ItemID - Int
* ItemIndex - Int
* ItemIndices - Triple<ArchiveID,ItemIndex,ArchiveSetID>
* ItemMap - MutableMap<ItemKey,Item>
* Archive
* ArchiveID - Int
* ArchiveSet
* ArchiveSetPaths - Array<ArchivePaths>
* ArchivePaths - Array<JointPath>
* ArchiveSetID - Int
* Leveled
* Level
* ItemRecordTable - SortedMap<ItemKey, ItemRecord>
* ItemList - MutableMap<ItemID,Item>
* ArchiveMap - MutableMap<Int,Archive>
* ExistenceMark - Pair<ArchiveID,ItemID>
* ExistenceBoard - Array<ExistenceMark?>
* Path - String
* RelativePath - Path
* RealPath - Path
* JointPath - Array<Path>


## Archive

* realArchivePaths
* ans
* itemID
* archiveSetID
* archiveID
* itemMap

### realArchivePaths - Array<JointPath>

Usually, I does not use this for now.
I'll use this for I/O access balancing.

### ans - ArchiveAndStream

For control `Archive`

### itemID - ItemID

`ItemID` of this `Archive`

### archiveSetID - ArchiveSetID

`ArchiveSetID` of this `Archive`

### archiveID - ArchiveID

### itemMap - ItemMap

**Local** `ItemMap` for each `Archive`

## ArchiveSet

* archiveMap
* itemMap
* archiveSetID

### archiveMap

`ArchiveMap` in this `ArchiveSet`

### itemMap

`ItemMap` for every archive in this `ArchiveSet`

### archiveSetID

Just `ArchiveSetID`


## Item

* `dataCRC`
* `dataSize`
* `modifiedDate`
* `path` - JointPath
* `parentID` - Real parent Archive's ItemID
* `idInArchive` - ItemIndex
* `parentArchiveID` - Virtual parent archive's ArchiveID (Not real, but same archive)
* `archiveSetID` - ArchiveSetID
* `id`

## TheTable

### TheTable

* theItemTable - ItemRecordTable
* theItemList - ItemList
* theArchiveSets - Array<ArchiveSet>
* theArchiveMap - ArchiveMap
* archiveSetNum - Int
* tableInstance - Int
* rootOutputDirectory - RealPath

### ItemRecord

* dataCRC - Int
* dataSize - DataSize
* modifiedDate - Date
* path - RelativePath
* existence - ExistenceBoard
* isFilled - Boolean
* isArchive - Boolean? // null when exe is not sure
* isExtracted - Boolean
* isFirstOrSingle - Boolean
