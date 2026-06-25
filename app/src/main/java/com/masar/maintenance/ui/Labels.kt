package com.masar.maintenance.ui

import androidx.compose.ui.graphics.Color
import com.masar.maintenance.data.Net
import com.masar.maintenance.ui.theme.Blue
import com.masar.maintenance.ui.theme.Green
import com.masar.maintenance.ui.theme.Muted
import com.masar.maintenance.ui.theme.RedStatus
import com.masar.maintenance.ui.theme.Yellow

object Labels {

    private fun orKey(key: String, raw: String): String { val v = I18n.t(key); return if (v == key) raw else v }

    fun role(r: String): String = orKey("role.$r", r)

    fun status(s: String): String = orKey("st.$s", s)

    fun stage(s: String): String = orKey("sg.$s", s)

    fun itemKind(k: String): String = orKey("kind.$k", k)

    fun tirePos(p: String?): String = when (p) {
        "front" -> tr("أمامي", "Front"); "rear" -> tr("خلفي", "Rear")
        "both" -> tr("الكل", "All"); else -> (p ?: "")
    }

    fun statusColor(s: String): Color = when (s) {
        "completed" -> Green
        "sent_to_admin" -> Blue
        else -> Yellow
    }

    /** لون حسب الأيام المتبقية (سالب/قريب = أحمر) */
    fun daysColor(days: Int?): Color {
        val n = days ?: return Muted
        return when { n < 0 -> RedStatus; n <= 7 -> RedStatus; n <= 30 -> Yellow; else -> Green }
    }

    fun carColor(c: String?): Color = when (c) {
        "green" -> Green; "red" -> RedStatus; "yellow" -> Yellow; else -> Muted
    }
}

/** يحوّل المسار النسبي (uploads/..) إلى رابط كامل حسب رابط الخادم. */
fun imageUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return Net.session.baseUrl + path.trimStart('/')
}
