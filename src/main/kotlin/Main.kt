import javafx.application.Application

import archive.*


fun main(args : Array<String>) {
    println("ArchiveDiffer-Kotlin")
    if (!jBindingChecker()) error("Fail to initialize 7Zip-JBinding")

    try {
        initialize(theIgnoringListPath)
    } catch (e: Exception) {
        println("Fail to load IgnoringList")
        return
    }

    Application.launch(GUI().javaClass, *args)
}
