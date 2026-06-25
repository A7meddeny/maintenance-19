package com.masar.maintenance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.ui.tr
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.data.RequestRow
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*

private val STATUS_ORDER = listOf(
    "new_office", "maintenance_received", "sent_to_purchasing", "purchasing_received",
    "sent_to_admin", "admin_approved", "purchasing_buying", "part_delivered",
    "maintenance_fixing", "completed"
)

@Composable
fun RequestsScreen(nav: NavController, scope: String) {
    val role = Net.session.userRole
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }

    val title = if (scope == "purchasing_followup") tr("متابعة المشتريات","Purchasing") else tr("متابعة الطلبات","Requests")

    MasarScaffold(
        title = title,
        onBack = { nav.popBackStack() }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                // أدوات البحث والفلترة
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                    MasarField(query, { query = it }, tr("ابحث بالرقم أو السيارة…","Search by number or car…"))
                    if (scope != "purchasing_followup") {
                        Spacer(Modifier.height(8.dp))
                        StatusFilter(status) { status = it }
                    }
                }

                RemoteContent(
                    reloadKey = "$query|$status|$scope|$reload",
                    load = {
                        Net.repo.requests(q = query, status = status, scope = scope,
                            excludePeriodic = false)
                    }
                ) { rows ->
                    if (rows.isEmpty()) {
                        EmptyBox(tr("لا توجد طلبات مطابقة","No matching requests"), "∅")
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                        ) {
                            items(rows, key = { it.id }) { r ->
                                RequestRowCard(r) {
                                    if (r.type == "periodic" && r.status != "completed" && role == "maintenance")
                                        nav.navigate("periodic/${r.id}")
                                    else nav.navigate("request/${r.id}")
                                }
                            }
                        }
                    }
                }
            }

            // أزرار الإنشاء حسب الدور
            if (scope != "purchasing_followup" && role in listOf("office", "maintenance", "admin")) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(if (role == "maintenance") "periodic" else "newRequest") },
                    containerColor = Red,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(if (role == "maintenance") tr("صيانة دورية","Periodic") else tr("طلب صيانة","Maintenance request")) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusFilter(status: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = if (status.isBlank()) tr("كل المراحل","All stages") else Labels.status(status)
    Box {
        GhostButton(text = tr("المرحلة: ","Stage: ") + "$label", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(tr("كل المراحل","All stages")) }, onClick = { onChange(""); open = false })
            STATUS_ORDER.forEach { s ->
                DropdownMenuItem(text = { Text(Labels.status(s)) }, onClick = { onChange(s); open = false })
            }
        }
    }
}

@Composable
private fun RequestRowCard(r: RequestRow, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.serialNo, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Badge(r.statusLabel.ifBlank { Labels.status(r.status) }, Labels.statusColor(r.status))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(
                    r.carPhoto,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.carName ?: "—", color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.plateFull ?: "", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                val typeLabel = if (r.type == "periodic") tr("صيانة دورية","Periodic") else tr("صيانة عامة","General")
                Text(typeLabel, color = Muted, fontSize = 12.sp)
                        if (r.status != "completed" && r.ageDays != null) {
                            Spacer(Modifier.width(8.dp))
                            val ageColor = when {
                                r.ageDays > 30 -> RedStatus
                                r.ageDays > 14 -> Yellow
                                else -> Muted
                            }
                            Badge("${r.ageDays} " + tr("يوم","days"), ageColor)
                        }
                    }
                }
            }
        }
    }
}
