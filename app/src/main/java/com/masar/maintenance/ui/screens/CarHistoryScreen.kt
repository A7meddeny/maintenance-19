package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.data.Car
import com.masar.maintenance.data.InspectionRow
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.data.RequestDetail
import com.masar.maintenance.ui.tr
import kotlinx.coroutines.launch
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*

@Composable
fun CarHistoryScreen(nav: NavController, id: Int) {
    val role = Net.session.userRole

    MasarScaffold(
        title = tr("سجل السيارة","Car file"),
        onBack = { nav.popBackStack() },
        actions = {
            if (role == "admin") {
                IconButton(onClick = { nav.navigate("carForm?id=$id") }) {
                    Icon(Icons.Filled.Edit, contentDescription = tr("تعديل","Edit"), tint = Red)
                }
            }
        }
    ) { pad ->
        RemoteContent(
            reloadKey = id,
            load = { Net.repo.carHistory(id) }
        ) { (car, requests) ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                if (car != null) CarInfoCard(car)
                if (car != null) CarAnalyticsCard(car, requests.size)

                SectionTitle(tr("الطلبات","Requests") + " (${requests.size})")
                if (requests.isEmpty()) {
                    EmptyBox(tr("لا توجد طلبات على هذه السيارة","No requests for this car"), "∅")
                } else {
                    requests.forEach { r ->
                        HistoryRequestCard(r) { nav.navigate("request/${r.id}") }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                CarInspectionsSection(id, nav)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CarInfoCard(c: Car) {
    MasarCard {
        if (!c.photo.isNullOrBlank()) {
            RemoteImage(
                c.photo,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                placeholder = "🚗"
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(c.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(c.plateFull ?: "—", color = Muted, fontSize = 14.sp)

        Spacer(Modifier.height(10.dp))
        InfoRow(tr("الكود","Code"), c.carCode ?: "—")
        InfoRow(tr("الموديل","Model"), c.model ?: "—")
        InfoRow(tr("العداد","Odometer"), c.odometer?.let { tr("%,d كم","%,d km").format(it) } ?: "—")
        InfoRow(tr("تغيير الزيت القادم","Next oil change"), c.nextOilChangeKm?.let { tr("%,d كم","%,d km").format(it) } ?: "—")
        InfoRow(tr("تغيير الكفرات القادم","Next tire change"), c.nextTireChangeKm?.let { tr("%,d كم","%,d km").format(it) } ?: "—")

        Spacer(Modifier.height(8.dp))
        ExpiryRow(tr("انتهاء الاستمارة","Registration expiry"), c.registrationExpiry, c.regDays)
        ExpiryRow(tr("انتهاء التأمين","Insurance expiry"), c.insuranceExpiry, c.insDays)
        ExpiryRow(tr("انتهاء الفحص الدوري","Periodic inspection expiry"), c.inspectionExpiry, c.inspectionDays)

        if (!c.platePhoto.isNullOrBlank() || !c.registrationPhoto.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(tr("المستندات:","Documents:"), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!c.platePhoto.isNullOrBlank()) {
                    RemoteImage(
                        c.platePhoto,
                        modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(8.dp)),
                        placeholder = "🔖"
                    )
                }
                if (!c.registrationPhoto.isNullOrBlank()) {
                    RemoteImage(
                        c.registrationPhoto,
                        modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(8.dp)),
                        placeholder = "📄"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.width(150.dp))
        Text(value, color = Txt, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpiryRow(label: String, date: String?, days: Int?) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.width(150.dp))
        Text(date ?: "—", color = Txt, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (days != null) {
            Spacer(Modifier.width(8.dp))
            val txt = if (days < 0) tr("منتهٍ منذ ","Expired ") + "${-days}" + tr(" يوم"," days ago") else tr("باقٍ ","") + "$days" + tr(" يوم"," days left")
            Badge(txt, Labels.daysColor(days))
        }
    }
}

@Composable
private fun HistoryRequestCard(r: RequestDetail, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.serialNo, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Badge(r.statusLabel.ifBlank { Labels.status(r.status) }, Labels.statusColor(r.status))
            }
            if (!r.problemDescription.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(r.problemDescription, color = Txt, fontSize = 14.sp)
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val typeLabel = if (r.type == "periodic") tr("صيانة دورية","Periodic") else tr("صيانة عامة","General")
                Text(typeLabel, color = Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (r.items.isNotEmpty()) Text("${r.items.size} " + tr("بند","items"), color = Muted, fontSize = 12.sp)
                r.createdAt?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

/* ===================== بطاقة تحليلات السيارة ===================== */
@Composable
private fun CarAnalyticsCard(c: Car, reqCount: Int) {
    Spacer(Modifier.height(12.dp))
    MasarCard {
        Text(tr("التحليلات","Analytics"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            AnalyticCell(tr("إجمالي الصرف","Total spend"), c.totalCost?.let { "${fmtNum(it)} " + tr("ر.س","SAR") } ?: "—", Modifier.weight(1f))
            AnalyticCell(tr("عدد الطلبات","Requests count"), "${c.requestsCount ?: reqCount}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            AnalyticCell(tr("التكلفة / كم","Cost / km"), c.costPerKm?.let { "${fmtNum(it)} " + tr("ر.س","SAR") } ?: "—", Modifier.weight(1f))
            AnalyticCell(tr("العداد الحالي","Current odometer"), c.odometer?.let { tr("%,d كم","%,d km").format(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun AnalyticCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/* ===================== قسم تقارير الفحص للسيارة ===================== */
@Composable
private fun CarInspectionsSection(carId: Int, nav: NavController) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<InspectionRow>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(carId) {
        when (val r = Net.repo.inspections(carId = carId)) {
            is Outcome.Ok -> { rows = r.data; loaded = true }
            is Outcome.Err -> { loaded = true }
        }
    }

    Spacer(Modifier.height(8.dp))
    SectionTitle(tr("تقارير الفحص","Inspection reports") + " (${rows.size})")
    if (!loaded) {
        LoadingBox()
    } else if (rows.isEmpty()) {
        EmptyBox(tr("لا توجد تقارير فحص لهذه السيارة","No inspection reports for this car"), "∅")
    } else {
        rows.forEach { x ->
            CarInspRow(x) { nav.navigate("inspection/${x.id}") }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CarInspRow(x: InspectionRow, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(x.reportNo, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (!x.description.isNullOrBlank())
                    Text(x.description ?: "", color = Txt, fontSize = 13.sp, maxLines = 2)
                x.createdAt?.let { Text(it, color = Muted, fontSize = 11.sp) }
            }
            Badge(
                if (x.status == "done") tr("مكتمل","Completed") else tr("بانتظار التعبئة","Pending"),
                if (x.status == "done") Green else Yellow
            )
        }
    }
}
