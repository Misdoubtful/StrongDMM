package strongdmm.controller.preferences

import com.google.gson.Gson
import strongdmm.StrongDMM
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.type.Provider
import strongdmm.event.type.controller.TriggerPreferencesController
import java.io.File

class PreferencesController : EventSender, EventConsumer {
    companion object {
        private val preferencesConfig: File = File(StrongDMM.homeDir.toFile(), "preferences.json")
    }

    private lateinit var preferences: Preferences

    init {
        consumeEvent(TriggerPreferencesController.SavePreferences::class.java, ::handleSavePreferences)
    }

    fun postInit() {
        ensurePreferencesConfigExists()
        readPreferencesConfig()

        sendEvent(Provider.PreferencesControllerPreferences(preferences))
    }

    private fun ensurePreferencesConfigExists() {
        if (preferencesConfig.createNewFile()) {
            preferencesConfig.writeText(Gson().toJson(Preferences()))
        }
    }

    private fun readPreferencesConfig() {
        preferencesConfig.reader().use {
            preferences = Gson().fromJson(it, Preferences::class.java)
        }
    }

    private fun writePreferencesConfig() {
        preferencesConfig.writeText(Gson().toJson(preferences))
    }

    private fun handleSavePreferences() {
        writePreferencesConfig()
    }
}
