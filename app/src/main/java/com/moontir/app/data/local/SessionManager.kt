package com.moontir.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moontir.app.BuildConfig
import com.moontir.app.data.model.Order
import com.moontir.app.data.model.User
import com.moontir.app.data.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("moontir_session_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _tokenFlow = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _userFlow = MutableStateFlow(getUser())
    val userFlow: StateFlow<User?> = _userFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _baseUrlFlow = MutableStateFlow(prefs.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL) ?: BuildConfig.DEFAULT_BASE_URL)
    val baseUrlFlow: StateFlow<String> = _baseUrlFlow.asStateFlow()

    // Default to true so all features run standalone without requiring a local backend server
    private val _isMockModeFlow = MutableStateFlow(prefs.getBoolean(KEY_MOCK_MODE, true))
    val isMockModeFlow: StateFlow<Boolean> = _isMockModeFlow.asStateFlow()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        _tokenFlow.value = token
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER)
            .apply()
        _tokenFlow.value = null
        _userFlow.value = null
    }

    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUser(user: User) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
        _userFlow.value = user
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _languageFlow.value = lang
    }

    fun getTheme(): String = prefs.getString(KEY_THEME, "system") ?: "system"

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _themeFlow.value = theme
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL) ?: BuildConfig.DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        val sanitized = if (!url.endsWith("/")) "$url/" else url
        prefs.edit().putString(KEY_BASE_URL, sanitized).apply()
        _baseUrlFlow.value = sanitized
    }

    fun isMockMode(): Boolean = prefs.getBoolean(KEY_MOCK_MODE, true)

    fun setMockMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOCK_MODE, enabled).apply()
        _isMockModeFlow.value = enabled
    }

    // Local / Standalone Vehicle Persistence
    fun getLocalVehicles(): List<Vehicle> {
        val json = prefs.getString(KEY_LOCAL_VEHICLES, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Vehicle>>() {}.type
                val list = gson.fromJson<List<Vehicle>>(json, type)
                if (list != null) return list
            } catch (e: Exception) {
                // fall through to seed
            }
        }
        val defaultList = listOf(
            Vehicle(
                id = "veh-1",
                nickname = "Family Innova",
                make = "Toyota",
                model = "Innova Reborn",
                year = "2023",
                plate = "B 1234 MON",
                type = "MPV",
                createdAt = "2026-09-28T00:00:00Z"
            ),
            Vehicle(
                id = "veh-2",
                nickname = "Daily Civic",
                make = "Honda",
                model = "Civic RS",
                year = "2022",
                plate = "B 5678 LNR",
                type = "Sedan",
                createdAt = "2026-09-28T00:00:00Z"
            )
        )
        saveLocalVehicles(defaultList)
        return defaultList
    }

    fun saveLocalVehicles(vehicles: List<Vehicle>) {
        prefs.edit().putString(KEY_LOCAL_VEHICLES, gson.toJson(vehicles)).apply()
    }

    // Local / Standalone Order Persistence
    fun getLocalOrders(): List<Order> {
        val json = prefs.getString(KEY_LOCAL_ORDERS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Order>>() {}.type
                val list = gson.fromJson<List<Order>>(json, type)
                if (list != null) return list
            } catch (e: Exception) {
                // fall through to seed
            }
        }
        return emptyList()
    }

    fun saveLocalOrders(orders: List<Order>) {
        prefs.edit().putString(KEY_LOCAL_ORDERS, gson.toJson(orders)).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "auth_user"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_THEME = "app_theme"
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_MOCK_MODE = "is_mock_mode"
        private const val KEY_LOCAL_VEHICLES = "local_vehicles_data"
        private const val KEY_LOCAL_ORDERS = "local_orders_data"
    }
}
