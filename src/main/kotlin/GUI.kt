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
        val fileIndicator = root.lookup("#FileIndicator") as Rectangle // For number of proper input file
        val filePathsLabel = root.lookup("#FilePathsLabel") as Label // Name of input file
        val statusIndicator = root.lookup("#StatusIndicator") as Rectangle // Show progress
        val differencesLabel = root.lookup("#DifferencesLabel") as TextArea
        val analyzedIndicator = root.lookup("#AnalyzedIndicator") as Rectangle // Show final result
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

                    for ( anArchiveSet in theTable!!.theArchiveSets)
                        printItemList(anArchiveSet, anArchiveSet.getThisIDs())

                    for ( anItemRecord in theTable!!.theItemTable ) {
                        print(anItemRecord.key.toString())
                        println(anItemRecord.value.toString())
                    }

                    println("Difference only")
                    var count = 0
                    var resultList = mutableListOf<String>()
                    for (anItemEntry in theTable!!.theItemTable) {
                        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
                            count++
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(anItemEntry.key.toString())
                            stringBuilder.append(anItemEntry.value.toString())
                            val theString = stringBuilder.toString()
                            resultList.add(theString)
                            println(theString)
                        }
                    }
                    println("Same")
                    resultList.add("--------------------------------    Same    --------------------------------")
                    for (anItemEntry in theTable!!.theItemTable) {
                        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(anItemEntry.key.toString())
                            stringBuilder.append(anItemEntry.value.toString())
                            val theString = stringBuilder.toString()
                            resultList.add(theString)
                            println(theString)
                        }
                    }

                    var runCount = 1
                    while (true) {
                        println("Phase #$runCount")
                        if (theTable!!.runOnce()) break

                        for (anArchiveSet in theTable!!.theArchiveSets)
                            printItemList(anArchiveSet, anArchiveSet.getThisIDs())

                        for (anItemRecord in theTable!!.theItemTable) {
                            print(anItemRecord.key.toString())
                            println(anItemRecord.value.toString())
                        }

                        println("Difference only")
                        count = 0
                        resultList = mutableListOf()
                        for (anItemEntry in theTable!!.theItemTable) {
                            if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
                                count++
                                val stringBuilder = StringBuilder()
                                stringBuilder.append(anItemEntry.key.toString())
                                stringBuilder.append(anItemEntry.value.toString())
                                val theString = stringBuilder.toString()
                                resultList.add(theString)
                                println(theString)
                            }
                        }
                        println("Same")
                        resultList.add("--------------------------------    Same    --------------------------------")
                        for (anItemEntry in theTable!!.theItemTable) {
                            if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
                                val stringBuilder = StringBuilder()
                                stringBuilder.append(anItemEntry.key.toString())
                                stringBuilder.append(anItemEntry.value.toString())
                                val theString = stringBuilder.toString()
                                resultList.add(theString)
                                println(theString)
                            }
                        }
                        runCount++
                    }

                    if (count == 0) {
                        println("Have no different files in the ArchiveSets")
                        resultList.add("Have no different files in the ArchiveSets")
                    }

                    statusIndicator.fill = Paint.valueOf("Green")
                    analyzedIndicator.fill = Paint.valueOf(if (count == 0) "Green" else "Red")

                    println("Difference only")
                    resultList = mutableListOf()
                    for (anItemEntry in theTable!!.theItemTable) {
                        if (!anItemEntry.value.isFilled && !anItemEntry.value.isExtracted) {
                            count++
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(anItemEntry.key.toString())
                            stringBuilder.append(anItemEntry.value.simpleString(theTable!!.theItemList))
                            val theString = stringBuilder.toString()
                            resultList.add(theString)
                            println(theString)
                        }
                    }
                    println("Same")
                    resultList.add("--------------------------------    Same    --------------------------------")
                    for (anItemEntry in theTable!!.theItemTable) {
                        if (anItemEntry.value.isFilled || anItemEntry.value.isExtracted) {
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(anItemEntry.key.toString())
                            stringBuilder.append(anItemEntry.value.simpleString(theTable!!.theItemList))
                            val theString = stringBuilder.toString()
                            resultList.add(theString)
                            println(theString)
                        }
                    }

                    isJobFinished = true
                    delay(17L)

                    differencesLabel.text = resultList.joinToString(separator = "\n")

                    theTable!!.closeAllArchiveSets()
                    theTable!!.removeAllArchiveSets()
                }

                GlobalScope.launch {
                    while (!isJobFinished) {
                        if (doesTheTableExist)
                            differencesLabel.text = "Now analyzing"
                        delay(31L)
                    }
                }

                println("End a phase")
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
