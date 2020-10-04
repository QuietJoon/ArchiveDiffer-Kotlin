package util

import com.xenomachina.argparser.ArgParser

class Config(parser: ArgParser) {
    val aWorkingDirectory by parser.storing("-D", help = "Directory for extracting archives")
    val aIgnoringListPath by parser.storing("-I", help = "Path to ignoring list")
    val aSZJBPath by parser.storing("-S", help = "Path to SevenZip-JBinding lib path")
    val aState by parser.storing("-X", help = "Are you developing now?")
}
