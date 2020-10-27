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
    if (jBindingChecker()) Application.launch(GUIListingLNAS().javaClass, *args)
    println("End")
}

data class ArchiveSetPathAnalyzedLNAS (
    val paths : String
    , val colorName: String
    , val firstOrSinglePaths: Array<String>)

fun pathAnalyzeLNAS(files: List<File>): ArchiveSetPathAnalyzedLNAS {
    val paths = generateStringFromFileList(files)
    var colorName = if (files.size == 1) "Yellow" else "Green"
    val pathArray = files.map{it.toString()}.toTypedArray()
    val firstOrSinglePaths = getFirstOrSingleArchivePaths(pathArray)

    return ArchiveSetPathAnalyzedLNAS (paths, colorName, firstOrSinglePaths)
}

class GUIListingLNAS : Application() {

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

                val firstResult = pathAnalyzeLNAS(db.files)

                filePathsLabel.text = firstResult.paths
                statusIndicator.fill = Paint.valueOf(firstResult.colorName)

                for ( aPath in firstResult.firstOrSinglePaths) {
                    val ans = openArchive(aPath) ?: error("[Error]<GUIListingLNAS>: Fail to open")
                    printItemListByIDs(ans.inArchive, getNestedArchivesIDArray(ans.inArchive))
                    ans.close()
                }
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
