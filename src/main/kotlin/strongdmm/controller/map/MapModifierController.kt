package strongdmm.controller.map

import strongdmm.byond.dmm.*
import strongdmm.controller.action.undoable.MultiAction
import strongdmm.controller.action.undoable.ReplaceTileAction
import strongdmm.controller.action.undoable.Undoable
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.TileItemType
import strongdmm.event.type.Reaction
import strongdmm.event.type.controller.*
import strongdmm.util.OUT_OF_BOUNDS

class MapModifierController : EventConsumer, EventSender {
    private var currentMapPos: MapPos = MapPos(OUT_OF_BOUNDS, OUT_OF_BOUNDS)

    init {
        consumeEvent(Reaction.MapMousePosChanged::class.java, ::handleMapMousePosChanged)
        consumeEvent(TriggerMapModifierController.DeleteTileItemsInActiveArea::class.java, ::handleDeleteTileItemsInActiveArea)
        consumeEvent(TriggerMapModifierController.FillSelectedMapPositionWithTileItems::class.java, ::handleFillSelectedMapPositionWithTileItems)
        consumeEvent(TriggerMapModifierController.ReplaceTileItemsWithTypeInPositions::class.java, ::handleReplaceTileItemsWithTypeInPositions)
        consumeEvent(TriggerMapModifierController.ReplaceTileItemsWithIdInPositions::class.java, ::handleReplaceTileItemsWithIdInPositions)
        consumeEvent(TriggerMapModifierController.DeleteTileItemsWithTypeInPositions::class.java, ::handleDeleteTileItemsWithTypeInPositions)
        consumeEvent(TriggerMapModifierController.DeleteTileItemsWithIdInPositions::class.java, ::handleDeleteTileItemsWithIdInPositions)
        consumeEvent(TriggerMapModifierController.ChangeMapSize::class.java, ::handleChangeMapSize)
    }

    private fun handleMapMousePosChanged(event: Event<MapPos, Unit>) {
        currentMapPos = event.body
    }

