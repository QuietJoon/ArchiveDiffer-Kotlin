Structures
====

## Package

### `archive` package

`archive` package is my own neat wrapper for **SevenZipJBinding**

#### archive.Constants

Define constants for `archive` package

#### archive.Data

Definition just for ExtractionException of this wrapper

#### archive.Extract

Virtual wrapper of **SevenZipJBinding**

* class
  * `Extract`
* function
  * `prepareOutputDirectory`
  * `extractEverything`
  * `extractSomething`

#### archive.File

Provides more neat functions to handle archives

* class
  * `ArchiveAndStream`
  * `ArchiveOpenVolumeCallback`
* function
  * `openArchive`
  * `openSingleVolumeArchive`
  * `openMultiVolumeArchive`

#### archive.JBinding

Functions just for **SevenZipJBinding**

#### archive.List

Provide some visual-listing functions with `IInArchive`

* function
  * `printItemList`
  * `printItemListByIDs`

### `ui` package

UI logics which is almostly independent from main logics of **ArchiveDiffer-Kotlin**

#### ui.JavaFX

Define `CheckBoxColumn` for `TableView`

### `util` package

Common and independent from main logics of **ArchiveDiffer-Kotlin**

#### util.Argument

Configurations for **ArgParser**

#### util.Collection

Collection code.
Disabled now because its logic is not yet supported by current Kotlin

#### util.File

A function set for read & write a file

#### util.StringManager

Manipulating functions with `String`, `ArchivePaths`, `ArchiveSetPaths`, `JointPath`, and `Path`

### Default package

#### Archive

Handle a Archive

* `init`
* `addNewItem`
  * Definition
    * Generate an `Item` from from`ISimpleInArchiveItem`
    * Check whether the `Item` is on `theIgnoringList`
    * And, add the `Item` to `ItemMap` of `Archive`
  * Features
    * Could handle duplicate `Item`
* `getThisIDs`
  * Get every `ItemIndices` of `Item` in this `Archive`
* `getInArchive`
  * Returns `inArchive` of the `Archive`

#### ArchiveSet

Handle a ArchiveSet

* `init`
* `addNewItem`
* `addNewArchive`
* `getInArchive`
  * Return `InArchive` in this `ArchiveSet` by `ArchiveID`
* `getThisIDs`
  * Returns `ItemIndices` of `Item` in this `ArchiveSet`

#### Config

Defines default configuration

* `theWorkingDirectory`
* `dataFormat`

#### Constants

* `hashPrime`
* `theIgnoringList`

#### Data

* `Leveled`
* `Level`

#### GUI

Logic with GUI

* `start`
* `initTab`
* `initSelectiveTab`

#### IgnoringList

* class
  * `IgnoringItem`
  * `IgnoringList`
* function
  * `makeItemFromRawItem`
  * `match`
  * `ignoringListFromString`
  * `readIgnoringList`
  * `writeIgnoringList`

#### Item

* class
  * `Item`
* function
  * `getFullName`
  * `generateItemKey`
  * `makeItemRecordFromItem`
  * `checkArchiveName`
  * `equals`
  * `equalsWithoutRealPath`
  * `hashCode`
  * `makeItemFromArchiveItem`

#### List

* `printIgnoringList`
* `printIgnoringListWithLevel`
* `getIDArrayWithoutIgnoringItem`
* `printITemMapOfArchiveSet`

#### Logic

* `printStatus`
* `printResult`
* `printFinalResult`

#### Main

Only entry point of this program

#### Sub

Some substantial functions which Main calls

* `initialize`
* `makeTheTable`

#### TheTable

A unit of task

* class
  * `TheTable`
  * `ItemRecord`
* function
  * `init`
  * `registerArchiveSetItemRecord`
  * `registerAnItemRecordWithExistence`
  * `queryInsensitive`
  * `isFilled`
  * `getFirstItemKey`
  * `modifyKeyOfTheItemTable`
  * `printSameItemTable`
  * `findMultiVolumes`
  * `runOnce`
  * `closeAllArchiveSets`
  * `removeAllArchiveSets`
  * `prepareWorkingDirectory`
  * ItemRecord.`toString`
  * `simpleString`
  * `managedString`
  * `getAnyID`
