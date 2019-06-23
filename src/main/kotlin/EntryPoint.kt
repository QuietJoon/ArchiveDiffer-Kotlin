import java.io.File
import java.util.*

import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.*
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.layout.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.text.*
import javafx.stage.*

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import archive.*
import util.*


fun main(args : Array<String>) {
    println("EntryPoint")

    Application.launch(EntryPoint().javaClass, *args)

}

class EntryPoint : Application() {

    private var tabCount = 0
    private val defaultMessageLabelStyle = "-fx-stroke: white; -fx-padding: 6 6 6 6; -fx-font-size: 16px; -fx-font-weight: bold;"
    private val defaultWhiteMessageLabelStyle = defaultMessageLabelStyle.plus(" -fx-text-fill: white;")
    private val defaultBlackMessageLabelStyle = defaultMessageLabelStyle.plus(" -fx-text-fill: Black;")
    private val defaultTabStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-focus-color: yellow; -fx-faint-focus-color: transparent;"
    private val defaultWhiteTabStyle = defaultTabStyle.plus(" -fx-text-base-color: white;")
    private val defaultBlackTabStyle = defaultTabStyle.plus(" -fx-text-base-color: Black;")

    private fun generateAnalyzeTab (tabPane: TabPane, packagedFilePaths: Array<ArchiveSetPaths>): Tab {
        val tab = Tab()
        tabCount += 1
        tab.text = "Tab$tabCount"
        val fxml = javaClass.getResource("fxml/Tab.fxml")
        val aTabSpace: Pane = FXMLLoader.load(fxml)
        val filePathArea= aTabSpace.lookup("#FilePaths") as TextArea // FilePaths TextArea
        val messageBox= aTabSpace.lookup("#MessageBox") as HBox
        val resultBox= aTabSpace.lookup("#ResultBox") as VBox
        val cancelButton= aTabSpace.lookup("#CancelButton") as Button

        messageBox.border = Border(BorderStroke(Paint.valueOf("Red"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        resultBox.border = Border(BorderStroke(Paint.valueOf("Green"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        tab.content = aTabSpace

        filePathArea.text = generatePackagedFilePaths(packagedFilePaths)
        filePathArea.font = Font.font(null,FontWeight.NORMAL,14.0)

        // Step: Check archive existence
        var rASV: Pair<MessageType, String>
        rASV = checkArchiveExistence(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)
        rASV = checkArchiveVolume(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)

        // TODO: Not implemented yet
        val titleFromFileName = ""

        var theTable: TheTable? = null

        print("Make the table for $titleFromFileName\n")
        tab.text = "Table Making: $titleFromFileName"
        tab.style = defaultBlackTabStyle

        val task = GlobalScope.launch {
            theTable = makeTheTable(packagedFilePaths, theWorkingDirectory)

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
            theTable = null

            Platform.runLater {
                val diffResult = TextArea()
                val sameResult = TextArea()

                resultBox.children.add(diffResult)
                resultBox.children.add(sameResult)

                diffResult.setMaxSize(2000.0,2000.0)
                diffResult.setPrefSize(2000.0,400.0)
                sameResult.setMaxSize(2000.0,2000.0)
                sameResult.setPrefSize(2000.0,0.0)

                // TODO: Get same/diff ratio, and apply it against height
                aTabSpace.heightProperty().addListener{ _, _, newVal ->
                    val height = newVal.toDouble()-240.0
                    diffResult.setPrefSize(0.0, height)
                    sameResult.setPrefSize(0.0, 0.0)
                }

                tab.text = if (count == 0) "Done: $titleFromFileName" else "Diff: $titleFromFileName"
                //Paint.valueOf(if (count == 0) "Green" else "Red")
                tab.style = defaultWhiteTabStyle.plus("-fx-background-color: ").plus(if (count == 0) "green;" else "red;")
                aTabSpace.style = "-fx-background-color: ".plus(if (count == 0) "greenyellow;" else "lightcoral;")
                if (count == 0)
                    addMessageLabel(messageBox,MessageType.NoProblem,"No\nProblem")
                else
                    addMessageLabel(messageBox,MessageType.Critical,"Have\nDiff")
                diffResult.text = resultList.joinToString(separator = "\n")
                tabPane.selectionModel.select(tab)
            }

            println("End a phase")
        }

        cancelButton.setOnAction {
            task.cancel()
            if (theTable != null) {
                println("Cancel job ${theTable!!.tableInstance}")
                theTable!!.closeAllArchiveSets()
                theTable!!.removeAllArchiveSets()
            }
        }

        aTabSpace.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }
        aTabSpace.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            val packagedFilePaths = packageFilePathsWithoutGuide(db.files.map{it.toString()})
            val newAnalyzeTab = generateAnalyzeTab(tabPane, packagedFilePaths)
            event.consume()

            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            event.isDropCompleted = true
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
        mb.children.add(0, messageLabel)
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

            val newAnalyzeTab = generateAnalyzeTab(tabPane, packagedFilePaths!!)
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
            val newAnalyzeTab = generateAnalyzeTab(tabPane, packagedFilePaths)
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

        if (!File(theWorkingDirectory).exists()) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Working directory is missing"
            alert.contentText = "Working directory $theWorkingDirectory is missing"
            alert.initOwner(primaryStage)
            alert.graphic = null
            alert.show()
        }

    }
}
