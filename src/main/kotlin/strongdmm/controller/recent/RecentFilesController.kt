package strongdmm.controller.recent

import com.google.gson.Gson
import strongdmm.StrongDMM
import strongdmm.byond.dme.Dme
import strongdmm.byond.dmm.Dmm
import strongdmm.byond.dmm.MapPath
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.type.Provider
import strongdmm.event.type.Reaction
import strongdmm.event.type.controller.TriggerEnvironmentController
import strongdmm.event.type.controller.TriggerRecentFilesController
import java.io.File

class RecentFilesController : EventConsumer, EventSender {
    companion object {
        private val recentFilesConfig: File = File(StrongDMM.homeDir.toFile(), "recent.json")
    }

    private lateinit var recentFiles: RecentFiles

    private val recentEnvironments: MutableList<String> = mutableListOf()
    private val recentMaps: MutableList<MapPath> = mutableListOf()

    init {
        consumeEvent(Reaction.EnvironmentChanged::class.java, ::handleEnvironmentChanged)
        consumeEvent(Reaction.EnvironmentReset::class.java, ::handleEnvironmentReset)
        consumeEvent(Reaction.SelectedMapChanged::class.java, ::handleSelectedMapChanged)
        consumeEvent(TriggerRecentFilesController.ClearRecentEnvironments::class.java, ::handleClearRecentEnvironments)
        consumeEvent(TriggerRecentFilesController.ClearRecentMaps::class.java, ::handleClearRecentMaps)
    }

    fun postInit() {
        ensureRecentFilesConfigExists()
        readRecentFilesConfig()

        sendEvent(Provider.RecentFilesControllerRecentEnvironments(recentEnvironments))
        sendEvent(Provider.RecentFilesControllerRecentMaps(recentMaps))
    }

    private fun ensureRecentFilesConfigExists() {
        if (recentFilesConfig.createNewFile()) {
            recentFilesConfig.writeText(Gson().toJson(RecentFiles()))
        }
    }

    private fun readRecentFilesConfig() {
        recentFilesConfig.reader().use {
            recentFiles = Gson().fromJson(it, RecentFiles::class.java)
            validateRecentFiles()
            updateRecentEnvironmentsList()
        }
    }

    private fun writeRecentJsonFile() {
        recentFilesConfig.writeText(Gson().toJson(recentFiles))
    }

    private fun validateRecentFiles() {
        recentFiles.environments.toList().forEach {
            if (!File(it).exists()) {
                recentFiles.environments.remove(it)
            }
        }
        recentFiles.maps.toMap().forEach { (envPath, mapPaths) ->
            if (File(envPath).exists()) {
                mapPaths.toList().forEach { mapPath ->
                    if (!File(mapPath.absolute).exists()) {
                        mapPaths.remove(mapPath)
                    }
                }
            } else {
                recentFiles.maps.remove(envPath)
            }
        }
    }

    private fun addEnvironment(environmentPath: String) {
        recentFiles.environments.remove(environmentPath)
        recentFiles.environments.add(0, environmentPath)
        writeRecentJsonFile()
        updateRecentEnvironmentsList()
    }

    private fun addMap(environmentPath: String, mapPath: MapPath) {
        val maps = recentFiles.maps.getOrPut(environmentPath) { mutableListOf() }
        maps.remove(mapPath)
        maps.add(0, mapPath)
        writeRecentJsonFile()
        updateRecentMapsList(environmentPath)
    }

    private fun updateRecentEnvironmentsList() {
        recentEnvironments.clear()
        recentEnvironments.addAll(recentFiles.environments)
    }

    private fun updateRecentMapsList(currentEnvironmentPath: String) {
        recentMaps.clear()
        recentMaps.addAll(recentFiles.maps.getOrPut(currentEnvironmentPath) { mutableListOf() })
    }

    private fun handleEnvironmentChanged(event: Event<Dme, Unit>) {
        addEnvironment(event.body.absEnvPath)
        updateRecentMapsList(event.body.absEnvPath)
    }

    private fun handleEnvironmentReset() {
        recentMaps.clear()
    }

    private fun handleSelectedMapChanged(event: Event<Dmm, Unit>) {
        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment { environment ->
            addMap(environment.absEnvPath, event.body.mapPath)
        })
    }

    private fun handleClearRecentEnvironments() {
        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment { environment ->
            recentFiles.environments.clear()
            writeRecentJsonFile()
            updateRecentMapsList(environment.absEnvPath)
        })
    }

    private fun handleClearRecentMaps() {
        sendEvent(TriggerEnvironmentController.FetchOpenedEnvironment { environment ->
            recentFiles.maps[environment.absEnvPath]?.clear()
            writeRecentJsonFile()
            updateRecentMapsList(environment.absEnvPath)
        })
    }
}
