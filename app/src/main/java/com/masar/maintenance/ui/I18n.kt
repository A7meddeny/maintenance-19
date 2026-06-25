package com.masar.maintenance.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.masar.maintenance.data.Net

/**
 * قاموس اللغة للتطبيق (عربي/إنجليزي).
 * - I18n.lang حالة Compose، فتغييرها يُعيد بناء كل الشاشات تلقائياً.
 * - t(key) للمصطلحات المتكررة (أدوار/حالات/مراحل/أزرار…).
 * - tr(ar, en) لنصوص الشاشات المفردة دون مفتاح.
 */
object I18n {
    var lang by mutableStateOf("ar")

    /** يُستدعى بعد تهيئة الجلسة لقراءة اللغة المحفوظة. */
    fun init() { if (Net.isInitialized()) lang = Net.session.lang }

    fun set(l: String) {
        lang = l
        if (Net.isInitialized()) Net.session.lang = l
    }

    fun toggle() = set(if (lang == "ar") "en" else "ar")

    val isAr: Boolean get() = lang == "ar"

    fun t(key: String): String {
        val m = if (lang == "ar") AR else EN
        return m[key] ?: AR[key] ?: key
    }

    private val AR: Map<String, String> = mapOf(
        // الأدوار
        "role.admin" to "الإدارة", "role.office" to "موظف المكتب",
        "role.maintenance" to "قسم الصيانة", "role.purchasing" to "قسم المشتريات",
        "role.manager" to "مدير", "role.car_follow" to "موظف متابعة السيارات",
        // الحالات
        "st.new_office" to "طلب جديد — بانتظار الصيانة",
        "st.maintenance_received" to "الصيانة استلمت السيارة",
        "st.sent_to_purchasing" to "محوّل للمشتريات",
        "st.purchasing_received" to "المشتريات استلم الطلب",
        "st.sent_to_admin" to "بانتظار اعتماد الإدارة",
        "st.admin_approved" to "اعتُمد — جارٍ الشراء",
        "st.purchasing_buying" to "المشتريات يشتري القطعة",
        "st.part_delivered" to "سُلّمت القطعة للصيانة",
        "st.maintenance_fixing" to "الصيانة تُصلح السيارة",
        "st.completed" to "مكتمل — عادت السيارة",
        "st.periodic_self" to "صيانة دورية — قيد التنفيذ",
        "st.rejected" to "مرفوض من الإدارة",
        // المراحل
        "sg.new_office" to "طلب جديد (المكتب)", "sg.periodic_self" to "بدء صيانة دورية",
        "sg.maintenance_received" to "استلام الصيانة", "sg.sent_to_purchasing" to "الإرسال للمشتريات",
        "sg.purchasing_received" to "استلام المشتريات", "sg.sent_to_admin" to "الإرسال للإدارة",
        "sg.admin_approved" to "اعتماد الإدارة", "sg.purchasing_buying" to "بدء الشراء",
        "sg.part_delivered" to "تسليم القطعة", "sg.maintenance_fixing" to "بدء الإصلاح",
        "sg.completed" to "الإنهاء", "sg.rejected" to "رفض الإدارة",
        // الأنواع/التقييم
        "kind.general" to "عام", "kind.oil_change" to "تغيير زيت",
        "kind.tire_change" to "تغيير إطارات", "kind.other_periodic" to "دوري آخر",
        "type.periodic" to "دورية", "type.general" to "عامة",
        "rate.excellent" to "ممتاز", "rate.good" to "جيد", "rate.fair" to "مقبول", "rate.bad" to "ضعيف",
        // عام/أزرار
        "c.save" to "حفظ", "c.cancel" to "إلغاء", "c.search" to "بحث", "c.view" to "عرض",
        "c.action" to "عرض / إجراء", "c.back" to "رجوع", "c.send" to "إرسال", "c.close" to "إغلاق",
        "c.retry" to "إعادة المحاولة", "c.loading" to "جارٍ التحميل…", "c.no_data" to "لا توجد بيانات",
        "c.required" to "إجباري", "c.optional" to "اختياري", "c.add" to "إضافة", "c.delete" to "حذف",
        "c.lang" to "English",
        // الصفحة الرئيسية (العناوين)
        "home.title" to "الرئيسية", "home.logout" to "تسجيل الخروج",
    )

    private val EN: Map<String, String> = mapOf(
        "role.admin" to "Admin", "role.office" to "Office staff",
        "role.maintenance" to "Maintenance", "role.purchasing" to "Purchasing",
        "role.manager" to "Manager", "role.car_follow" to "Car-tracking staff",
        "st.new_office" to "New — awaiting maintenance",
        "st.maintenance_received" to "Maintenance received the car",
        "st.sent_to_purchasing" to "Sent to purchasing",
        "st.purchasing_received" to "Purchasing received the request",
        "st.sent_to_admin" to "Awaiting admin approval",
        "st.admin_approved" to "Approved — buying",
        "st.purchasing_buying" to "Purchasing is buying the part",
        "st.part_delivered" to "Part delivered to maintenance",
        "st.maintenance_fixing" to "Maintenance is repairing",
        "st.completed" to "Completed — car returned",
        "st.periodic_self" to "Periodic maintenance — in progress",
        "st.rejected" to "Rejected by admin",
        "sg.new_office" to "New request (Office)", "sg.periodic_self" to "Periodic maintenance started",
        "sg.maintenance_received" to "Maintenance received", "sg.sent_to_purchasing" to "Sent to purchasing",
        "sg.purchasing_received" to "Purchasing received", "sg.sent_to_admin" to "Sent to admin",
        "sg.admin_approved" to "Admin approved", "sg.purchasing_buying" to "Buying started",
        "sg.part_delivered" to "Part delivered", "sg.maintenance_fixing" to "Repair started",
        "sg.completed" to "Completed", "sg.rejected" to "Admin rejected",
        "kind.general" to "General", "kind.oil_change" to "Oil change",
        "kind.tire_change" to "Tire change", "kind.other_periodic" to "Other periodic",
        "type.periodic" to "Periodic", "type.general" to "General",
        "rate.excellent" to "Excellent", "rate.good" to "Good", "rate.fair" to "Fair", "rate.bad" to "Poor",
        "c.save" to "Save", "c.cancel" to "Cancel", "c.search" to "Search", "c.view" to "View",
        "c.action" to "View / Action", "c.back" to "Back", "c.send" to "Send", "c.close" to "Close",
        "c.retry" to "Retry", "c.loading" to "Loading…", "c.no_data" to "No data",
        "c.required" to "Required", "c.optional" to "Optional", "c.add" to "Add", "c.delete" to "Delete",
        "c.lang" to "عربي",
        "home.title" to "Home", "home.logout" to "Logout",
    )
}

/** نص مركزي بالمفتاح. */
fun tr(key: String): String = I18n.t(key)

/** نص مفرد عربي/إنجليزي دون مفتاح (للشاشات الفردية). */
fun tr(ar: String, en: String): String = if (I18n.lang == "ar") ar else en
