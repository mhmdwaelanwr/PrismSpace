package com.prismspace.container.core.privacy

import android.os.Build
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.app.PActivityThread
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Locale

object DeviceProfileManager {
    private const val PREF_NAME = "premium_device_profiles_v1"
    private const val KEY_PREFIX = "profile_"
    private val lock = Any()
    private val secureRandom = SecureRandom()
    private val commonModels = listOf(
        "Galaxy S24" to "Samsung",
        "Galaxy S24 Ultra" to "Samsung",
        "Pixel 8 Pro" to "Google",
        "Pixel 9" to "Google",
        "Xiaomi 14" to "Xiaomi",
        "OnePlus 12" to "OnePlus"
    )
    private val macPrefixes = listOf(
        "3C:5A:B4",
        "F0:18:98",
        "A4:C3:F0",
        "D8:BB:2C",
        "58:CB:52",
        "40:4E:36"
    )

    data class DeviceProfile(
        val imei: String? = null,
        val mac: String? = null,
        val androidId: String? = null,
        val model: String? = null,
        val manufacturer: String? = null
    ) {
        fun toJson(): String {
            val json = JSONObject()
            putIfNotBlank(json, "imei", imei)
            putIfNotBlank(json, "mac", mac)
            putIfNotBlank(json, "androidId", androidId)
            putIfNotBlank(json, "model", model)
            putIfNotBlank(json, "manufacturer", manufacturer)
            return json.toString()
        }

        companion object {
            fun fromJson(raw: String): DeviceProfile? {
                return runCatching {
                    val json = JSONObject(raw)
                    DeviceProfile(
                        imei = json.optStringOrNull("imei"),
                        mac = json.optStringOrNull("mac"),
                        androidId = json.optStringOrNull("androidId"),
                        model = json.optStringOrNull("model"),
                        manufacturer = json.optStringOrNull("manufacturer")
                    )
                }.getOrNull()
            }

            fun defaultProfile(): DeviceProfile {
                return DeviceProfile(
                    model = Build.MODEL,
                    manufacturer = Build.MANUFACTURER
                )
            }

            private fun putIfNotBlank(json: JSONObject, key: String, value: String?) {
                if (!value.isNullOrBlank()) {
                    json.put(key, value)
                }
            }

            private fun JSONObject.optStringOrNull(key: String): String? {
                val value = optString(key, "").trim()
                return value.ifEmpty { null }
            }
        }
    }

    @JvmStatic
    fun saveProfile(packageName: String, userId: Int, profile: DeviceProfile): Boolean {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) return false
        val prefs = preferences() ?: return false
        synchronized(lock) {
            prefs.edit().putString(storageKey(normalizedPackage, normalizeUserId(userId)), profile.toJson()).apply()
        }
        return true
    }

    @JvmStatic
    fun getProfile(packageName: String?, userId: Int): DeviceProfile? {
        val normalizedPackage = packageName?.trim().orEmpty()
        if (normalizedPackage.isEmpty()) return null
        val prefs = preferences() ?: return null
        val raw = synchronized(lock) {
            prefs.getString(storageKey(normalizedPackage, normalizeUserId(userId)), null)
        } ?: return null
        return DeviceProfile.fromJson(raw)
    }

    @JvmStatic
    fun clearProfile(packageName: String, userId: Int): Boolean {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) return false
        val prefs = preferences() ?: return false
        synchronized(lock) {
            prefs.edit().remove(storageKey(normalizedPackage, normalizeUserId(userId))).apply()
        }
        return true
    }

    @JvmStatic
    fun getProfileForCurrentApp(): DeviceProfile? {
        return getProfile(PActivityThread.getAppPackageName(), PActivityThread.getUserId())
    }

    @JvmStatic
    fun generateRandomProfile(): DeviceProfile {
        val selected = commonModels[secureRandom.nextInt(commonModels.size)]
        return DeviceProfile(
            imei = generateRandomImei(),
            mac = generateRandomMac(),
            androidId = generateRandomAndroidId(),
            model = selected.first,
            manufacturer = selected.second
        )
    }

    private fun generateRandomImei(): String {
        val base = buildString {
            append("86")
            repeat(12) {
                append(secureRandom.nextInt(10))
            }
        }
        val checkDigit = calculateLuhnCheckDigit(base)
        return base + checkDigit
    }

    private fun calculateLuhnCheckDigit(numberWithoutCheck: String): Int {
        var sum = 0
        for (index in numberWithoutCheck.indices) {
            var digit = numberWithoutCheck[index].digitToInt()
            if (index % 2 == 1) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun generateRandomMac(): String {
        val prefix = macPrefixes[secureRandom.nextInt(macPrefixes.size)]
        val suffix = (1..3)
            .joinToString(":") { String.format(Locale.US, "%02X", secureRandom.nextInt(256)) }
        return "$prefix:$suffix"
    }

    private fun generateRandomAndroidId(): String {
        val chars = CharArray(16)
        val hexChars = "0123456789abcdef"
        for (i in chars.indices) {
            chars[i] = hexChars[secureRandom.nextInt(hexChars.length)]
        }
        return String(chars)
    }

    private fun preferences() = PrismSpaceCore.getContext()?.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)

    private fun normalizeUserId(userId: Int): Int = if (userId < 0) 0 else userId

    private fun storageKey(packageName: String, userId: Int): String = "$KEY_PREFIX${userId}_$packageName"
}
