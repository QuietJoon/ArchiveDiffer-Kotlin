import java.util.*
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.input.TransferMode
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.*

import util.*


class GUI : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ArchiveDiffer-Kotlin"
        primaryStage.isAlwaysOnTop = true
        val fxml = javaClass.getResource("fxml/Main.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)

        // TODO: sessionID=4 is little different
        for ( sessionID in 1..3)
            initTab(root, sessionID)
        initSelectiveTab(root, 4)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun initTab(root: Parent, sessionID: Int) {

        val tab = root.lookup("#TabSession$sessionID")
        val fileIndicator = root.lookup("#FileIndicator$sessionID") as Rectangle // For number of proper input file
        val filePathsLabel = root.lookup("#FilePathsLabel$sessionID") as Label // Name of input file
        val statusIndicator = root.lookup("#StatusIndicator$sessionID") as Rectangle // Show progress
        val differencesLabel = root.lookup("#DifferencesLabel$sessionID") as TextArea
        val analyzedIndicator = root.lookup("#AnalyzedIndicator$sessionID") as Rectangle // Show final result
        var fileSwitch = true

        tab.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY)
            } else {
                event.consume()
            }
        }

        tab.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            var success = false
            if (db.hasFiles()) {
                success = true

                statusIndicator.fill = Paint.valueOf("DODGERBLUE")
                analyzedIndicator.fill = Paint.valueOf("#eeff99")

                val packagedFilePaths = packageFilePathsWithoutGuide(db.files.map{it.toString()})

                val sb = StringBuilder()
                packagedFilePaths.forEachIndexed { sIdx, archiveSetPaths ->
                    println("ArchiveSet $sIdx")
                    archiveSetPaths.forEachIndexed { aIdx, archivePaths ->
                        println("\tArchive $aIdx")
                        sb.append(String.format("%4s %4s %s\n", sIdx, aIdx, archivePaths[0].last()))
                        for (aPath in archivePaths) {
                            println("\t\t" + aPath.last())

                        }
                    }
                }
                val str = sb.toString()
                filePathsLabel.text = str

                for ( archiveSetPaths in packagedFilePaths) {
                    println("ArchiveSet")
                    for ( archivePaths in archiveSetPaths ) {
                        println("\tArchive")
                        for (aPath in archivePaths) {
                            println("\t\t" + aPath.last())
                        }
                    }
                }

                GlobalScope.launch {
                    for ( i in 0..10 ) {
                        fileIndicator.fill = Paint.valueOf(if (fileSwitch) "Blue" else "White")
                        fileSwitch = !fileSwitch
                        delay(100L)
                    }
                }

                var theTable: TheTable? = null
                var doesTheTableExist = false
                println("Make the table")
                GlobalScope.launch {
                    theTable = makeTheTable(packagedFilePaths, theDebugDirectory)
                    doesTheTableExist = true
                }

                var isJobFinished = false
                GlobalScope.launch {
                    while ( !doesTheTableExist ) {
                        statusIndicator.fill = Paint.valueOf("Gray")
                        delay(19L)
                    }
                    differencesLabel.text = "Start Analyzing"

                    theTable!!.prepareWorkingDirectory()

                    theTable!!.printStatus()
                    theTable!!.printResult()

                    var runCount = 1
                    while (true) {
                        println("Phase #$runCount")
                        if (theTable!!.runOnce()) break

                        theTable!!.printStatus()
                        theTable!!.printResult()

                        runCount++
                    }

                    val result = theTable!!.printFinalResult()
                    val count = result.first
                    val resultList = result.second

                    if (count == 0) {
                        println("Have no different files in the ArchiveSets")
                        resultList.add(0,"Have no different files in the ArchiveSets")
                    }

                    statusIndicator.fill = Paint.valueOf("Green")
                    analyzedIndicator.fill = Paint.valueOf(if (count == 0) "Green" else "Red")


                    isJobFinished = true
                    delay(17L)

                    differencesLabel.text = resultList.joinToString(separator = "\n")

                    theTable!!.closeAllArchiveSets()
                    theTable!!.removeAllArchiveSets()

                    println("End a phase")
                }

                GlobalScope.launch {
                    while (!isJobFinished) {
                        if (doesTheTableExist)
                            differencesLabel.text = "Now analyzing"
                        delay(31L)
                    }
                }

            } else {
                filePathsLabel.text = "No File"
                statusIndicator.fill = Paint.valueOf("Pink")
            }
            event.isDropCompleted = success
            event.consume()
        }
    }
    private fun initSelectiveTab(root: Parent, sessionID: Int) {

        var packagedFilePaths: Array<ArchiveSetPaths>? = null

        val tab = root.lookup("#TabSession$sessionID")
        val fileIndicator = root.lookup("#FileIndicator$sessionID") as Rectangle // For number of proper input file
        val filePathsLabel = root.lookup("#FilePathsLabel$sessionID") as Label // Name of input file
        val statusIndicator = root.lookup("#StatusIndicator$sessionID") as Rectangle // Show progress
        val differencesLabel = root.lookup("#DifferencesLabel$sessionID") as TextArea
        val analyzedIndicator = root.lookup("#AnalyzedIndicator$sessionID") as Rectangle // Show final result

        val fileTable = root.lookup("#TableView$sessionID") as TableView<GroupedFile>
        val newButton = root.lookup("#NewButton$sessionID") as Button
        val goButton = root.lookup("#GoButton$sessionID") as Button

        var isGroupingMode = true

        fun switchMode() {
            fileIndicator.isVisible = !fileIndicator.isVisible
            filePathsLabel.isVisible = !filePathsLabel.isVisible
            statusIndicator.isVisible = !statusIndicator.isVisible
            differencesLabel.isVisible = !differencesLabel.isVisible
            analyzedIndicator.isVisible = !analyzedIndicator.isVisible

            fileTable.isVisible = !fileTable.isVisible
            newButton.isVisible = !newButton.isVisible

            isGroupingMode = !isGroupingMode
            println("Is grouping mode: $isGroupingMode")
        }

        fileTable.columns[1].style = "-fx-alignment: CENTER-RIGHT;"


        tab.onDragOver = EventHandler { event ->
            if (isGroupingMode) {
                val db = event.dragboard
                if (db.hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY)
                } else {
                    event.consume()
                }
            }
        }

        tab.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            var success = false
            if (db.hasFiles()) {
                success = true

                statusIndicator.fill = Paint.valueOf("DODGERBLUE")
                analyzedIndicator.fill = Paint.valueOf("#eeff99")

                if ( packagedFilePaths != null)
                    switchMode()

                val fileList = db.files.map {
                    val anGroupingFile = GroupedFile(false, 0, it.toString())
                    anGroupingFile.select.addListener { _, old_val, new_val ->
                        println(
                            anGroupingFile.getPath() + "'s CB status changed from '"
                                    + old_val + "' to '" + new_val + "'."
                        )
                    }
                    anGroupingFile
                }

                fileTable.items = FXCollections.observableArrayList(fileList)

                var doesLabelSet = false
                GlobalScope.launch(Dispatchers.JavaFx) {
                    while (isGroupingMode) delay(100L)
                    if (packagedFilePaths == null) error("[ERROR]<initSelectiveTab>: packagedFilePath is not set yet")

                    statusIndicator.fill = Paint.valueOf("Black")
                    analyzedIndicator.fill = Paint.valueOf("GRAY")

                    val sb = StringBuilder()
                    packagedFilePaths!!.forEachIndexed { sIdx, archiveSetPaths ->
                        println("ArchiveSet $sIdx")
                        archiveSetPaths.forEachIndexed { aIdx, archivePaths ->
                            println("\tArchive $aIdx")
                            sb.append(String.format("%4s %4s %s\n", sIdx, aIdx, archivePaths[0].last()))
                            for (aPath in archivePaths) {
                                println("\t\t" + aPath.last())

                            }
                        }
                    }
                    val str = sb.toString()
                    filePathsLabel.text = str
                    println(str)
                    doesLabelSet = true
                }

                var theTable: TheTable? = null
                var doesTheTableExist = false
                GlobalScope.launch(Dispatchers.JavaFx) {
                    while (!doesLabelSet) delay(100L)
                    println("Make the table")
                    theTable = makeTheTable(packagedFilePaths!!, theDebugDirectory)
                    doesTheTableExist = true
                }

                var isJobFinished = false
                GlobalScope.launch(Dispatchers.JavaFx) {
                    while (!doesTheTableExist) {
                        statusIndicator.fill = Paint.valueOf("Gray")
                        delay(19L)
                    }
                    differencesLabel.text = "Start Analyzing"

                    theTable!!.prepareWorkingDirectory()

                    theTable!!.printStatus()
                    theTable!!.printResult()

                    var runCount = 1
                    while (true) {
                        println("Phase #$runCount")
                        if (theTable!!.runOnce()) break

                        theTable!!.printStatus()
                        theTable!!.printResult()

                        runCount++
                    }

                    val result = theTable!!.printFinalResult()
                    val count = result.first
                    val resultList = result.second

                    if (count == 0) {
                        println("Have no different files in the ArchiveSets")
                        resultList.add(0,"Have no different files in the ArchiveSets")
                    }

                    statusIndicator.fill = Paint.valueOf("Green")
                    analyzedIndicator.fill = Paint.valueOf(if (count == 0) "Green" else "Red")

                    isJobFinished = true
                    delay(17L)

                    differencesLabel.text = resultList.joinToString(separator = "\n")

                    theTable!!.closeAllArchiveSets()
                    theTable!!.removeAllArchiveSets()

                    println("End a phase")
                }

                GlobalScope.launch(Dispatchers.JavaFx) {
                    while (!isJobFinished) {
                        if (doesTheTableExist)
                            differencesLabel.text = "Now analyzing"
                        delay(31L)
                    }
                }
            } else {
                filePathsLabel.text = "No File"
                statusIndicator.fill = Paint.valueOf("Pink")
            }
            event.isDropCompleted = success
            event.consume()
        }

        newButton.setOnAction {
            val groupIDSet: SortedSet<Int> = sortedSetOf()

            for (anItem in fileTable.items) {
                if (anItem.isSelected)
                    println(anItem.getPath())
                else
                    groupIDSet.add(anItem.getGroupID())
            }
            var newID = 0
            for ( idx in groupIDSet ) {
                if (newID < idx) break
                newID++
            }
            for (anItem in fileTable.items) {
                if (anItem.isSelected)
                    anItem.setGroupID(newID)
                // TODO: [BUG] This does not update GUI
                anItem.isSelected = false
            }

            // TODO: Too bad, but works...
            fileTable.refresh()
        }

        goButton.setOnAction {
            if (isGroupingMode) {
                if (fileTable.items.isNotEmpty()) {
                    val groupIDSet: SortedSet<Int> = sortedSetOf()
                    fileTable.items.forEach { groupedFile ->
                        if (groupedFile != null)
                            groupIDSet.add(groupedFile.getGroupID())
                    }

                    while (groupIDSet.last() != groupIDSet.size - 1) {
                        var smallestGroupID = 0
                        for (idx in groupIDSet) {
                            if (smallestGroupID < idx) break
                            smallestGroupID++
                        }
                        for (anItem in fileTable.items) {
                            anItem.setGroupID(smallestGroupID)
                        }
                    }

                    val packagedFilePathList = mutableListOf<ArchiveSetPaths>()

                    if (groupIDSet.size > 1) {
                        for (i in 0 until groupIDSet.size) {
                            val unpackagedPathList = mutableListOf<RealPath>()
                            for (anItem in fileTable.items) {
                                if (anItem.getGroupID() == i)
                                    unpackagedPathList.add(anItem.getPath())
                            }
                            packagedFilePathList.add(packageFilePathsForGrouped(unpackagedPathList))
                        }
                        packagedFilePaths = packagedFilePathList.toTypedArray()

                        switchMode()
                    }
                }
            } else {
                fileIndicator.fill = Paint.valueOf("DODGERBLUE")
                statusIndicator.fill = Paint.valueOf("DODGERBLUE")
                analyzedIndicator.fill = Paint.valueOf("#eeff99")
                differencesLabel.text = "Waiting"

                packagedFilePaths = null

                // NOTE: Do not reset fileTable, because a user can try it again
                //fileTable.items = FXCollections.observableArrayList()
                fileTable.refresh()

                switchMode()
            }

        }
    }
}
