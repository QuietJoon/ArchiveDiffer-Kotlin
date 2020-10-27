import java.io.File

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.input.TransferMode
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

import archive.*
import util.*


fun main(args : Array<String>) {
    println("StudyKotlin-JBinding")
    if (jBindingChecker()) Application.launch(GUIListing().javaClass, *args)
    println("End")
}

data class ArchiveSetPathAnalyzedLFEAS (
    val paths : String
    , val colorName: String
    , val firstOrSinglePaths: Array<String>)

fun pathAnalyzeLFEAS(files: List<File>): ArchiveSetPathAnalyzedLFEAS {
    val paths = generateStringFromFileList(files)
    var colorName = if (files.size == 1) "Yellow" else "Green"
    val pathArray = files.map{it.toString()}.toTypedArray()
    val firstOrSinglePaths = getFirstOrSingleArchivePaths(pathArray)
    lateinit var anANS: ArchiveAndStream

    for ( aPath in firstOrSinglePaths ) {
        try {
            println("<firstPhase>: opening $aPath")
            anANS = openArchive(aPath) ?: error("[Error]<FirstPhase>: Fail ot open")
        } catch (e: Exception) {
            println("[Error]<FirstPhase>: Seems to fail opening")
            colorName = "Red"
        }
        printItemList(anANS.inArchive)
        anANS.close()
    }

    return ArchiveSetPathAnalyzedLFEAS (paths, colorName, firstOrSinglePaths)
}

class GUIListing : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Study 7-Zip JBinding! - Listing items foreach archive set"
        val fxml = javaClass.getResource("fxml/Main.fxml")
        val root: Parent = FXMLLoader.load(fxml)
        val scene = Scene(root)
        val filePathsLabel = root.lookup("#FilePathsLabel") as Label
        val statusIndicator = root.lookup("#StatusIndicator") as Rectangle

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
                var filePath: String?

                for (file in db.files) { db.files
                    filePath = file.absolutePath
                    println(filePath)
                }

                val firstResult = pathAnalyzeLFEAS(db.files)

                filePathsLabel.text = firstResult.paths
                statusIndicator.fill = Paint.valueOf(firstResult.colorName)

                println("End a phase")
            } else {
                filePathsLabel.text = "No File"
                statusIndicator.fill = Paint.valueOf("Red")
            }
            event.isDropCompleted = success
            event.consume()
        }
        primaryStage.scene = scene
        primaryStage.show()
    }
}
