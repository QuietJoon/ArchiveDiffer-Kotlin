# Changelog for ArchiveDiffer-Kotlin

## Unreleased changes

### Change
* Implement real `ArchiveSet`
  * Currently, `ArchiveSet` is just an archive
* Add space holder for missing Item's full-name for `differencesLabel`
* Make more pretty columned text output
* Extract archives only when other file Items is not full-filled

## 0.5.1.0  -- 2018/12/29

### Changed
* Many refactoring/renaming

### Added
* Add `addNewArchive` in ArchiveSet
* Add an document `Note.md`

## 0.5.0.0  -- 2018/12/29

### Changed
* Redefine Archive and ArchiveSet - Return to original design

## 0.4.1.1  -- 2018/12/29

### Changed
* Move common/global variables from Environment to Constants
* Limit archive format {ZIP,RAR,RAR4,7z} only

## 0.4.1.0  -- 2018/12/28

### Added
* Support multi-tasking

## 0.4.0.0  -- 2018/12/28

Fork ArchiveDiffer from StudyKotlin-JBinding

### Changed
* Change UI as Tab to prepare multi-tasking
* Export redundant logics to `Logic`

### Fixed
* Move some print code which placed wrong point 

## 0.3.2.0  -- 2018/12/27

### Changed
* Make UI reatively

## 0.3.1.0  -- 2018/12/25

### Changed
* Make `fileIndicator` more identifiable
* Refine contents of `differencesLabel`

## 0.3.0.1  -- 2018/12/24

### Fixed
* Missed marking when the Item wasn't registered before

## 0.3.0.0  -- 2018/12/23

### Changed
* Add more indicators and Redesign GUI
* Use simpleString for `differenceLabel`
* Rename `rawFileAnalyze` as `filePathAnalyze` and returns more simple result

## 0.2.4.0  -- 2018/12/22

### Fixed
* Exclude CAB archive as ArchiveSet when testing executable file is an archive or not
* Add missing async/await for `makeTheTable`
* Remove `openArchive` from `rawFileAnalyze`

## 0.2.3.0  -- 2018/12/22

### Added
* Threading opening process
* Treat 7z
* Add manifest for building executable jar

## 0.2.2.0  -- 2018/12/21

### Added
* Show progress message for runOnce phases

### Fixed
* UI is freezing mo more: Introducing coroutines

## 0.2.1.0  -- 2018/12/20

### Fixed
* Implement `queryInsensitive`
* Printing miss of `ItemRecord.toString()` when treating `isEXtracted`
* When opening an ArchiveSet which is contained by more then one ArchiveSet,
  fail to marking on existence
* Typo: existance -> existence

## 0.2.0.1  -- 2018/12/19

### Fixed
* Fix bug when opening exe type archive

## 0.2.0.0  -- 2018/12/19

### Added
* Could extract/test with nested multi-volume archive

## 0.1.0.2  -- 2018/12/19

### Added
* itemID for ArchiveSet

### Fixed
* Fix bug that labeling wrong parentArchiveSetID and itemID on existence of theItemTable

## 0.1.0.1  -- 2018/12/19

### Fixed
* Fix bug with IgnoringList.match
* Close ArchiveSets after analyzing - was implemented in tested code
* Fix some error messages - was indicate wrong function

## 0.1.0.0  -- 2018/12/19

First release

* Could compare every files in each archive set
  * Even in nested archive
* Could handle executable archive
* Could open/compare multi-volume archive
* Items on IgnoringList will be ignored when comparing
