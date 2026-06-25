package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Noti
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.I18n
import com.masar.maintenance.ui.tr
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

private data class HomeItem(val label: String, val route: String, val icon: String, val desc: String)

@Composable
fun HomeScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val role = Net.session.userRole
    val name = Net.session.userName

    val items = remember(role, I18n.lang) { buildItems(role) }
    var notis by remember { mutableStateOf<List<Noti>>(emptyList()) }
    var showNotis by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (val r = Net.repo.notifications()) { is Outcome.Ok -> notis = r.data; is Outcome.Err -> {} }
    }

    MasarScaffold(
        title = tr("نظام مسار للصيانة", "MASAR Maintenance"),
        actions = {
            TextButton(onClick = { I18n.toggle() }) {
                Text(tr("c.lang"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Box {
                val bellAnim = rememberInfiniteTransition(label = "bell")
                val angle by bellAnim.animateFloat(
                    initialValue = 0f, targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1500
                            0f at 0; -14f at 90; 14f at 180; -10f at 270; 10f at 360; 0f at 450; 0f at 1500
                        },
                        repeatMode = RepeatMode.Restart
                    ), label = "angle"
                )
                val shake = if (notis.isNotEmpty()) angle else 0f
                IconButton(onClick = { showNotis = true }) {
                    Icon(
                        Icons.Filled.Notifications, contentDescription = tr("الإشعارات","Notifications"), tint = Txt,
                        modifier = Modifier.graphicsLayer { rotationZ = shake }
                    )
                }
                if (notis.isNotEmpty()) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp)
                            .size(18.dp).clip(CircleShape).background(Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (notis.size > 9) "9+" else notis.size.toString(), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = {
                scope.launch {
                    Net.repo.logout()
                    nav.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            }) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = tr("خروج","Logout"), tint = Red) }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MasarCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photo = Net.session.userPhoto
                        if (!photo.isNullOrBlank()) {
                            RemoteImage(
                                photo,
                                modifier = Modifier.size(44.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                            )
                        } else {
                            Avatar(name.take(1).ifBlank { tr("م","U") })
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name.ifBlank { tr("مستخدم", "User") }, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Txt)
                            Text(Labels.role(role), color = Muted)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            items(items) { it -> HomeRow(it) { nav.navigate(it.route) } }
        }
    }

    if (showNotis) {
        AlertDialog(
            onDismissRequest = { showNotis = false },
            containerColor = Ink2,
            title = { Text(tr("الإشعارات", "Notifications") + " (${notis.size})", color = Txt) },
            text = {
                if (notis.isEmpty()) Text(tr("لا توجد تنبيهات حالياً ✓", "No notifications ✓"), color = Muted)
                else LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notis) { n ->
                        val c = when (n.color) { "green" -> Green; "yellow" -> Yellow; else -> RedStatus }
                        Surface(
                            color = Panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showNotis = false
                                if (n.requestId > 0) nav.navigate("request/${n.requestId}")
                            }
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(n.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(n.title, color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (n.sub.isNotBlank()) Text(n.sub, color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotis = false }) { Text(tr("c.close"), color = Red) } }
        )
    }
}

@Composable
private fun HomeRow(item: HomeItem, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Txt)
                Text(item.desc, color = Muted, fontSize = 13.sp)
            }
            Text("‹", color = Muted, fontSize = 22.sp)
        }
    }
}

