import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.*
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.layout.*
import javafx.stage.*

import util.checkArchiveExistence
import util.generatePackagedFilePaths
import util.packageFilePathsWithoutGuide
import archive.checkArchiveVolume
import javafx.collections.FXCollections
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.packageFilePathsForGrouped
import java.util.*


fun main(args : Array<String>) {
    println("EntryPoint")

    Application.launch(EntryPoint().javaClass, *args)

}

class EntryPoint : Application() {

    private var tabCount = 0
    private val defaultWhiteMessageLabelStyle = "-fx-stroke: white; -fx-padding: 4 4 4 4; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
    private val defaultBlackMessageLabelStyle = "-fx-stroke: white; -fx-padding: 4 4 4 4; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: Black;"

    private fun generateAnalyzeTab (packagedFilePaths: Array<ArchiveSetPaths>): Tab {
        val tab = Tab()
        tabCount += 1
        tab.text = "Tab$tabCount"
        val fxml = javaClass.getResource("fxml/Tab.fxml")
        val aTabPane: Pane = FXMLLoader.load(fxml)
        val filePathArea= aTabPane.lookup("#FilePaths") as TextArea // FilePaths TextArea
        val messageBox= aTabPane.lookup("#MessageBox") as HBox // MessageBox HBox
        val resultBox= aTabPane.lookup("#ResultBox") as VBox // ResultBox VBox

        messageBox.border = Border(BorderStroke(Paint.valueOf("Red"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        resultBox.border = Border(BorderStroke(Paint.valueOf("Green"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        tab.content = aTabPane

        filePathArea.text = generatePackagedFilePaths(packagedFilePaths)

        // Step: Check archive existence
        var rASV: Pair<MessageType, String>
        rASV = checkArchiveExistence(packagedFilePaths)
        if (rASV.first != MessageType.NoProblem) addMessageLabel(messageBox, rASV.first, rASV.second)
        rASV = checkArchiveVolume(packagedFilePaths)
        if (rASV.first != MessageType.NoProblem) addMessageLabel(messageBox, rASV.first, rASV.second)

        // TODO: Not implemented yet
        val titleFromFileName = ""

        var theTable: TheTable? = null
        var doesTheTableExist = false
        print("Make the table for $titleFromFileName\n")
        tab.text = "Table Making: $titleFromFileName"

        GlobalScope.launch {
            theTable = makeTheTable(packagedFilePaths, theWorkingDirectory)
            doesTheTableExist = true

            Platform.runLater {
                tab.text = "Analyzing: $titleFromFileName"
            }

            theTable!!.prepareWorkingDirectory()

            theTable!!.printStatus()
            theTable!!.printResult()

            var runCount = 1
            while (true) {
                print("Phase #$runCount: $titleFromFileName\n")
                Platform.runLater {
                    tab.text = "Phase #$runCount - $titleFromFileName"
                }
                if (theTable!!.runOnce()) break

                theTable!!.printStatus()
                theTable!!.printResult()

                runCount++
            }

            val result = theTable!!.printFinalResult()
            var count = result.first
            val resultList = result.second

            if (count == 0) {
                print("Have no different files in the ArchiveSets\n")
                resultList.add(0,"Have no different files in the ArchiveSets")
            }

            theTable!!.closeAllArchiveSets()
            theTable!!.removeAllArchiveSets()

            Platform.runLater {
                tab.text = if (count == 0) "Done: $titleFromFileName" else "Diff: $titleFromFileName"
                //Paint.valueOf(if (count == 0) "Green" else "Red")
                tab.style = "-fx-background-color: ".plus(if (count == 0) "green;" else "red;")
            }

            println("End a phase")
        }

        return tab
    }

    private fun addMessageLabel (mb: HBox, mt: MessageType, msg: String) {
        val messageLabel = Label(msg)
        messageLabel.style = when(mt) {
            MessageType.Critical -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: red;")
            MessageType.Bad -> defaultBlackMessageLabelStyle.plus("-fx-background-color: yellow;")
            MessageType.Warning -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: blue")
            MessageType.NoProblem -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: green")
        }
        mb.children.add(messageLabel)
    }

    private fun openMASGrouper (tabPane: TabPane, unpackagedFilePaths: List<Path>) {
        var packagedFilePaths: Array<ArchiveSetPaths>? = null

        val masgStage = Stage()
        masgStage.title = "MAS Grouper"
        masgStage.isAlwaysOnTop = true

        val fxml = javaClass.getResource("fxml/MASGrouper.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)

        masgStage.scene = scene
        masgStage.show()

        val candidateTable = root.lookup("#CandidateTableView") as TableView<GroupedFile>
        val groupingButton= root.lookup("#MakeGroupButton") as Button
        val goButton= root.lookup("#GoButton") as Button

        goButton.setDisable(true)

        candidateTable.columns[1].style = "-fx-alignment: CENTER-RIGHT;"

        val fileList = unpackagedFilePaths.map {
            val anGroupingFile = GroupedFile(false, 0, it)
            anGroupingFile.select.addListener { _, _, new_val ->
                print("Set " + anGroupingFile.getPath() + " as '" + new_val + "'.\n")
            }
            anGroupingFile
        }

        candidateTable.items = FXCollections.observableArrayList(fileList)

        goButton.setOnAction {

            val groupIDSet: SortedSet<Int> = sortedSetOf()
            candidateTable.items.forEach { groupedFile ->
                if (groupedFile != null)
                    groupIDSet.add(groupedFile.getGroupID())
            }

            while (groupIDSet.last() != groupIDSet.size - 1) {
                var smallestGroupID = 0
                for (idx in groupIDSet) {
                    if (smallestGroupID < idx) break
                    smallestGroupID++
                }
                for (anItem in candidateTable.items) {
                    anItem.setGroupID(smallestGroupID)
                }
            }

            val packagedFilePathList = mutableListOf<ArchiveSetPaths>()

            if (groupIDSet.size > 1) {
                for (i in 0 until groupIDSet.size) {
                    val unpackagedPathList = mutableListOf<RealPath>()
                    for (anItem in candidateTable.items) {
                        if (anItem.getGroupID() == i)
                            unpackagedPathList.add(anItem.getPath())
                    }
                    packagedFilePathList.add(packageFilePathsForGrouped(unpackagedPathList))
                }
                packagedFilePaths = packagedFilePathList.toTypedArray()
            }

            val newAnalyzeTab = generateAnalyzeTab(packagedFilePaths!!)
            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            masgStage.close()
        }

        groupingButton.setOnAction {
            val groupIDSet: SortedSet<Int> = sortedSetOf()

            for (anItem in candidateTable.items) {
                if (anItem.isSelected)
                    print(anItem.getPath().plus("\n"))
                else
                    groupIDSet.add(anItem.getGroupID())
            }
            var newID = 0
            for ( idx in groupIDSet ) {
                if (newID < idx) break
                newID++
            }
            for (anItem in candidateTable.items) {
                if (anItem.isSelected)
                    anItem.setGroupID(newID)
                // TODO: [BUG] This does not update GUI represetation
                anItem.isSelected = false
            }

            goButton.setDisable(groupIDSet.size == 0)

            // TODO: Too bad solution, but works...
            candidateTable.refresh()
        }
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "EntryPoint"
        primaryStage.isAlwaysOnTop = true
        val fxml = javaClass.getResource("fxml/EntryPoint.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)

        val epPane= root.lookup("#EPPane") as AnchorPane // Entry Point Pane
        val dropPane= root.lookup("#DropPane") as AnchorPane // Drop Pane
        val tabPane= root.lookup("#TabPane") as TabPane // Tab Pane
        val singleDropPoint= root.lookup("#ForSingle") as Rectangle // Single-ArchiveSet drop point
        val multiDropPoint = root.lookup("#ForMulti") as Rectangle // Multi-ArchiveSet drop point

        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS // or SELECTED_TAB, UNAVAILABLE

        singleDropPoint.heightProperty().bind(epPane.heightProperty().divide(32).multiply(19))

        multiDropPoint.yProperty().bind(singleDropPoint.yProperty().add(dropPane.heightProperty().divide(2)))
        multiDropPoint.heightProperty().bind(epPane.heightProperty().divide(8).multiply(3))

        primaryStage.scene = scene
        primaryStage.show()

        val singleColor = singleDropPoint.fill
        val multiColor = multiDropPoint.fill
        val selectedColor = Paint.valueOf("Green")

        singleDropPoint.onDragEntered = EventHandler { event ->
            singleDropPoint.fill = selectedColor
            event.consume()
        }
        singleDropPoint.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }
        singleDropPoint.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            val packagedFilePaths = packageFilePathsWithoutGuide(db.files.map{it.toString()})
            val newAnalyzeTab = generateAnalyzeTab(packagedFilePaths)
            event.consume()

            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            event.isDropCompleted = true
        }
        singleDropPoint.onDragExited = EventHandler { event ->
            singleDropPoint.fill = singleColor
            event.consume()
        }

        multiDropPoint.onDragEntered = EventHandler { event ->
            multiDropPoint.fill = selectedColor
            event.consume()
        }
        multiDropPoint.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }
        multiDropPoint.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            val unpackagedFilePaths = db.files.map{it.toString()}
            event.consume()
            // TODO: Open MAS Grouper with packagedFilePaths
            openMASGrouper(tabPane, unpackagedFilePaths)
        }
        multiDropPoint.onDragExited = EventHandler { event ->
            multiDropPoint.fill = multiColor
            event.consume()
        }
    }
}
