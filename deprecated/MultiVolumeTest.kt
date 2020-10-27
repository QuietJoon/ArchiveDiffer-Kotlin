import archive.*


fun main(args : Array<String>) {
    if (!jBindingChecker()) return

    println("Multi-volume RAR Test")
    openArchive("R:\\TestArchives\\MultiVolume.part1.rar")
    openArchive("R:\\TestArchives\\MissingMultiVolume.part1.rar")
    println("End")
}