    private fun handleDeleteTileItemsInActiveArea() {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { selectedMap ->
            sendEvent(TriggerLayersFilterController.FetchFilteredLayers { filteredLayers ->
                sendEvent(TriggerToolsController.FetchActiveArea { activeArea ->
                    val reverseActions = mutableListOf<Undoable>()

                    for (x in (activeArea.x1..activeArea.x2)) {
                        for (y in (activeArea.y1..activeArea.y2)) {
                            val tile = selectedMap.getTile(x, y, selectedMap.zActive)
                            val initialTileItems = tile.getTileItemsId()

                            tile.getFilteredTileItems(filteredLayers).let { filteredTileItems ->
                                val replaceTileAction = ReplaceTileAction(tile) {
                                    filteredTileItems.forEach { tileItem ->
                                        tile.deleteTileItem(tileItem)
                                    }
                                }

                                if (!tile.getTileItemsId().contentEquals(initialTileItems)) {
                                    reverseActions.add(replaceTileAction)
                                }
                            }
                        }
                    }

                    if (reverseActions.isNotEmpty()) {
                        sendEvent(TriggerActionController.AddAction(MultiAction(reverseActions)))
                        sendEvent(TriggerFrameController.RefreshFrame())
                    }
                })
            })
        })
    }

    private fun handleFillSelectedMapPositionWithTileItems(event: Event<Array<Array<List<TileItem>>>, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { selectedMap ->
            sendEvent(TriggerLayersFilterController.FetchFilteredLayers { filteredLayers ->
                val reverseActions = mutableListOf<Undoable>()

                var x2 = currentMapPos.x
                var y2 = currentMapPos.y

                for ((x, col) in event.body.withIndex()) {
                    for ((y, tileItems) in col.withIndex()) {
                        val xPos = currentMapPos.x + x
                        val yPos = currentMapPos.y + y

                        if (xPos !in 1..selectedMap.maxX || yPos !in 1..selectedMap.maxY) {
                            continue
                        }

                        x2 = xPos
                        y2 = yPos

                        val tile = selectedMap.getTile(currentMapPos.x + x, currentMapPos.y + y, selectedMap.zActive)

                        reverseActions.add(ReplaceTileAction(tile) {
                            tile.getFilteredTileItems(filteredLayers).forEach { tileItem ->
                                tile.deleteTileItem(tileItem)
                            }

                            tileItems.forEach {
                                tile.addTileItem(it)
                            }
                        })
                    }
                }

                if (reverseActions.isNotEmpty()) {
                    sendEvent(TriggerActionController.AddAction(MultiAction(reverseActions)))
                    sendEvent(TriggerToolsController.SelectActiveArea(MapArea(currentMapPos.x, currentMapPos.y, x2, y2)))
                    sendEvent(TriggerFrameController.RefreshFrame())
                }
            })
        })
    }

    private fun handleReplaceTileItemsWithTypeInPositions(event: Event<Pair<TileItemType, List<Pair<TileItem, MapPos>>>, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { dmm ->
            val replaceWithTileItem = GlobalTileItemHolder.getOrCreate(event.body.first)
            val replaceActions = mutableListOf<Undoable>()

            event.body.second.forEach { (tileItem, pos) ->
                val tile = dmm.getTile(pos.x, pos.y, dmm.zActive)
                replaceActions.add(ReplaceTileAction(tile) {
                    tile.replaceTileItem(tileItem.type, replaceWithTileItem)
                })
            }

            sendEvent(TriggerActionController.AddAction(MultiAction(replaceActions)))
            sendEvent(TriggerFrameController.RefreshFrame())
        })
    }

    private fun handleReplaceTileItemsWithIdInPositions(event: Event<Pair<TileItemType, List<Pair<TileItem, MapPos>>>, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { dmm ->
            val replaceWithTileItem = GlobalTileItemHolder.getOrCreate(event.body.first)
            val replaceActions = mutableListOf<Undoable>()

            event.body.second.forEach { (tileItem, pos) ->
                val tile = dmm.getTile(pos.x, pos.y, dmm.zActive)
                replaceActions.add(ReplaceTileAction(tile) {
                    tile.replaceTileItem(tileItem.id, replaceWithTileItem)
                })
            }

            sendEvent(TriggerActionController.AddAction(MultiAction(replaceActions)))
            sendEvent(TriggerFrameController.RefreshFrame())
        })
    }

    private fun handleDeleteTileItemsWithTypeInPositions(event: Event<List<Pair<TileItem, MapPos>>, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { dmm ->
            val deleteActions = mutableListOf<Undoable>()

            event.body.forEach { (tileItem, pos) ->
                val tile = dmm.getTile(pos.x, pos.y, dmm.zActive)
                deleteActions.add(ReplaceTileAction(tile) {
                    tile.deleteTileItem(tileItem.type)
                })
            }

            sendEvent(TriggerActionController.AddAction(MultiAction(deleteActions)))
            sendEvent(TriggerFrameController.RefreshFrame())
        })
    }

    private fun handleDeleteTileItemsWithIdInPositions(event: Event<List<Pair<TileItem, MapPos>>, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { dmm ->
            val deleteActions = mutableListOf<Undoable>()

            event.body.forEach { (tileItem, pos) ->
                val tile = dmm.getTile(pos.x, pos.y, dmm.zActive)
                deleteActions.add(ReplaceTileAction(tile) {
                    tile.deleteTileItem(tileItem.id)
                })
            }

            sendEvent(TriggerActionController.AddAction(MultiAction(deleteActions)))
            sendEvent(TriggerFrameController.RefreshFrame())
        })
    }

    private fun handleChangeMapSize(event: Event<MapSize, Unit>) {
        sendEvent(TriggerMapHolderController.FetchSelectedMap { dmm ->
            dmm.setMapSize(event.body.maxZ, event.body.maxY, event.body.maxX)
            sendEvent(Reaction.SelectedMapMapSizeChanged(event.body))
        })
    }
}
