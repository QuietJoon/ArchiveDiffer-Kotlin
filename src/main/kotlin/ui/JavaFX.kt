package ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.event.Event
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent

import GroupedFile


class CheckBoxColumn : TableColumn<GroupedFile, Boolean>() {
    init {
        this.cellValueFactory = PropertyValueFactory<GroupedFile, Boolean>("select")

        this.setCellFactory { column ->
            val cell = CheckBoxTableCell<GroupedFile, Boolean> { index ->
                val selected = SimpleBooleanProperty(
                    this.tableView.items[index].isSelected
                )
                selected.addListener { _, _, n ->
                    this.tableView.items[index].isSelected = n
                    this.tableView.selectionModel.select(index)

                    Event.fireEvent(
                        column.tableView, MouseEvent(
                            MouseEvent.MOUSE_CLICKED, 0.0, 0.0, 0.0, 0.0,
                            MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null
                        )
                    )
                }
                selected
            }
            cell
        }
    }
}
