package strongdmm.ui

import imgui.ImBool
import imgui.ImGui
import imgui.ImGui.separator
import imgui.ImGui.text
import org.lwjgl.glfw.GLFW
import strongdmm.byond.TYPE_AREA
import strongdmm.byond.TYPE_MOB
import strongdmm.byond.TYPE_OBJ
import strongdmm.byond.TYPE_TURF
import strongdmm.byond.dmm.MapPath
import strongdmm.controller.action.ActionStatus
import strongdmm.controller.shortcut.Shortcut
import strongdmm.controller.shortcut.ShortcutHandler
import strongdmm.event.DmeItemType
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.type.Provider
import strongdmm.event.type.Reaction
import strongdmm.event.type.controller.*
import strongdmm.event.type.ui.*
import strongdmm.util.NfdUtil
import strongdmm.util.imgui.mainMenuBar
import strongdmm.util.imgui.menu
import strongdmm.util.imgui.menuItem
import strongdmm.window.AppWindow
import java.io.File

class MenuBarUi : EventSender, EventConsumer, ShortcutHandler() {
    private var progressText: String? = null
    private var isEnvironmentOpened: Boolean = false
    private var isMapOpened: Boolean = false

    private var isUndoEnabled: Boolean = false
    private var isRedoEnabled: Boolean = false

    private val isAreaLayerActive: ImBool = ImBool(true)
    private val isTurfLayerActive: ImBool = ImBool(true)
    private val isObjLayerActive: ImBool = ImBool(true)
    private val isMobLayerActive: ImBool = ImBool(true)

    private lateinit var providedShowInstanceLocator: ImBool
    private lateinit var providedFrameAreas: ImBool
    private lateinit var providedRecentEnvironments: List<String>
    private lateinit var providedRecentMaps: List<MapPath>

