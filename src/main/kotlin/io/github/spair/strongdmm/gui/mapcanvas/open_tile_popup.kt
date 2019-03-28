package io.github.spair.strongdmm.gui.mapcanvas

import io.github.spair.strongdmm.DI
import io.github.spair.strongdmm.gui.edit.ViewVariablesListener
import io.github.spair.strongdmm.logic.dme.VAR_NAME
import io.github.spair.strongdmm.logic.dmi.DmiProvider
import org.kodein.di.erased.instance
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import javax.swing.JMenu
import javax.swing.JMenuItem

private val dmiProvider by DI.instance<DmiProvider>()

fun MapCanvasController.openTilePopup() {
    view.createAndShowTilePopup(Mouse.getX(), Display.getHeight() - Mouse.getY()) { popup ->
        selectedMap!!.getTile(xMouseMap, yMouseMap)!!.tileItems.forEach { tileItem ->
            val menu = JMenu("${tileItem.getVar(VAR_NAME)} (${tileItem.type})").apply { popup.add(this) }

            dmiProvider.getDmi(tileItem.icon)?.let { dmi ->
                dmi.getIconState(tileItem.iconState)?.let { iconState ->
                    menu.icon = iconState.getIconSprite(tileItem.dir).scaledIcon
                }
            }

            menu.add(JMenuItem("View Variables")).apply { addActionListener(ViewVariablesListener(tileItem)) }
        }
    }
}