private fun buildItems(role: String): List<HomeItem> {
    val list = mutableListOf<HomeItem>()
    if (role != "car_follow") {
        list += HomeItem(tr("متابعة الطلبات", "Requests"), "requests?scope=", "⇄", tr("كل طلبات الصيانة وحالتها", "All maintenance requests & status"))
    }
    list += HomeItem(tr("سجل السيارات", "Cars"), "cars", "⛍", tr("بحث وعرض السيارات", "Search & view cars"))
    list += HomeItem(tr("مسح رمز سيارة (QR)", "Scan car QR"), "scan", "🔳", tr("افتح ملف السيارة بمسح الرمز الملصق عليها", "Open the car file by scanning its QR"))
    when (role) {
        "office" -> {
            list += HomeItem(tr("إنشاء طلب صيانة", "New request"), "newRequest", "＋", tr("إدخال عطل سيارة وإسناده للصيانة", "Report a car issue and assign to maintenance"))
            list += HomeItem(tr("تأجير سيارة", "Rent a car"), "rentCar", "⛐", tr("تفويض سيارة لمستأجر وإسناد موظف متابعة", "Delegate a car to a renter and assign a tracking employee"))
        }
        "maintenance" -> {
            list += HomeItem(tr("المهام الموكلة", "Assigned tasks"), "tasks", "📋", tr("طلبات من الإدارة: نقل، قطعة، توصيل", "Admin tasks: transfer, part, delivery"))
            list += HomeItem(tr("الأعمال اليومية", "Daily works"), "dailyWorks", "🗓", tr("نقل سيارة، توصيل، استلام قطعة، أو ملاحظة", "Transfer, delivery, part pickup, or a note"))
            list += HomeItem(tr("صيانة دورية", "Periodic maintenance"), "periodic", "🛢", tr("غيار زيت/كفرات/بطارية — تُغلق من الصيانة", "Oil/tires/battery — closed by maintenance"))
            list += HomeItem(tr("تقارير الفحص", "Inspection reports"), "inspections", "📋", tr("تقارير فحص طلبتها الإدارة", "Inspection reports requested by admin"))
        }
        "purchasing" -> list += HomeItem(tr("متابعة المشتريات", "Purchasing"), "requests?scope=purchasing_followup", "₪", tr("الطلبات المنتظرة للشراء", "Requests awaiting purchase"))
        "car_follow" -> {
            list += HomeItem(tr("سيارات للفحص", "Cars to inspect"), "rentalInbox", "📝", tr("السيارات المؤجَّرة المُسندة إليك لعمل تقرير الفحص", "Rented cars assigned to you for an inspection report"))
            list += HomeItem(tr("إضافة سيارة جديدة", "Add a new car"), "carForm?id=0", "＋", tr("تسجيل سيارة جديدة في النظام", "Register a new car in the system"))
            list += HomeItem(tr("تصحيح عداد سيارة", "Correct car odometer"), "scan", "📟", tr("امسح رمز السيارة لمراجعة العداد وتصحيحه", "Scan the car QR to review & correct the odometer"))
        }
        "admin" -> {
            list += HomeItem(tr("تقرير سيارة", "Car report"), "carReport", "📑", tr("بحث/QR لعرض طلبات السيارة وفحوصها وتحليلاتها", "Search/QR for a car's requests, inspections & analytics"))
            list += HomeItem(tr("إنشاء طلب صيانة", "New request"), "newRequest", "＋", tr("إدخال عطل سيارة وإسناده للصيانة", "Report a car issue and assign to maintenance"))
            list += HomeItem(tr("لوحة المعلومات", "Dashboard"), "dashboard", "▣", tr("المؤشرات والتنبيهات", "KPIs & alerts"))
            list += HomeItem(tr("متابعة المشتريات", "Purchasing"), "requests?scope=purchasing_followup", "₪", tr("الطلبات المنتظرة للشراء", "Requests awaiting purchase"))
            list += HomeItem(tr("متابعة الموظفين", "Staff"), "staff", "☖", tr("طلبات كل موظف وتأخيره", "Each employee's requests & delays"))
            list += HomeItem(tr("الموظفون", "Employees"), "employees", "⛁", tr("إدارة حسابات الموظفين", "Manage employee accounts"))
            list += HomeItem(tr("الشركات المورّدة", "Suppliers"), "companies", "⌂", tr("شركات قطع الغيار", "Spare-parts companies"))
        }
    }
    return list.distinctBy { it.route + it.label }
}
