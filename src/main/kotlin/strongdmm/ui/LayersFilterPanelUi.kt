package strongdmm.ui

import gnu.trove.set.hash.TLongHashSet
import imgui.ImBool
import imgui.ImGui.*
import imgui.ImString
import imgui.enums.ImGuiCol
import imgui.enums.ImGuiMouseCursor
import imgui.enums.ImGuiTreeNodeFlags
import imgui.enums.ImGuiWindowFlags
import strongdmm.byond.TYPE_AREA
import strongdmm.byond.TYPE_MOB
import strongdmm.byond.TYPE_OBJ
import strongdmm.byond.TYPE_TURF
import strongdmm.byond.dme.Dme
import strongdmm.byond.dme.DmeItem
import strongdmm.event.DmeItemType
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.type.Reaction
import strongdmm.event.type.controller.TriggerEnvironmentController
import strongdmm.event.type.controller.TriggerLayersFilterController
import strongdmm.event.type.ui.TriggerLayersFilterPanelUi
import strongdmm.util.imgui.GREEN32
import strongdmm.util.imgui.RED32
import strongdmm.util.imgui.child
import strongdmm.util.imgui.window
import strongdmm.window.AppWindow

class LayersFilterPanelUi : EventConsumer, EventSender {
    companion object {
        private const val MIN_FILTER_CHARS: Int = 1
    }

    private val isOpened: ImBool = ImBool(false)

    private var currentEnv: Dme? = null
    private val filteredTypesId: TLongHashSet = TLongHashSet()

    private val typesFilter: ImString = ImString(50)

    init {
        consumeEvent(TriggerLayersFilterPanelUi.Open::class.java, ::handleOpen)
        consumeEvent(Reaction.EnvironmentReset::class.java, ::handleEnvironmentReset)
        consumeEvent(Reaction.EnvironmentChanged::class.java, ::handleEnvironmentChanged)
        consumeEvent(Reaction.LayersFilterRefreshed::class.java, ::handleLayersFilterRefreshed)
    }

    fun process() {
        if (!isOpened.get()) {
            return
        }

        setNextWindowPos(AppWindow.windowWidth / 2 - 200f, AppWindow.windowHeight / 2 - 225f, AppWindow.defaultWindowCond)
        setNextWindowSize(400f, 450f, AppWindow.defaultWindowCond)

        window("Layers Filter", isOpened) {
            if (currentEnv == null) {
                text("No types to filter")
                return@window
            }

            setNextItemWidth(-1f)
            strongdmm.util.imgui.inputText("##types_filter", typesFilter, "Types Filter", "Provide at least $MIN_FILTER_CHARS char to apply")

            separator()

            child("tree_nodes", imGuiWindowFlags = ImGuiWindowFlags.HorizontalScrollbar) {
                createTreeNodes(currentEnv!!.getItem(TYPE_AREA)!!)
                createTreeNodes(currentEnv!!.getItem(TYPE_TURF)!!)
                createTreeNodes(currentEnv!!.getItem(TYPE_OBJ)!!)
                createTreeNodes(currentEnv!!.getItem(TYPE_MOB)!!)
            }
        }
    }

    private fun createTreeNodes(dmeItem: DmeItem) {
        if (typesFilter.length >= MIN_FILTER_CHARS) {
            if (dmeItem.type.contains(typesFilter.get())) {
                createToggleButton(dmeItem)
                sameLine()
                treeNodeEx(dmeItem.type, ImGuiTreeNodeFlags.Leaf or ImGuiTreeNodeFlags.NoTreePushOnOpen)
            }

            dmeItem.children.forEach { child ->
                createTreeNodes(currentEnv!!.getItem(child)!!)
            }
        } else {
            createToggleButton(dmeItem)
            sameLine()

            if (dmeItem.children.isEmpty()) {
                treeNodeEx(dmeItem.type, ImGuiTreeNodeFlags.Leaf or ImGuiTreeNodeFlags.NoTreePushOnOpen)
            } else if (treeNode(dmeItem.type)) {
                dmeItem.children.forEach { child ->
                    createTreeNodes(currentEnv!!.getItem(child)!!)
                }

                treePop()
            }
        }
    }

    private fun createToggleButton(dmeItem: DmeItem) {
        val isFilteredType = filteredTypesId.contains(dmeItem.id)
        val buttonColor = if (isFilteredType) RED32 else GREEN32

        pushStyleColor(ImGuiCol.Button, buttonColor)
        pushStyleColor(ImGuiCol.ButtonActive, buttonColor)
        pushStyleColor(ImGuiCol.ButtonHovered, buttonColor)

        if (smallButton(" ##layer_filter_${dmeItem.id}")) {
            toggleItemFilter(dmeItem, isFilteredType)
            sendEvent(TriggerLayersFilterController.FilterLayersById(filteredTypesId.toArray()))
        }

        if (isItemHovered()) {
            setMouseCursor(ImGuiMouseCursor.Hand)
        }

        popStyleColor(3)
    }

    private fun toggleItemFilter(dmeItem: DmeItem, isFilteredType: Boolean) {
        if (isFilteredType) {
            filteredTypesId.remove(dmeItem.id)
            dmeItem.children.forEach {
                val item = currentEnv!!.getItem(it)!!
                filteredTypesId.remove(item.id)
                toggleItemFilter(item, isFilteredType)
            }
        } else {
            filteredTypesId.add(dmeItem.id)
            dmeItem.children.forEach {
                val item = currentEnv!!.getItem(it)!!
                filteredTypesId.add(item.id)
                toggleItemFilter(item, isFilteredType)
            }
        }
    }

    private fun handleOpen() {
        isOpened.set(true)
    }

    private fun handleEnvironmentReset() {
        currentEnv = null
        filteredTypesId.clear()
    }

    private fun handleEnvironmentChanged(event: Event<Dme, Unit>) {
        currentEnv = event.body
    }

    private fun handleLayersFilterRefreshed(event: Event<Set<DmeItemType>, Unit>) {
        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment {
            filteredTypesId.clear()
            it.items.values.forEach { dmeItem ->
                if (event.body.contains(dmeItem.type)) {
                    filteredTypesId.add(dmeItem.id)
                }
            }
        })
    }
}
