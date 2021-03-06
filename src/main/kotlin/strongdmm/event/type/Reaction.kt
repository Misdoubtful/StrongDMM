package strongdmm.event.type

import strongdmm.byond.dme.Dme
import strongdmm.byond.dmm.Dmm
import strongdmm.byond.dmm.MapPos
import strongdmm.byond.dmm.MapSize
import strongdmm.byond.dmm.TileItem
import strongdmm.controller.action.ActionStatus
import strongdmm.controller.shortcut.Shortcut
import strongdmm.controller.tool.ToolType
import strongdmm.event.*
import java.io.File

abstract class Reaction {
    class ApplicationBlockChanged(body: ApplicationBlockStatus) : Event<ApplicationBlockStatus, Unit>(body, null)

    class EnvironmentReset : Event<Unit, Unit>(Unit, null)
    class EnvironmentLoading(body: File) : Event<File, Unit>(body, null)
    class EnvironmentLoaded(body: EnvironmentLoadedStatus) : Event<EnvironmentLoadedStatus, Unit>(body, null)
    class EnvironmentChanged(body: Dme) : Event<Dme, Unit>(body, null)

    class SelectedMapChanged(body: Dmm) : Event<Dmm, Unit>(body, null)
    class SelectedMapClosed : Event<Unit, Unit>(Unit, null)
    class OpenedMapClosed(body: Dmm) : Event<Dmm, Unit>(body, null)
    class SelectedMapZActiveChanged(body: ActiveZ) : Event<ActiveZ, Unit>(body, null)
    class SelectedMapMapSizeChanged(body: MapSize) : Event<MapSize, Unit>(body, null)

    class MapMousePosChanged(body: MapPos) : Event<MapPos, Unit>(body, null)
    class MapMouseDragStarted : Event<Unit, Unit>(Unit, null)
    class MapMouseDragStopped : Event<Unit, Unit>(Unit, null)

    class FrameRefreshed : Event<Unit, Unit>(Unit, null)
    class ActionStatusChanged(body: ActionStatus) : Event<ActionStatus, Unit>(body, null)
    class ActiveTileItemChanged(body: TileItem?) : Event<TileItem?, Unit>(body, null)
    class LayersFilterRefreshed(body: Set<DmeItemType>) : Event<Set<DmeItemType>, Unit>(body, null)
    class ActiveToolChanged(body: ToolType) : Event<ToolType, Unit>(body, null)
    class ShortcutTriggered(body: Shortcut) : Event<Shortcut, Unit>(body, null)

    class TilePopupOpened : Event<Unit, Unit>(Unit, null)
    class TilePopupClosed : Event<Unit, Unit>(Unit, null)
}
