package strongdmm.ui

import imgui.ImGui.*
import imgui.ImString
import imgui.enums.ImGuiWindowFlags
import org.lwjgl.glfw.GLFW.*
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.type.Provider
import strongdmm.event.type.Reaction
import strongdmm.event.type.controller.TriggerMapHolderController
import strongdmm.event.type.ui.TriggerAvailableMapsDialogUi
import strongdmm.util.imgui.*
import strongdmm.window.AppWindow
import java.io.File

class AvailableMapsDialogUi : EventSender, EventConsumer {
    private var isDoOpen: Boolean = false
    private var isFirstOpen: Boolean = true

    private var providedAvailableMaps: Set<Pair<String, String>> = emptySet()
    private var selectedMapPath: String? = null // to store an absolute path for currently selected map
    private var selectionStatus: String = "" // to display a currently selected map (visible path)

    private val mapFilter: ImString = ImString().apply { inputData.isResizable = true }

    init {
        consumeEvent(TriggerAvailableMapsDialogUi.Open::class.java, ::handleOpen)
        consumeEvent(Provider.MapHolderControllerAvailableMaps::class.java, ::handleProviderMapHolderControllerAvailableMaps)
    }

    fun process() {
        if (isDoOpen) {
            openPopup("Available Maps")
            isDoOpen = false
        }

        setNextWindowSize(600f, 285f, AppWindow.defaultWindowCond)

        popupModal("Available Maps") {
            text("Selected: $selectionStatus")
            setNextItemWidth(getWindowWidth() - 20)

            if (isFirstOpen) {
                setKeyboardFocusHere()
                isFirstOpen = false
            }

            inputText("##maps_path_filter", mapFilter, "Paths Filter")

            child("available_maps_list", getWindowWidth() - 20, getWindowHeight() - 100, true, ImGuiWindowFlags.HorizontalScrollbar) {
                for ((absoluteFilePath, visibleFilePath) in providedAvailableMaps) {
                    if (mapFilter.length > 0 && !visibleFilePath.contains(mapFilter.get(), ignoreCase = true)) {
                        continue
                    }

                    bullet()
                    sameLine()
                    selectable(visibleFilePath, selectedMapPath == absoluteFilePath) {
                        selectedMapPath = absoluteFilePath
                        selectionStatus = visibleFilePath
                    }
                }
            }

            button("Open", block = ::openSelectedMapAndClosePopup)
            sameLine()
            button("Cancel", block = ::closePopup)

            if (isKeyPressed(GLFW_KEY_ENTER) || isKeyPressed(GLFW_KEY_KP_ENTER)) {
                openSelectedMapAndClosePopup()
            } else if (isKeyPressed(GLFW_KEY_ESCAPE)) {
                closePopup()
            }
        }
    }

    private fun openSelectedMapAndClosePopup() {
        selectedMapPath?.let {
            sendEvent(TriggerMapHolderController.OpenMap(File(it)))
            closePopup()
        }
    }

    private fun closePopup() {
        closeCurrentPopup()
        selectedMapPath = null
        selectionStatus = ""
        sendEvent(Reaction.ApplicationBlockChanged(false))
    }

    private fun handleOpen() {
        isDoOpen = true
        isFirstOpen = true
        sendEvent(Reaction.ApplicationBlockChanged(true))
    }

    private fun handleProviderMapHolderControllerAvailableMaps(event: Event<Set<Pair<String, String>>, Unit>) {
        providedAvailableMaps = event.body
    }
}
