package util

import com.xenomachina.argparser.ArgParser

class Config(parser: ArgParser) {
    val aWorkingDirectory by parser.storing("-D", help = "Directory for extracting archives")
    val aIgnoringListPath by parser.storing("-I", help = "Path to ignoring list")
}
