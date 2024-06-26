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
import kotlinx.coroutines.DelicateCoroutinesApi
import util.*


fun main(args: Array<String>) {
    println("EntryPoint")

    Application.launch(EntryPoint().javaClass, *args)
}

class EntryPoint : Application() {

    private var tabCount = 0
    private val defaultMessageLabelStyle =
        "-fx-stroke: white; -fx-padding: 6 6 6 6; -fx-font-size: 16px; -fx-font-weight: bold;"
    private val defaultWhiteMessageLabelStyle = defaultMessageLabelStyle.plus(" -fx-text-fill: white;")
    private val defaultBlackMessageLabelStyle = defaultMessageLabelStyle.plus(" -fx-text-fill: Black;")
    private val defaultTabStyle =
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-focus-color: yellow; -fx-faint-focus-color: transparent;"
    private val defaultWhiteTabStyle = defaultTabStyle.plus(" -fx-text-base-color: white;")
    private val defaultBlackTabStyle = defaultTabStyle.plus(" -fx-text-base-color: Black;")

    @DelicateCoroutinesApi
    private fun generateAnalyzeTab(tabPane: TabPane, packagedFilePaths: Array<ArchiveSetPaths>): Tab {
        tabCount += 1

        // Interface
        val tab = Tab()
        tab.text = "Tab$tabCount"
        val fxml = javaClass.getResource("fxml/NestTab.fxml")
        val aTabSpace: Pane = FXMLLoader.load(fxml)
        val filePathArea = aTabSpace.lookup("#FilePaths") as TextArea // FilePaths TextArea
        val messageBox = aTabSpace.lookup("#MessageBox") as HBox
        val resultTabPane = aTabSpace.lookup("#ResultTab") as TabPane
        val cancelButton = aTabSpace.lookup("#CancelButton") as Button
        val showIgnrBox = aTabSpace.lookup("#ShowIgnored") as CheckBox
        val showExedBox = aTabSpace.lookup("#ShowExtracted") as CheckBox
        val showDirBox = aTabSpace.lookup("#ShowDirectory") as CheckBox

        messageBox.border = Border(
            BorderStroke(
                Paint.valueOf("Red"),
                BorderStrokeStyle.DASHED,
                CornerRadii.EMPTY,
                BorderWidths.DEFAULT
            )
        )
        resultTabPane.border = Border(
            BorderStroke(
                Paint.valueOf("Green"),
                BorderStrokeStyle.DASHED,
                CornerRadii.EMPTY,
                BorderWidths.DEFAULT
            )
        )
        tab.content = aTabSpace

        filePathArea.text = generatePackagedFilePaths(packagedFilePaths)
        filePathArea.font = Font.font(null, FontWeight.NORMAL, 14.0)

        // Drag & Drop
        aTabSpace.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            // TODO: Add popup for missing/corrupted files
            val (newPackagedFilePaths, checks) = packageFilePathsWithoutGuide(db.files.map { it.toString() })
            val newAnalyzeTab = generateAnalyzeTab(tabPane, newPackagedFilePaths)
            event.consume()

            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            event.isDropCompleted = true
        }
        aTabSpace.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }

        // Step: Check archive existence
        var rASV: Message = checkArchiveExistence(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)
        if (rASV.first != MessageType.NoProblem) {
            tab.text = "No such Archive"
            tab.style = defaultBlackTabStyle.plus("-fx-background-color: yellow")
            aTabSpace.style = "-fx-background-color: yellow"
            return tab
        }

        rASV = checkArchiveVolume(packagedFilePaths)
        addMessageLabel(messageBox, rASV.first, rASV.second)
        when (rASV.first) {
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

            else -> {}
        }

        val titleFromFileName = packagedFilePaths.getCommonFileName()

        var theTable: TheTable? = null

        print("Make the table for $titleFromFileName\n")
        tab.text = "Table Making: $titleFromFileName"
        tab.style = defaultBlackTabStyle

        var noError = true

        val task = GlobalScope.launch {
            try {
                theTable = makeTheTable(packagedFilePaths, theWorkingDirectory)
            } catch (e: Exception) {
                noError = false
                print(e)
                Platform.runLater {
                    tab.text = "File Error"
                    tab.style = defaultWhiteTabStyle.plus("-fx-background-color: red;")
                    aTabSpace.style = "-fx-background-color: lightcoral;"
                    filePathArea.text = "File Error\n" + e.message
                }
            }
            if (noError) {
                Platform.runLater {
                    tab.text = "Analyzing: $titleFromFileName"
                }

                theTable!!.prepareWorkingDirectory()

                //theTable!!.printStatus()
                //theTable!!.printResult()

                var runCount = 1
                while (true) {
                    print("Phase #$runCount: $titleFromFileName\n")
                    Platform.runLater {
                        tab.text = "Phase #$runCount - $titleFromFileName"
                    }
                    try {
                        val res = theTable!!.runOnce()
                        if (res.first)
                            break
                        else
                            filePathArea.text = filePathArea.text + "\n" + res.second
                    } catch (e: ExtractionException) {
                        noError = false
                        print(e)
                        Platform.runLater {
                            tab.text = "ExtrError"
                            tab.style = defaultWhiteTabStyle.plus("-fx-background-color: red;")
                            aTabSpace.style = "-fx-background-color: lightcoral;"
                            filePathArea.text = "Extraction Error\n" + e.message
                        }
                        break
                    } catch (e: Exception) {
                        noError = false
                        print(e)
                        Platform.runLater {
                            tab.text = "Error"
                            tab.style = defaultWhiteTabStyle.plus("-fx-background-color: red;")
                            aTabSpace.style = "-fx-background-color: lightcoral;"
                            filePathArea.text = "Unknown Error\n" + e.message
                        }
                        break
                    }

                    //theTable!!.printStatus()
                    //theTable!!.printResult()

                    runCount++
                }

                val result = theTable!!.printFinalResult()
                val count = result.first
                val resultList = result.second

                if (count == 0) {
                    print("Have no different files in the ArchiveSets\n")
                    resultList.add(0, "Have no different files in the ArchiveSets")
                }

                val asNum = theTable!!.archiveSetNum
                val theResult = theTable!!.generateResultStringList()
                val theSameResult = theResult.filter { it[0] == "O" }
                val theDiffResult = theResult.filter { it[0] == "X" }
                // TODO: Hard coded index
                val noExedResult = theDiffResult.filter { it[3] != "E" }
                // TODO: IgnoredResult

                theTable!!.closeAllArchiveSets()
                theTable!!.removeAllArchiveSets()
                theTable = null

                if (noError) {
                    Platform.runLater {

                        val allTable = TableView<ObservableList<StringProperty>>()
                        val diffTable = TableView<ObservableList<StringProperty>>()
                        val sameTable = TableView<ObservableList<StringProperty>>()
                        val noExTable = TableView<ObservableList<StringProperty>>()

                        makeResultTable(allTable, theResult, asNum, listOf())
                        makeResultTable(diffTable, theDiffResult, asNum, listOf(7 + asNum))
                        makeResultTable(sameTable, theSameResult, asNum, listOf())
                        makeResultTable(noExTable, noExedResult, asNum, listOf(7 + asNum))
                        // TODO: IgnoredResult

                        var diffTab: Tab? = null
                        if (!(theSameResult.isEmpty() || theDiffResult.isEmpty()))
                            generateResultTab(resultTabPane, ResultType.All, allTable)
                        if (theSameResult.isNotEmpty())
                            resultTabPane.selectionModel.select(
                                generateResultTab(
                                    resultTabPane,
                                    ResultType.Same,
                                    sameTable
                                )
                            )
                        // Do not use `theDiffResult.isNotEmpty()`, Refer T144
                        if (noExedResult.isNotEmpty()) {
                            diffTab =
                                if (showExedBox.isSelected)
                                    generateResultTab(resultTabPane, ResultType.Diff, diffTable)
                                else
                                    generateResultTab(resultTabPane, ResultType.Diff, noExTable)
                            resultTabPane.selectionModel.select(diffTab)
                        }
                        // TODO: IgnoredResult

                        tab.text = (if (count == 0) "Done" else "Diff") + ": $titleFromFileName"
                        tab.style =
                            defaultWhiteTabStyle.plus("-fx-background-color: ")
                                .plus(if (count == 0) "green;" else "red;")
                        aTabSpace.style =
                            "-fx-background-color: ".plus(if (count == 0) "greenyellow;" else "lightcoral;")
                        if (count == 0)
                            addMessageLabel(messageBox, MessageType.NoProblem, "No\nProblem")
                        else
                            addMessageLabel(messageBox, MessageType.Critical, "Have\nDiff")

                        tabPane.selectionModel.select(tab)

                        if (diffTab != null) {
                            showExedBox.setOnMouseClicked {
                                diffTab.content = if (showExedBox.isSelected) diffTable else noExTable
                            }
                        }
                        showDirBox.setOnMouseClicked {
                            for (c in 1..asNum) {
                                allTable.columns[6 + asNum + 2 * c].isVisible = showDirBox.isSelected
                                if (sameTable.columns.size != 0)
                                    sameTable.columns[6 + asNum + 2 * c].isVisible = showDirBox.isSelected
                                if (diffTable.columns.size != 0)
                                    diffTable.columns[6 + asNum + 2 * c].isVisible = showDirBox.isSelected
                                if (noExTable.columns.size != 0)
                                    noExTable.columns[6 + asNum + 2 * c].isVisible = showDirBox.isSelected
                            }
                        }
                    }

                    var doesAllFileHaveSameName = true
                    if (theSameResult.isNotEmpty() && noExedResult.isEmpty()) {
                        for (aRow in theSameResult) {
                            if (aRow[6 + asNum + 3] != "====") {
                                doesAllFileHaveSameName = false
                                break
                            }
                        }
                        if (!doesAllFileHaveSameName) {
                            Platform.runLater {
                                addMessageLabel(messageBox, MessageType.Warning, "Same, but\nDifferent Name")
                                tab.style = defaultWhiteTabStyle.plus("-fx-background-color: ").plus("blue;")
                                aTabSpace.style = "-fx-background-color: ".plus("LightSkyBlue;")
                            }
                        }
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


    private fun generateCheckTab(tabPane: TabPane, checks: List<MultiArchiveVolumeInfo>): Tab {
        tabCount += 1

        // Interface
        val tab = Tab()
        tab.text = "CheckTab$tabCount"
        val fxml = javaClass.getResource("fxml/CheckTab.fxml")
        val aTabSpace: Pane = FXMLLoader.load(fxml)
        val filePathArea = aTabSpace.lookup("#FilePaths") as TextArea // FilePaths TextArea
        tab.content = aTabSpace

        filePathArea.text = ""
        // if every check is not missing nor corrupted, then show "Good"
        val noProblem = checks.all { !it.isMissing }
        if (noProblem) {
            filePathArea.text = "No missing nor corrupted volume"
            tab.text = "Good"
            tab.style = defaultBlackTabStyle.plus("-fx-background-color: green")
        } else {
            tab.text = "Bad"
            tab.style = defaultBlackTabStyle.plus("-fx-background-color: yellow")
            var aText = filePathArea.text
            for (aCheck in checks) {
                if (aCheck.isMissing) {
                    aText += "Missing: ${aCheck.commonPath}\n"
                    var missingText = ""
                    for (aMissingVolume in aCheck.missingVolumes) {
                        missingText += "  $aMissingVolume\n"
                    }
                    aText += missingText
                    var corruptedText = ""
                    for (aCorruptedVolume in aCheck.corruptedVolumes) {
                        corruptedText += "  $aCorruptedVolume\n"
                    }
                }
            }
            filePathArea.text = aText
        }
        filePathArea.font = Font.font(null, FontWeight.NORMAL, 14.0)
        return tab
    }


    private fun generateResultTab(
        resultTabPane: TabPane,
        resultType: ResultType,
        aTable: TableView<ObservableList<StringProperty>>
    ): Tab {
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
            tableView.isEditable = true
            tableView.placeholder = Label("Loading....")
            if (inputData.isNotEmpty()) {
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

    private fun createColumn(
        columnIndex: Int, columnTitle: String
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

    private fun addMessageLabel(mb: HBox, mt: MessageType, msg: String) {
        val messageLabel = Label(msg)
        messageLabel.style = when (mt) {
            MessageType.Critical -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: red;")
            MessageType.Bad -> defaultBlackMessageLabelStyle.plus("-fx-background-color: yellow;")
            MessageType.Warning -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: blue")
            MessageType.NoProblem -> defaultWhiteMessageLabelStyle.plus("-fx-background-color: green")
        }
        mb.children.add(0, messageLabel)
    }

    @DelicateCoroutinesApi
    private fun openMASGrouper(tabPane: TabPane, unpackagedFilePaths: List<Path>) {
        var packagedFilePaths: Array<ArchiveSetPaths>? = null


        val masgStage = Stage()
        masgStage.title = "MAS Grouper"
        masgStage.isAlwaysOnTop = true

        val fxml = javaClass.getResource("fxml/MASGrouper.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)

        masgStage.scene = scene
        masgStage.show()

        @Suppress("UNCHECKED_CAST")
        val candidateTable = root.lookup("#CandidateTableView") as TableView<GroupedFile>
        val groupingButton = root.lookup("#MakeGroupButton") as Button
        val goButton = root.lookup("#GoButton") as Button

        goButton.isDisable = true

        candidateTable.columns[1].style = "-fx-alignment: CENTER-RIGHT;"

        // filter unpackagedFilePaths which is isSingleOrFirstArchivePath
        val filteredFilePaths = unpackagedFilePaths.filter { it.isFirstOrSingleArchivePath() }

        // Grouping file path
        val groupedFilePaths: Map<Int, List<Path>> = groupingFilePaths(filteredFilePaths)

        goButton.isDisable = groupedFilePaths.size < 2

        val fileLists: Map<Int, List<GroupedFile>> = groupedFilePaths.mapValues { entry ->
            val key = entry.key
            val paths: List<GroupedFile> = entry.value.map {
                val anGroupingFile = GroupedFile(false, key, it)
                anGroupingFile.select.addListener { _, _, newVal ->
                    print("Set " + anGroupingFile.getPath() + " as '" + newVal + "'.\n")
                }
                anGroupingFile
            }
            paths
        }

        // Concat fileLists to fileList
        val fileList: List<GroupedFile> = fileLists.flatMap { it.value }

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

            val checks = mutableListOf<MultiArchiveVolumeInfo>()
            if (groupIDSet.size > 1) {
                for (i in 0 until groupIDSet.size) {
                    val unpackagedPathList = mutableListOf<RealPath>()
                    for (anItem in candidateTable.items) {
                        if (anItem.getGroupID() == i)
                            unpackagedPathList.add(anItem.getPath())
                    }
                    val (packaged, check) = packageFilePathsForGrouped(unpackagedPathList)
                    packagedFilePathList.add(packaged)
                    checks.addAll(check)
                }
                packagedFilePaths = packagedFilePathList.toTypedArray()
            }
            // TODO: Add popup for missing/corrupted files
            // if any info of checks is missing, then show the popup
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
            for (idx in groupIDSet) {
                if (newID < idx) break
                newID++
            }
            for (anItem in candidateTable.items) {
                if (anItem.isSelected)
                    anItem.setGroupID(newID)
                // TODO: [BUG] This does not update GUI represetation
                anItem.isSelected = false
            }

            goButton.isDisable = groupIDSet.size == 0

            // TODO: Too bad solution, but works...
            candidateTable.refresh()
        }
    }

    @DelicateCoroutinesApi
    override fun start(primaryStage: Stage) {
        primaryStage.title = "EntryPoint"
        primaryStage.isAlwaysOnTop = true
        val fxml = javaClass.getResource("fxml/EntryPoint.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)

        val epPane = root.lookup("#EPPane") as AnchorPane // Entry Point Pane
        val dropPane = root.lookup("#DropPane") as AnchorPane // Drop Pane
        val tabPane = root.lookup("#TabPane") as TabPane // Tab Pane
        val checkDropPoint = root.lookup("#ForCheck") as Rectangle // Single-ArchiveSet drop point
        val singleDropPoint = root.lookup("#ForSingle") as Rectangle // Single-ArchiveSet drop point
        val multiDropPoint = root.lookup("#ForMulti") as Rectangle // Multi-ArchiveSet drop point
        val closeSameOnlyButton = root.lookup("#CloseSameOnlyButton") as Button
        val closeAllButton = root.lookup("#CloseAllButton") as Button

        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS // or SELECTED_TAB, UNAVAILABLE

        checkDropPoint.heightProperty().bind(epPane.heightProperty().divide(32).multiply(4))

        singleDropPoint.heightProperty().bind(epPane.heightProperty().divide(32).multiply(15))

        multiDropPoint.yProperty().bind(singleDropPoint.yProperty().add(dropPane.heightProperty().divide(2)))
        multiDropPoint.heightProperty().bind(epPane.heightProperty().divide(32).multiply(11))

        primaryStage.scene = scene
        primaryStage.show()

        val checkColor = checkDropPoint.fill
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
            val (packagedFilePaths, checks) = packageFilePathsWithoutGuide(db.files.map { it.toString() })
            // TODO: Add popup for missing/corrupted files
            val newAnalyzeTab = generateAnalyzeTab(tabPane, packagedFilePaths)
            event.consume()

            tabPane.tabs.add(newAnalyzeTab)
            tabPane.selectionModel.select(newAnalyzeTab)
            event.isDropCompleted = true
        }

        checkDropPoint.onDragEntered = EventHandler { event ->
            checkDropPoint.fill = selectedColor
            event.consume()
        }
        checkDropPoint.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }
        checkDropPoint.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            val (packagedFilePaths, checks) = packageFilePathsWithoutGuide(db.files.map { it.toString() })
            // TODO: Add popup for missing/corrupted files
            val newCheckTab = generateCheckTab(tabPane, checks)
            event.consume()

            tabPane.tabs.add(newCheckTab)
            tabPane.selectionModel.select(newCheckTab)
            event.isDropCompleted = true
        }
        checkDropPoint.onDragExited = EventHandler { event ->
            checkDropPoint.fill = checkColor
            event.consume()
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
            val (packagedFilePaths, checks) = packageFilePathsWithoutGuide(db.files.map { it.toString() })
            // TODO: Add popup for missing/corrupted files
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
            val unpackagedFilePaths = db.files.map { it.toString() }
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

        closeSameOnlyButton.setOnAction {
            val tabList = mutableListOf<Tab>()
            tabPane.tabs.forEach { if (it.style.endsWith("green;") || it.style.endsWith("blue;")) tabList.add(it) }
            tabList.reverse()
            for (aTab in tabList) {
                tabPane.tabs.remove(aTab)
            }
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
