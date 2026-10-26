package dev.cluvex.zedsecure.platform

import kotlin.concurrent.Volatile
import kotlin.random.Random

object AppInfo {
    @Volatile
    var versionName: String = "3.0.9"

    val userAgent: String get() = "ZedSecure/$versionName"
}

object DeviceIdentity {
    @Volatile
    var id: String = ""

    @Volatile
    var os: String = ""

    @Volatile
    var osVersion: String = ""

    @Volatile
    var model: String = ""

    private const val STORE_KEY = "device_hwid"

    /** Панели на основе Remnawave принимают идентификатор только в этом виде. */
    private val ALLOWED = Regex("^[A-Za-z0-9=-]{10,64}$")

    /**
     * Возвращает идентификатор, одинаковый между запусками: берёт сохранённый,
     * иначе принимает [candidate] (например ANDROID_ID), иначе создаёт новый и сохраняет.
     */
    fun ensure(store: KeyValueStore, candidate: String? = null): String {
        val saved = store.getString(STORE_KEY)?.trim().orEmpty()
        if (ALLOWED.matches(saved)) {
            id = saved
            return saved
        }
        val value = candidate?.trim()?.takeIf { ALLOWED.matches(it) } ?: newId()
        store.putString(STORE_KEY, value)
        id = value
        return value
    }

    /** Заголовки, по которым панель считает устройства. Пустая карта — если идентификатора нет. */
    fun subscriptionHeaders(): Map<String, String> {
        val hwid = id.trim()
        if (!ALLOWED.matches(hwid)) return emptyMap()
        return buildMap {
            put("x-hwid", hwid)
            put("x-app-version", AppInfo.versionName)
            os.trim().takeIf { it.isNotEmpty() }?.let { put("x-device-os", it) }
            osVersion.trim().takeIf { it.isNotEmpty() }?.let { put("x-ver-os", it) }
            model.trim().takeIf { it.isNotEmpty() }?.let { put("x-device-model", it) }
        }
    }
}

internal fun newId(): String = buildString {
    val hex = "0123456789abcdef"
    repeat(32) { append(hex[Random.nextInt(16)]) }
}
