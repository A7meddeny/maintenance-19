package com.masar.maintenance.data

import android.content.Context
import android.content.SharedPreferences

/** تخزين بسيط لرابط الخادم والتوكن وبيانات المستخدم الحالي. */
class Session(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("masar_session", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString("base_url", "") ?: ""
        set(v) = sp.edit().putString("base_url", normalize(v)).apply()

    var token: String?
        get() = sp.getString("token", null)
        set(v) = sp.edit().putString("token", v).apply()

    var userId: Int
        get() = sp.getInt("uid", 0)
        set(v) = sp.edit().putInt("uid", v).apply()

    var userName: String
        get() = sp.getString("uname", "") ?: ""
        set(v) = sp.edit().putString("uname", v).apply()

    var userRole: String
        get() = sp.getString("urole", "") ?: ""
        set(v) = sp.edit().putString("urole", v).apply()

    var userPhoto: String?
        get() = sp.getString("uphoto", null)
        set(v) = sp.edit().putString("uphoto", v).apply()

    var lang: String
        get() = sp.getString("lang", "ar") ?: "ar"
        set(v) = sp.edit().putString("lang", v).apply()

    val isLoggedIn: Boolean get() = !token.isNullOrBlank() && baseUrl.isNotBlank()

    fun saveUser(u: User) { userId = u.id; userName = u.name; userRole = u.role; userPhoto = u.photo }

    fun clearAuth() {
        sp.edit().remove("token").remove("uid").remove("uname").remove("urole").remove("uphoto").apply()
    }

    companion object {
        /** يضمن أن الرابط ينتهي بـ / ويبدأ بـ http */
        fun normalize(url: String): String {
            var u = url.trim()
            if (u.isEmpty()) return u
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
            if (!u.endsWith("/")) u += "/"
            return u
        }
    }
}
