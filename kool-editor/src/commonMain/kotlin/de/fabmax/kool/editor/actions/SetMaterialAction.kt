package de.fabmax.kool.editor.actions

import de.fabmax.kool.editor.components.MaterialComponent
import de.fabmax.kool.editor.data.MaterialData
import de.fabmax.kool.editor.model.EditorNodeModel

class SetMaterialAction(
    val nodeModel: EditorNodeModel,
    val materialComponent: MaterialComponent,
    val setMaterialModel: MaterialData?,
) : EditorAction {

    private val prevMaterial = materialComponent.materialData

    override fun apply() {
        materialComponent.materialState.set(setMaterialModel)
    }

    override fun undo() {
        materialComponent.materialState.set(prevMaterial)
    }
}