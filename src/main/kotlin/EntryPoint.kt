import java.io.File
import java.util.*

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.*
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.*
import javafx.scene.layout.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.text.*
import javafx.stage.*
import javafx.util.Callback

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
        val fxml = javaClass.getResource("fxml/NestTab.fxml")
        val aTabSpace: Pane = FXMLLoader.load(fxml)
        val filePathArea= aTabSpace.lookup("#FilePaths") as TextArea // FilePaths TextArea
        val messageBox= aTabSpace.lookup("#MessageBox") as HBox
        val resultTabPane= aTabSpace.lookup("#ResultTab") as TabPane
        val cancelButton= aTabSpace.lookup("#CancelButton") as Button
        val showIgnrBox = aTabSpace.lookup("#ShowIgnored") as CheckBox
        val showExedBox = aTabSpace.lookup("#ShowExtracted") as CheckBox
        val showDirBox = aTabSpace.lookup("#ShowDirectory") as CheckBox

        messageBox.border = Border(BorderStroke(Paint.valueOf("Red"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        resultTabPane.border = Border(BorderStroke(Paint.valueOf("Green"),BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        tab.content = aTabSpace

        filePathArea.text = generatePackagedFilePaths(packagedFilePaths)
        filePathArea.font = Font.font(null,FontWeight.NORMAL,14.0)

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

        // Step: Check archive existence
        var rASV: Pair<MessageType, String>
        rASV = checkArchiveExistence(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)
        if (rASV.first != MessageType.NoProblem) {
            tab.text =  "No Archive"
            tab.style = defaultBlackTabStyle.plus("-fx-background-color: yellow")
            aTabSpace.style = "-fx-background-color: yellow"
            return tab
        }

        rASV = checkArchiveVolume(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)
        when(rASV.first) {
            MessageType.Warning -> {
                tab.text = "Only One"
                tab.style = defaultBlackTabStyle.plus("-fx-background-color: LightSkyBlue")
                aTabSpace.style = "-fx-background-color: CornflowerBlue"
                return tab
                }
            MessageType.Critical -> {
                tab.text = "Missing"
                tab.style = defaultBlackTabStyle.plus("-fx-background-color: yellow")
                aTabSpace.style = "-fx-background-color: yellow"
                return tab
            }
        }

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

            val asNum = theTable!!.archiveSetNum
            val theResult = theTable!!.generateResultStringList()
            val theSameResult = theResult.filter{it[0] == "O"}
            val theDiffResult = theResult.filter{it[0] == "X"}
            // TODO: Hard coded index
            val noExedResult = theDiffResult.filter{it[3] != "E"}
            // TODO: IgnoredResult

            theTable!!.closeAllArchiveSets()
            theTable!!.removeAllArchiveSets()
            theTable = null

            Platform.runLater {

                val allTable = TableView<ObservableList<StringProperty>>()
                val diffTable = TableView<ObservableList<StringProperty>>()
                val sameTable = TableView<ObservableList<StringProperty>>()
                val noExTable = TableView<ObservableList<StringProperty>>()

                makeResultTable(allTable, theResult, asNum, listOf())
                makeResultTable(diffTable, theDiffResult, asNum, listOf(7+asNum))
                makeResultTable(sameTable, theSameResult, asNum, listOf())
                makeResultTable(noExTable, noExedResult, asNum, listOf(7+asNum))
                // TODO: IgnoredResult

                var diffTab: Tab? = null
                if (!(theSameResult.isEmpty() || theDiffResult.isEmpty()))
                    generateResultTab(resultTabPane, ResultType.All, allTable)
                if (theSameResult.isNotEmpty())
                    resultTabPane.selectionModel.select(generateResultTab(resultTabPane, ResultType.Same, sameTable))
                if (theDiffResult.isNotEmpty()) {
                    diffTab =
                        if (showExedBox.isSelected)
                            generateResultTab(resultTabPane, ResultType.Diff, diffTable)
                        else
                            generateResultTab(resultTabPane, ResultType.Diff, noExTable)
                    resultTabPane.selectionModel.select(diffTab)
                }
                // TODO: IgnoredResult

                tab.text = if (count == 0) "Done: $titleFromFileName" else "Diff: $titleFromFileName"
                tab.style = defaultWhiteTabStyle.plus("-fx-background-color: ").plus(if (count == 0) "green;" else "red;")
                aTabSpace.style = "-fx-background-color: ".plus(if (count == 0) "greenyellow;" else "lightcoral;")
                if (count == 0)
                    addMessageLabel(messageBox,MessageType.NoProblem,"No\nProblem")
                else
                    addMessageLabel(messageBox,MessageType.Critical,"Have\nDiff")

                tabPane.selectionModel.select(tab)

                if (diffTab != null) {
                    showExedBox.setOnMouseClicked {
                        diffTab.content = if (showExedBox.isSelected) diffTable else noExTable
                    }
                }
                showDirBox.setOnMouseClicked {
                    for (c in 1..asNum) {
                        allTable.columns[6+asNum+asNum*c].isVisible = showDirBox.isSelected
                        if (sameTable.columns.size != 0)
                            sameTable.columns[6+asNum+asNum*c].isVisible = showDirBox.isSelected
                        if (diffTable.columns.size != 0)
                            diffTable.columns[6+asNum+asNum*c].isVisible = showDirBox.isSelected
                        if (noExTable.columns.size != 0)
                            noExTable.columns[6+asNum+asNum*c].isVisible = showDirBox.isSelected
                    }
                }
            }

            println("End a analysis")
        }

        cancelButton.setOnAction {
            task.cancel()
            if (theTable != null) {
                println("Cancel job ${theTable!!.tableInstance}")
                theTable!!.closeAllArchiveSets()
                theTable!!.removeAllArchiveSets()
            }
        }

        return tab
    }


    private fun generateResultTab (resultTabPane: TabPane, resultType: ResultType, aTable: TableView<ObservableList<StringProperty>>): Tab {
        val tab = Tab()
        tab.text = resultType.toString()
        tab.content = aTable

        resultTabPane.tabs.add(tab)
        return tab
    }

    private fun makeResultTable(
        tableView: TableView<ObservableList<StringProperty>>,
        inputData: List<ResultRow>,
        asNum: Int,
        invisibleList: List<Int>
    ) {
        Platform.runLater {
            tableView.placeholder = Label("Loading....")
            if ( inputData.isNotEmpty()) {
                for (loopIndex in 0.until(inputData[0].size)) {
                    tableView.columns.add(createColumn(loopIndex, "C-$loopIndex"))
                }
                for (aRow in inputData) {
                    val data = FXCollections.observableArrayList<StringProperty>()
                    for (value in aRow) {
                        data.add(SimpleStringProperty(value))
                    }
                    tableView.items.add(data)
                }
                var index = 0
                // Matching Result
                tableView.columns[index++].text = "Match"
                // Hash
                tableView.columns[index++].text = "CRC32"
                // File size
                tableView.columns[index].style = "-fx-alignment: CENTER-RIGHT;"
                tableView.columns[index++].text = "Size"
                // Extracted or not
                tableView.columns[index++].text = "Ex"
                // File Type
                tableView.columns[index++].text = "FT"
                // Archive Type
                tableView.columns[index++].text = "Sv/Mv"
                // Existance
                for (i in 0.until(asNum))
                    tableView.columns[index++].text = "AS$i"
                tableView.columns[index++].text = "Existence"
                // Common Name
                tableView.columns[index++].text = "C. Name"
                for (i in 0.until(asNum)) {
                    tableView.columns[index++].text = "AS$i-Directory"
                    tableView.columns[index++].text = "AS$i-FileName"
                }
                for (i in invisibleList)
                    tableView.columns[i].isVisible = false
            } else {
                tableView.placeholder = Label("No result")
            }
        }
    }

    private fun createColumn(columnIndex: Int, columnTitle: String
    ): TableColumn<ObservableList<StringProperty>, String> {
        val column = TableColumn<ObservableList<StringProperty>, String>()
        column.text = columnTitle
        column.cellValueFactory =
            Callback { cellDataFeatures ->
                cellDataFeatures.value[columnIndex]
            }
        column.cellFactory = TextFieldTableCell.forTableColumn()
        return column
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
        val closeAllButton = root.lookup("#CloseAllButton") as Button

        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS // or SELECTED_TAB, UNAVAILABLE

        singleDropPoint.heightProperty().bind(epPane.heightProperty().divide(32).multiply(19))

        multiDropPoint.yProperty().bind(singleDropPoint.yProperty().add(dropPane.heightProperty().divide(2)))
        multiDropPoint.heightProperty().bind(epPane.heightProperty().divide(8).multiply(3))

        primaryStage.scene = scene
        primaryStage.show()

        val singleColor = singleDropPoint.fill
        val multiColor = multiDropPoint.fill
        val selectedColor = Paint.valueOf("Green")

        tabPane.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }
        tabPane.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            val packagedFilePaths = packageFilePathsWithoutGuide(db.files.map{it.toString()})
            val newAnalyzeTab = generateAnalyzeTab(tabPane, packagedFilePaths)
            event.consume()

            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            event.isDropCompleted = true
        }

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

        closeAllButton.setOnAction {
            tabPane.tabs.last().style = ""
            tabPane.tabs.clear()
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
