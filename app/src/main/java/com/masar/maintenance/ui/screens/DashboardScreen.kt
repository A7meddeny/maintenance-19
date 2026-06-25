package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.ui.tr
import com.masar.maintenance.data.CarAlert
import com.masar.maintenance.data.IqamaAlert
import com.masar.maintenance.data.Kpis
import com.masar.maintenance.data.Net
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*

@Composable
fun DashboardScreen(nav: NavController) {
    MasarScaffold(title = tr("لوحة المؤشرات","Dashboard"), onBack = { nav.popBackStack() }) { pad ->
        RemoteContent(reloadKey = 0, load = { Net.repo.kpis() }) { k ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                KpiGrid(k)

                if (k.byStatus.isNotEmpty()) {
                    SectionTitle(tr("توزيع الطلبات حسب المرحلة","Requests by stage"))
                    StatusBreakdown(k)
                }

                if (k.regAlerts.isNotEmpty()) {
                    SectionTitle(tr("استمارات تقترب من الانتهاء","Registrations expiring soon"))
                    k.regAlerts.forEach { CarAlertRow(it, suffix = tr("الاستمارة","Registration")) }
                }
                if (k.insAlerts.isNotEmpty()) {
                    SectionTitle(tr("تأمينات تقترب من الانتهاء","Insurances expiring soon"))
                    k.insAlerts.forEach { CarAlertRow(it, suffix = tr("التأمين","Insurance")) }
                }
                if (k.oilAlerts.isNotEmpty()) {
                    SectionTitle(tr("سيارات تحتاج تغيير زيت قريباً","Cars needing oil change soon"))
                    k.oilAlerts.forEach { OilAlertRow(it) }
                }
                if (k.iqamaAlerts.isNotEmpty()) {
                    SectionTitle(tr("إقامات تقترب من الانتهاء","Iqamas expiring soon"))
                    k.iqamaAlerts.forEach { IqamaAlertRow(it) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun KpiGrid(k: Kpis) {
    val cards = listOf(
        Triple(tr("إجمالي السيارات","Total cars"), k.totalCars.toString(), Blue),
        Triple(tr("متاحة","Available"), k.available.toString(), Green),
        Triple(tr("تحت الصيانة","Under maintenance"), k.underMaintenance.toString(), Yellow),
        Triple(tr("طلبات مفتوحة","Open requests"), k.openRequests.toString(), Red),
        Triple(tr("بانتظار الإدارة","Awaiting admin"), k.pendingAdmin.toString(), Blue),
        Triple(tr("سيارات حرجة","Critical cars"), k.redCars.toString(), RedStatus),
        Triple(tr("الموظفون","Employees"), k.employees.toString(), Muted),
        Triple(tr("الشركات","Companies"), k.companies.toString(), Muted)
    )
    cards.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { (label, value, color) ->
                KpiCard(label, value, color, Modifier.weight(1f))
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line), modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StatusBreakdown(k: Kpis) {
    val order = listOf(
        "new_office", "maintenance_received", "sent_to_purchasing", "purchasing_received",
        "sent_to_admin", "admin_approved", "purchasing_buying", "part_delivered",
        "maintenance_fixing", "completed"
    )
    MasarCard {
        val keys = order.filter { k.byStatus.containsKey(it) } + k.byStatus.keys.filter { it !in order }
        keys.forEachIndexed { i, key ->
            val item = k.byStatus[key] ?: return@forEachIndexed
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.label.ifBlank { Labels.status(key) }, color = Txt, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Badge(item.count.toString(), Labels.statusColor(key))
            }
        }
    }
}

@Composable
private fun CarAlertRow(a: CarAlert, suffix: String) {
    Spacer(Modifier.height(8.dp))
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name ?: "—", color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(a.plateFull ?: "", color = Muted, fontSize = 12.sp)
            }
            val days = a.days
            val txt = when {
                days == null -> suffix
                days < 0 -> "$suffix " + tr("منتهٍ منذ ${-days} يوم","expired ${-days} days ago")
                else -> "$suffix " + tr(tr("خلال $days يوم","in $days days"),"in $days days")
            }
            Badge(txt, Labels.daysColor(days))
        }
    }
}

@Composable
private fun OilAlertRow(a: CarAlert) {
    Spacer(Modifier.height(8.dp))
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name ?: "—", color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(a.plateFull ?: "", color = Muted, fontSize = 12.sp)
            }
            val rem = a.remaining
            val txt = when {
                rem == null -> tr("تغيير الزيت","Oil change")
                rem <= 0 -> tr("تجاوز بـ ${-rem} كم","over by ${-rem} km")
                else -> tr("باقٍ $rem كم","$rem km left")
            }
            val color = when {
                rem == null -> Muted
                rem <= 0 -> RedStatus
                rem <= 500 -> Yellow
                else -> Green
            }
            Badge(txt, color)
        }
    }
}

@Composable
private fun IqamaAlertRow(a: IqamaAlert) {
    Spacer(Modifier.height(8.dp))
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name ?: "—", color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                a.employeeCode?.let { Text(tr("كود: ","Code: ") + "$it", color = Muted, fontSize = 12.sp) }
            }
            val days = a.days
            val txt = when {
                days == null -> tr("الإقامة","Iqama")
                days < 0 -> tr("منتهية منذ ${-days} يوم","expired ${-days} days ago")
                else -> tr("خلال $days يوم","in $days days")
            }
            Badge(txt, Labels.daysColor(days))
        }
    }
}
