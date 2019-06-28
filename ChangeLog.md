# Changelog for ArchiveDiffer-Kotlin

## [Unreleased]

### Change
* Extract archives only when other file Items is not full-filled

### Add
* Real `CANCEL` button - now exist, but not work
* Real Report type changer - now exist, but not work
* Real Report filter - now exist, but not work


## [0.7.0.0]  -- 2019/06/28

### Added
* Add new ResultView - as List<String> type TableView

### Changed
* Use VBox for allocating components in Tab
* Move some functions to List
* Correct function name `queryInsensitive` to `queryInsensitively`

### Fixed
* Reallocate drag & drop EventHandler code

### Deprecated
* Move Tester.kt out


## [0.6.0.6]  -- 2019/06/24

### Added
* Add messages with archive pre-checking

### Fixed
* Reallocate drag & drop EventHandler code


## [0.6.0.5]  -- 2019/06/23

### Added
* Add close all tab button

### Changed
* Correct confused variable name
* Fix color change code which changes wrong region

### Deprecated
* Drop unnecessary resource files


## [0.6.0.4]  -- 2019/06/23

### Changed
* Change selected tab's border color better

### Deprecated
* Drop old `GUI` class
* Clean up `Sub` class


## [0.6.0.3]  -- 2019/06/23

### Added
* Add warning message dialog when `theWorkingDirectory` does not exist


## [0.6.0.2]  -- 2019/06/23

### Changed
* Bring a tab to front which finished a assigned task
* Refactor functions and classes
* Extend drag & drop region to TabPane


## [0.6.0.1]  -- 2019/06/22

### Changed
* Introduce new GUI - EntryPoint
  * Add MessageBox
  * Generate a new tab when drag & drop
* Refactor many classes/file names/functions
* Correct typos
* Add spaces for better readability of Result text

### Added
* Use gradle build plugin for JavaFX
* Add documents
* Add LICENSE - LGPL 3.0
* Add references of code as annotations


## [0.5.4.2]  -- 2019/02/22

### Changed
* No more hard-coded configuration

### Added
* Add argument parser to generalize configuration


## [0.5.4.1]  -- 2018/12/30

### Fixed
* No more lost duplicated files in TheTable
* Now `getDirectory` returns empty string when there is no directory in given path


## [0.5.4.0]  -- 2018/12/30

### Added
* Add progress messages

### Changed
* Change non-existence file displaying
* Change `filePathsLabel` font bolder
* Show whether the file names are same or not


## [0.5.3.1]  -- 2018/12/29

### Changed
* Clean up warnings


## [0.5.3.0]  -- 2018/12/29

### Fixed
* Displaying with SelectiveTab is working correctly
* Initialize indicator with Go button
* Initialize fileTable with Go button
* Use isGroupingMode and container's size for deciding initialization
* Disable drag and drop when isGroupingMode is false


## [0.5.2.0]  -- 2018/12/29

### Changed
* ArchiveSet system fully implemented

### Fixed
* Use Coroutine-JavaFX to avoid threading problem
* Forget to identify `ItemID` and `Item.idInArchive`
* Add correct child's path
* Fix displaying `filePathsLabel` because of changing `packagedFilePathsWithoutGuide`


## [0.5.1.0]  -- 2018/12/29

### Changed
* Many refactoring/renaming

### Added
* Add `addNewArchive` in ArchiveSet
* Add an document `Note.md`


## [0.5.0.0]  -- 2018/12/29

### Changed
* Redefine Archive and ArchiveSet - Return to original design


## [0.4.1.1]  -- 2018/12/29

### Changed
* Move common/global variables from Environment to Constants
* Limit archive format {ZIP,RAR,RAR4,7z} only


## [0.4.1.0]  -- 2018/12/28

### Added
* Support multi-tasking


## [0.4.0.0]  -- 2018/12/28

Fork ArchiveDiffer from StudyKotlin-JBinding

### Changed
* Change UI as Tab to prepare multi-tasking
* Export redundant logics to `Logic`

### Fixed
* Move some print code which placed wrong point 


## [0.3.2.0]  -- 2018/12/27

### Changed
* Make UI reactively


## [0.3.1.0]  -- 2018/12/25

### Changed
* Make `fileIndicator` more identifiable
* Refine contents of `differencesLabel`


## [0.3.0.1]  -- 2018/12/24

### Fixed
* Missed marking when the Item wasn't registered before


## [0.3.0.0]  -- 2018/12/23

### Changed
* Add more indicators and Redesign GUI
* Use simpleString for `differenceLabel`
* Rename `rawFileAnalyze` as `filePathAnalyze` and returns more simple result


## [0.2.4.0]  -- 2018/12/22

### Fixed
* Exclude CAB archive as ArchiveSet when testing executable file is an archive or not
* Add missing async/await for `makeTheTable`
* Remove `openArchive` from `rawFileAnalyze`


## [0.2.3.0]  -- 2018/12/22

### Added
* Threading opening process
* Treat 7z
* Add manifest for building executable jar


## [0.2.2.0]  -- 2018/12/21

### Added
* Show progress message for runOnce phases

### Fixed
* UI is freezing mo more: Introducing coroutines


## [0.2.1.0]  -- 2018/12/20

### Fixed
* Implement `queryInsensitive`
* Printing miss of `ItemRecord.toString()` when treating `isEXtracted`
* When opening an ArchiveSet which is contained by more then one ArchiveSet,
  fail to marking on existence
* Typo: existance -> existence


## [0.2.0.1]  -- 2018/12/19

### Fixed
* Fix bug when opening exe type archive


## [0.2.0.0]  -- 2018/12/19

### Added
* Could extract/test with nested multi-volume archive


## [0.1.0.2]  -- 2018/12/19

### Added
* itemID for ArchiveSet

### Fixed
* Fix bug that labeling wrong parentArchiveSetID and itemID on existence of theItemTable


## [0.1.0.1]  -- 2018/12/19

### Fixed
* Fix bug with IgnoringList.match
* Close ArchiveSets after analyzing - was implemented in tested code
* Fix some error messages - was indicate wrong function


## [0.1.0.0]  -- 2018/12/19

First release

* Could compare every files in each archive set
  * Even in nested archive
* Could handle executable archive
* Could open/compare multi-volume archive
* Items on IgnoringList will be ignored when comparing
