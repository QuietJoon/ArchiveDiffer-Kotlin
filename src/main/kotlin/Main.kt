import javafx.application.Application
import com.xenomachina.argparser.ArgParser

import archive.*
import util.*

fun main(args: Array<String>) {
    println("ArchiveDiffer-Kotlin")

    ArgParser(args).parseInto(::Config).run {
        try {
            initialize(aIgnoringListPath)
        } catch (e: Exception) {
            println("Fail to load IgnoringList")
            return
        }
        if (!jBindingChecker((aState == "Dev"), aSZJBPath)) error("Fail to initialize 7Zip-JBinding")
        theWorkingDirectory = aWorkingDirectory
        Application.launch(EntryPoint().javaClass, *args)
        jBindingClear(aSZJBPath)
    }
}

fun initialize(ignoringListConfigPath: RealPath) {
    // Set IgnoringList
    theIgnoringList = readIgnoringList(ignoringListConfigPath)
}
