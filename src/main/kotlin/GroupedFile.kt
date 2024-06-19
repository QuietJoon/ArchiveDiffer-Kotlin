import javafx.beans.property.*


class GroupedFile(isSelected: Boolean, groupID: GroupID, path: RealPath) {
    val select = SimpleBooleanProperty()
    private val path = SimpleStringProperty()
    private val groupID = SimpleIntegerProperty()

    var isSelected: Boolean
        get() = select.get()
        set(on) = select.set(on)

    init {
        setGroupID(groupID)
        setPath(path)
        this.isSelected = isSelected
    }

    private fun pathProperty(): StringProperty = this.path

    fun getPath(): String = this.pathProperty().get()

    fun setPath(path: String) = this.pathProperty().set(path)

    private fun groupIDProperty(): IntegerProperty = this.groupID

    fun getGroupID(): GroupID = this.groupIDProperty().get()

    fun setGroupID(groupID: GroupID) = this.groupIDProperty().set(groupID)

    override fun toString(): String = getGroupID().toString() + getPath()
}

typealias GroupID = Int
