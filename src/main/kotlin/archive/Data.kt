package archive

class ExtractionException : Exception {

    constructor(msg: String) : super(msg) {}

    constructor(msg: String, e: Exception) : super(msg, e) {}

    companion object {
        private const val serialVersionUID = -5108931481040742838L
    }
}
