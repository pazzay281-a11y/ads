package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class BubbleClickAction {
    DIRECT_RESET, // Tapping floating bubble instantly launches Google Ads settings
    SHOW_MENU     // Tapping floating bubble shows floating popup controls
}

enum class BubbleColor(val label: String, val hex: Long) {
    BLUE("Google Blue", 0xFF1E88E5),
    GREEN("Emerald", 0xFF2E7D32),
    AMBER("Amber Gold", 0xFFF57F17),
    RED("Ruby Red", 0xFFD32F2F),
    PURPLE("Deep Purple", 0xFF7B1FA2),
    DARK("Onyx Dark", 0xFF263238)
}

data class ResetLogEntry(
    val timestamp: Long,
    val type: String, // "RESET_LAUNCHED" or "ID_REFRESHED"
    val note: String,
    val adId: String = ""
)

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_reset_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BUBBLE_SIZE = "bubble_size" // 48, 58, 68
        private const val KEY_BUBBLE_ALPHA = "bubble_alpha" // 0.3f to 1.0f
        private const val KEY_CLICK_ACTION = "bubble_click_action"
        private const val KEY_SNAP_EDGES = "snap_edges"
        private const val KEY_VIBRATE = "vibrate_enabled"
        private const val KEY_BUBBLE_COLOR = "bubble_color"
        private const val KEY_LANGUAGE = "app_language" // "en" or "bn"
        private const val KEY_RESET_COUNT = "reset_count"
        private const val KEY_HISTORY_JSON = "history_json"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _bubbleSizeDp = MutableStateFlow(prefs.getInt(KEY_BUBBLE_SIZE, 58))
    val bubbleSizeDp: StateFlow<Int> = _bubbleSizeDp.asStateFlow()

    private val _bubbleAlpha = MutableStateFlow(prefs.getFloat(KEY_BUBBLE_ALPHA, 0.95f))
    val bubbleAlpha: StateFlow<Float> = _bubbleAlpha.asStateFlow()

    private val _clickAction = MutableStateFlow(
        BubbleClickAction.valueOf(prefs.getString(KEY_CLICK_ACTION, BubbleClickAction.DIRECT_RESET.name) ?: BubbleClickAction.DIRECT_RESET.name)
    )
    val clickAction: StateFlow<BubbleClickAction> = _clickAction.asStateFlow()

    private val _snapEdges = MutableStateFlow(prefs.getBoolean(KEY_SNAP_EDGES, true))
    val snapEdges: StateFlow<Boolean> = _snapEdges.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE, true))
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    private val _bubbleColor = MutableStateFlow(
        BubbleColor.valueOf(prefs.getString(KEY_BUBBLE_COLOR, BubbleColor.BLUE.name) ?: BubbleColor.BLUE.name)
    )
    val bubbleColor: StateFlow<BubbleColor> = _bubbleColor.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "bn") ?: "bn")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _resetCount = MutableStateFlow(prefs.getInt(KEY_RESET_COUNT, 0))
    val resetCount: StateFlow<Int> = _resetCount.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<ResetLogEntry>> = _history.asStateFlow()

    fun setBubbleSize(sizeDp: Int) {
        prefs.edit().putInt(KEY_BUBBLE_SIZE, sizeDp).apply()
        _bubbleSizeDp.value = sizeDp
    }

    fun setBubbleAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0.25f, 1.0f)
        prefs.edit().putFloat(KEY_BUBBLE_ALPHA, clamped).apply()
        _bubbleAlpha.value = clamped
    }

    fun setClickAction(action: BubbleClickAction) {
        prefs.edit().putString(KEY_CLICK_ACTION, action.name).apply()
        _clickAction.value = action
    }

    fun setSnapEdges(snap: Boolean) {
        prefs.edit().putBoolean(KEY_SNAP_EDGES, snap).apply()
        _snapEdges.value = snap
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _vibrateEnabled.value = enabled
    }

    fun setBubbleColor(color: BubbleColor) {
        prefs.edit().putString(KEY_BUBBLE_COLOR, color.name).apply()
        _bubbleColor.value = color
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    fun recordResetAction(note: String = "Google Ads settings opened", adId: String = "") {
        val newCount = _resetCount.value + 1
        prefs.edit().putInt(KEY_RESET_COUNT, newCount).apply()
        _resetCount.value = newCount

        val entry = ResetLogEntry(
            timestamp = System.currentTimeMillis(),
            type = "RESET_LAUNCHED",
            note = note,
            adId = adId
        )
        val currentList = _history.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }
        saveHistory(currentList)
        _history.value = currentList
    }

    fun recordIdRefresh(newId: String) {
        val entry = ResetLogEntry(
            timestamp = System.currentTimeMillis(),
            type = "ID_REFRESHED",
            note = "Advertising ID checked",
            adId = newId
        )
        val currentList = _history.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }
        saveHistory(currentList)
        _history.value = currentList
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
        _history.value = emptyList()
    }

    private fun loadHistory(): List<ResetLogEntry> {
        val json = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<ResetLogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ResetLogEntry(
                        timestamp = obj.getLong("timestamp"),
                        type = obj.getString("type"),
                        note = obj.getString("note"),
                        adId = obj.optString("adId", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<ResetLogEntry>) {
        try {
            val array = JSONArray()
            for (entry in list) {
                val obj = JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("type", entry.type)
                    put("note", entry.note)
                    put("adId", entry.adId)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply()
        } catch (_: Exception) {}
    }
}
