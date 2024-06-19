data class Leveled<T>(val level: Level, val datum: T)

enum class Level { SURE, SUSPICIOUS, IGNORE, NOTYET }

fun Level.toShortString() = when (this) {
    Level.SURE -> "SR"
    Level.SUSPICIOUS -> "SS"
    Level.IGNORE -> "IG"
    Level.NOTYET -> "NY"
}

enum class MessageType { NoProblem, Warning, Bad, Critical }

fun MessageType.toShortString() = when (this) {
    MessageType.NoProblem -> "NP"
    MessageType.Warning -> "WN"
    MessageType.Bad -> "BD"
    MessageType.Critical -> "CR"
}

enum class ResultType { All, Same, Diff, Ignored }
