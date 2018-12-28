import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.input.TransferMode
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import kotlinx.coroutines.*

import util.filePathAnalyze


class GUI : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ArchiveDiffer-Kotlin"
        primaryStage.isAlwaysOnTop = true
        val fxml = javaClass.getResource("fxml/Main.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)
        val fileIndicator = root.lookup("#FileIndicator1") as Rectangle // For number of proper input file
        val filePathsLabel = root.lookup("#FilePathsLabel1") as Label // Name of input file
        val statusIndicator = root.lookup("#StatusIndicator1") as Rectangle // Show progress
        val differencesLabel = root.lookup("#DifferencesLabel1") as TextArea
        val analyzedIndicator = root.lookup("#AnalyzedIndicator1") as Rectangle // Show final result
        var fileSwitch = true

        scene.onDragOver = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY)
            } else {
                event.consume()
            }
        }

        scene.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            var success = false
            if (db.hasFiles()) {
                success = true

                statusIndicator.fill = Paint.valueOf("Black")
                analyzedIndicator.fill = Paint.valueOf("GRAY")

                val filePaths = filePathAnalyze(db.files)

                filePathsLabel.text = filePaths.joinToString(separator = "\n")
                GlobalScope.launch {
                    for ( i in 0..10 ) {
                        fileIndicator.fill = Paint.valueOf(if (fileSwitch) "Blue" else "White")
                        fileSwitch = !fileSwitch
                        delay(100L)
                    }
                }

                println("Make the table")
                var theTable: TheTable? = null
                var doesTheTableExist = false
                GlobalScope.launch {
                    theTable = makeTheTable(filePaths, theDebugDirectory)
                    doesTheTableExist = true
                }

                var isJobFinished = false
                differencesLabel.text = "Start Analyzing"
                GlobalScope.launch {
                    while ( !doesTheTableExist ) {
                        statusIndicator.fill = Paint.valueOf("Gray")
                        delay(19L)
                    }

                    theTable!!.prepareWorkingDirectory()

                    printStatus(theTable!!)

                    printResult(theTable!!)

                    var runCount = 1
                    while (true) {
                        println("Phase #$runCount")
                        if (theTable!!.runOnce()) break

                        printStatus(theTable!!)

                        printResult(theTable!!)

                        runCount++
                    }

                    val result = printFinalResult(theTable!!)
                    val count = result.first
                    val resultList = result.second

                    if (count == 0) {
                        println("Have no different files in the ArchiveSets")
                        resultList.add("Have no different files in the ArchiveSets")
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
        primaryStage.scene = scene
        primaryStage.show()
    }
}