    init {
        consumeEvent(Reaction.EnvironmentLoading::class.java, ::handleEnvironmentLoading)
        consumeEvent(Reaction.EnvironmentLoaded::class.java, ::handleEnvironmentLoaded)
        consumeEvent(Reaction.EnvironmentChanged::class.java, ::handleEnvironmentChanged)
        consumeEvent(Reaction.EnvironmentReset::class.java, ::handleEnvironmentReset)
        consumeEvent(Reaction.SelectedMapChanged::class.java, ::handleSelectedMapChanged)
        consumeEvent(Reaction.SelectedMapClosed::class.java, ::handleSelectedMapClosed)
        consumeEvent(Reaction.ActionStatusChanged::class.java, ::handleActionStatusChanged)
        consumeEvent(Reaction.LayersFilterRefreshed::class.java, ::handleLayersFilterRefreshed)

        consumeEvent(Provider.InstanceLocatorPanelUiOpen::class.java, ::handleProviderInstanceLocatorPanelUiOpen)
        consumeEvent(Provider.CanvasControllerFrameAreas::class.java, ::handleProviderCanvasControllerFrameAreas)
        consumeEvent(Provider.RecentFilesControllerRecentEnvironments::class.java, ::handleProviderRecentFilesControllerRecentEnvironments)
        consumeEvent(Provider.RecentFilesControllerRecentMaps::class.java, ::handleProviderRecentFilesControllerRecentMaps)

        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_N, action = ::doNewMap)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_O, action = ::doOpenMap)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_O, action = ::doOpenAvailableMap)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_W, action = ::doCloseMap)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_W, action = ::doCloseAllMaps)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_S, action = ::doSave)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_S, action = ::doSaveAll)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_Q, action = ::doExit)

        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_Z, action = ::doUndo)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_Z, action = ::doRedo)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_X, action = ::doCut)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_C, action = ::doCopy)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_V, action = ::doPaste)
        addShortcut(GLFW.GLFW_KEY_DELETE, action = ::doDelete)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_D, action = ::doDeselectAll)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_F, action = ::doFindInstance)

        // "Manual" methods since toggle through the buttons switches ImBool status vars automatically.
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_1, action = ::toggleAreaLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_2, action = ::toggleTurfLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_3, action = ::toggleObjLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_4, action = ::toggleMobLayerManual)
    }

    fun process() {
        mainMenuBar {
            menu("File") {
                menuItem("Open Environment...", enabled = progressText == null, block = ::doOpenEnvironment)
                menu("Recent Environments", enabled = progressText == null) {
                    showRecentEnvironments()
                }
                separator()
                menuItem("New Map...", shortcut = "Ctrl+N", enabled = isEnvironmentOpened, block = ::doNewMap)
                menuItem("Open Map...", shortcut = "Ctrl+O", enabled = isEnvironmentOpened, block = ::doOpenMap)
                menuItem("Open Available Map", shortcut = "Ctrl+Shift+O", enabled = isEnvironmentOpened, block = ::doOpenAvailableMap)
                menu("Recent Maps", enabled = isEnvironmentOpened) { showRecentMaps() }
                separator()
                menuItem("Close Map", shortcut = "Ctrl+W", enabled = isMapOpened, block = ::doCloseMap)
                menuItem("Close All Maps", shortcut = "Ctrl+Shift+W", enabled = isMapOpened, block = ::doCloseAllMaps)
                separator()
                menuItem("Save", shortcut = "Ctrl+S", enabled = isMapOpened, block = ::doSave)
                menuItem("Save All", shortcut = "Ctrl+Shift+S", enabled = isMapOpened, block = ::doSaveAll)
                menuItem("Save As...", enabled = isMapOpened, block = ::doSaveAs)
                separator()
                menuItem("Exit", shortcut = "Ctrl+Q", block = ::doExit)
            }

            menu("Edit") {
                menuItem("Undo", shortcut = "Ctrl+Z", enabled = isUndoEnabled, block = ::doUndo)
                menuItem("Redo", shortcut = "Ctrl+Shift+Z", enabled = isRedoEnabled, block = ::doRedo)
                separator()
                menuItem("Cut", shortcut = "Ctrl+X", enabled = isMapOpened, block = ::doCut)
                menuItem("Copy", shortcut = "Ctrl+C", enabled = isMapOpened, block = ::doCopy)
                menuItem("Paste", shortcut = "Ctrl+V", enabled = isMapOpened, block = ::doPaste)
                menuItem("Delete", shortcut = "Delete", enabled = isMapOpened, block = ::doDelete)
                menuItem("Deselect All", shortcut = "Ctrl+D", block = ::doDeselectAll)
                separator()
                menuItem("Set Map Size...", enabled = isMapOpened, block = ::doSetMapSize)
                menuItem("Find Instance...", shortcut = "Ctrl+F", block = ::doFindInstance)
            }

            menu("Options") {
                menuItem("Layers Filter", enabled = isEnvironmentOpened, block = ::doOpenLayersFilter)
                menuItem("Toggle Area", shortcut = "Ctrl+1", enabled = isEnvironmentOpened, selected = isAreaLayerActive, block = ::toggleAreaLayer)
                menuItem("Toggle Turf", shortcut = "Ctrl+2", enabled = isEnvironmentOpened, selected = isTurfLayerActive, block = ::toggleTurfLayer)
                menuItem("Toggle Object", shortcut = "Ctrl+3", enabled = isEnvironmentOpened, selected = isObjLayerActive, block = ::toggleObjLayer)
                menuItem("Toggle Mob", shortcut = "Ctrl+4", enabled = isEnvironmentOpened, selected = isMobLayerActive, block = ::toggleMobLayer)
                separator()
                menuItem("Frame Areas", selected = providedFrameAreas, block = {})
                menuItem("Preferences...", block = ::doOpenPreferences)
            }

            menu("Window") {
                menuItem("Reset Windows", block = ::doResetWindows)
            }

            menu("Help") {
                menuItem("About", block = ::doAbout)
            }

            progressText?.let {
                val count = (ImGui.getTime() / 0.25).toInt() and 3
                val bar = charArrayOf('|', '/', '-', '\\')
                text("${bar[count]} $it${".".repeat(count)}")
            }
        }
    }

    private fun showRecentEnvironments() {
        if (providedRecentEnvironments.isEmpty()) {
            return
        }

        providedRecentEnvironments.toTypedArray().forEach { recentEnvironmentPath ->
            menuItem(recentEnvironmentPath) {
                sendEvent(TriggerEnvironmentController.OpenEnvironment(File(recentEnvironmentPath)))
            }
        }
        separator()
        menuItem("Clear Recent Environments") {
            sendEvent(TriggerRecentFilesController.ClearRecentEnvironments())
        }
    }

    private fun showRecentMaps() {
        if (providedRecentMaps.isEmpty()) {
            return
        }

        providedRecentMaps.toTypedArray().forEach { (readable, absolute) ->
            menuItem(readable) {
                sendEvent(TriggerMapHolderController.OpenMap(File(absolute)))
            }
        }
        separator()
        menuItem("Clear Recent Maps") {
            sendEvent(TriggerRecentFilesController.ClearRecentMaps())
        }
    }

    private fun doOpenEnvironment() {
        NfdUtil.selectFile("dme")?.let { file ->
            sendEvent(TriggerEnvironmentController.OpenEnvironment(file))
        }
    }

    private fun doNewMap() {
        if (!isEnvironmentOpened) {
            return
        }

        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment {
            NfdUtil.saveFile("dmm", it.absRootDirPath)?.let { file ->
                sendEvent(TriggerMapHolderController.CreateNewMap(file))
            }
        })
    }

    private fun doOpenMap() {
        if (!isEnvironmentOpened) {
            return
        }

        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment { environment ->
            NfdUtil.selectFile("dmm", environment.absRootDirPath)?.let { path ->
                sendEvent(TriggerMapHolderController.OpenMap(path))
            }
        })
    }

    private fun doOpenAvailableMap() {
        if (isEnvironmentOpened) {
            sendEvent(TriggerAvailableMapsDialogUi.Open())
        }
    }

    private fun doCloseMap() {
        if (isEnvironmentOpened) {
            sendEvent(TriggerMapHolderController.CloseSelectedMap())
        }
    }

    private fun doCloseAllMaps() {
        if (isEnvironmentOpened) {
            sendEvent(TriggerMapHolderController.CloseAllMaps())
        }
    }

    private fun doSave() {
        if (isEnvironmentOpened) {
            sendEvent(TriggerMapHolderController.SaveSelectedMap())
        }
    }

    private fun doSaveAll() {
        if (isEnvironmentOpened) {
            sendEvent(TriggerMapHolderController.SaveAllMaps())
        }
    }

    private fun doSaveAs() {
        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment {
            NfdUtil.saveFile("dmm", it.absRootDirPath)?.let { file ->
                sendEvent(TriggerMapHolderController.SaveSelectedMapToFile(file))
            }
        })
    }

    private fun doExit() {
        GLFW.glfwSetWindowShouldClose(AppWindow.window, true)
    }

    private fun doUndo() {
        sendEvent(TriggerActionController.UndoAction())
    }

    private fun doRedo() {
        sendEvent(TriggerActionController.RedoAction())
    }

    private fun doCut() {
        sendEvent(TriggerClipboardController.Cut())
    }

    private fun doCopy() {
        sendEvent(TriggerClipboardController.Copy())
    }

    private fun doPaste() {
        sendEvent(TriggerClipboardController.Paste())
    }

    private fun doDelete() {
        sendEvent(TriggerMapModifierController.DeleteTileItemsInActiveArea())
    }

    private fun doDeselectAll() {
        sendEvent(TriggerToolsController.ResetTool())
    }

    private fun doSetMapSize() {
        sendEvent(TriggerSetMapSizeDialogUi.Open())
    }

    private fun doFindInstance() {
        providedShowInstanceLocator.set(!providedShowInstanceLocator.get())
    }

    private fun doOpenPreferences() {
        sendEvent(TriggerPreferencesPanelUi.Open())
    }

    private fun doOpenLayersFilter() {
        sendEvent(TriggerLayersFilterPanelUi.Open())
    }

    private fun toggleAreaLayer() {
        toggleLayer(isAreaLayerActive, TYPE_AREA)
    }

    private fun toggleAreaLayerManual() {
        if (isEnvironmentOpened) {
            isAreaLayerActive.set(!isAreaLayerActive.get())
            toggleLayer(isAreaLayerActive, TYPE_AREA)
        }
    }

    private fun toggleTurfLayer() {
        toggleLayer(isTurfLayerActive, TYPE_TURF)
    }

    private fun toggleTurfLayerManual() {
        if (isEnvironmentOpened) {
            isTurfLayerActive.set(!isTurfLayerActive.get())
            toggleLayer(isTurfLayerActive, TYPE_TURF)
        }
    }

    private fun toggleObjLayer() {
        toggleLayer(isObjLayerActive, TYPE_OBJ)
    }

    private fun toggleObjLayerManual() {
        if (isEnvironmentOpened) {
            isObjLayerActive.set(!isObjLayerActive.get())
            toggleLayer(isObjLayerActive, TYPE_OBJ)
        }
    }

    private fun toggleMobLayer() {
        toggleLayer(isMobLayerActive, TYPE_MOB)
    }

    private fun toggleMobLayerManual() {
        if (isEnvironmentOpened) {
            isMobLayerActive.set(!isMobLayerActive.get())
            toggleLayer(isMobLayerActive, TYPE_MOB)
        }
    }

    private fun toggleLayer(layerStatus: ImBool, layerType: String) {
        if (layerStatus.get()) {
            sendEvent(TriggerLayersFilterController.ShowLayersByType(layerType))
        } else {
            sendEvent(TriggerLayersFilterController.HideLayersByType(layerType))
        }
    }

    private fun doResetWindows() {
        AppWindow.resetWindows()
    }

    private fun doAbout() {
        sendEvent(TriggerAboutPanelUi.Open())
    }

    private fun handleEnvironmentLoading(event: Event<File, Unit>) {
        progressText = "Loading " + event.body.absolutePath.replace('\\', '/').substringAfterLast("/")
    }

    private fun handleEnvironmentLoaded() {
        progressText = null
    }

    private fun handleEnvironmentChanged() {
        isEnvironmentOpened = true
    }

    private fun handleEnvironmentReset() {
        isEnvironmentOpened = false
    }

    private fun handleSelectedMapChanged() {
        isMapOpened = true
    }

    private fun handleSelectedMapClosed() {
        isMapOpened = false
    }

    private fun handleActionStatusChanged(event: Event<ActionStatus, Unit>) {
        isUndoEnabled = event.body.hasUndoAction
        isRedoEnabled = event.body.hasRedoAction
    }

    private fun handleLayersFilterRefreshed(event: Event<Set<DmeItemType>, Unit>) {
        isAreaLayerActive.set(!event.body.contains(TYPE_AREA))
        isTurfLayerActive.set(!event.body.contains(TYPE_TURF))
        isObjLayerActive.set(!event.body.contains(TYPE_OBJ))
        isMobLayerActive.set(!event.body.contains(TYPE_MOB))
    }

    private fun handleProviderInstanceLocatorPanelUiOpen(event: Event<ImBool, Unit>) {
        providedShowInstanceLocator = event.body
    }

    private fun handleProviderCanvasControllerFrameAreas(event: Event<ImBool, Unit>) {
        providedFrameAreas = event.body
    }

    private fun handleProviderRecentFilesControllerRecentEnvironments(event: Event<List<String>, Unit>) {
        providedRecentEnvironments = event.body
    }

    private fun handleProviderRecentFilesControllerRecentMaps(event: Event<List<MapPath>, Unit>) {
        providedRecentMaps = event.body
    }
}
