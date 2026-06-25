package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.masar.maintenance.data.StaffMember
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*

@Composable
fun StaffScreen(nav: NavController) {
    var selected by remember { mutableStateOf<StaffMember?>(null) }

    MasarScaffold(title = tr("متابعة الموظفين","Staff"), onBack = { nav.popBackStack() }) { pad ->
        RemoteContent(
            reloadKey = 0,
            load = { Net.repo.staffFollowup() }
        ) { list ->
            if (list.isEmpty()) {
                EmptyBox(tr("لا يوجد موظفون ميدانيون لعرضهم","No field staff to show"), "∅")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(list, key = { "${it.role}-${it.id}" }) { m ->
                        StaffCard(m) { selected = m }
                    }
                }
            }
        }
    }

    selected?.let { m ->
        StaffRequestsDialog(
            member = m,
            onDismiss = { selected = null },
            onOpenRequest = { rid -> selected = null; nav.navigate("request/$rid") }
        )
    }
}

@Composable
private fun StaffCard(m: StaffMember, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (m.overdue) RedStatus else Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!m.photo.isNullOrBlank()) {
                RemoteImage(m.photo, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)), placeholder = "👤")
            } else {
                Avatar(m.name.take(1).ifBlank { tr("؟","?") }, size = 46)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(m.roleLabel ?: Labels.role(m.role), color = Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(tr("مفتوحة: ","Open: ") + "${m.open}", if (m.open > 0) Yellow else Green)
                Spacer(Modifier.height(4.dp))
                Text(tr("الإجمالي: ","Total: ") + "${m.total}", color = Muted, fontSize = 12.sp)
                if (m.oldestDays != null && m.open > 0) {
                    Spacer(Modifier.height(4.dp))
                    val color = when {
                        m.oldestDays > 30 -> RedStatus
                        m.oldestDays > 14 -> Yellow
                        else -> Muted
                    }
                    Badge(tr("أقدم: ","Oldest: ") + "${m.oldestDays} " + tr("يوم","days"), color)
                }
            }
        }
    }
}

@Composable
private fun StaffRequestsDialog(member: StaffMember, onDismiss: () -> Unit, onOpenRequest: (Int) -> Unit) {
    var state by remember { mutableStateOf<UiState<List<RequestRow>>>(UiState.Loading) }
    LaunchedEffect(member.id, member.role) {
        state = UiState.Loading
        state = when (val r = Net.repo.requests(staffId = member.id, staffRole = member.role)) {
            is Outcome.Ok -> UiState.Data(r.data)
            is Outcome.Err -> UiState.Error(r.message)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink2,
        title = { Text(tr("طلبات: ","Requests: ") + "${member.name}", color = Txt, fontSize = 16.sp) },
        text = {
            Box(Modifier.heightIn(min = 80.dp, max = 420.dp)) {
                when (val s = state) {
                    is UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(s.message)
                    is UiState.Data -> {
                        if (s.value.isEmpty()) {
                            EmptyBox(tr("لا توجد طلبات لهذا الموظف","No requests for this employee"), "∅")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(s.value, key = { it.id }) { r ->
                                    Surface(
                                        color = Panel, shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Line),
                                        modifier = Modifier.fillMaxWidth().clickable { onOpenRequest(r.id) }
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(r.serialNo, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.weight(1f))
                                                Badge(r.statusLabel.ifBlank { Labels.status(r.status) }, Labels.statusColor(r.status))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(r.carName ?: "—", color = Txt, fontWeight = FontWeight.Bold)
                                            Text(r.plateFull ?: "", color = Muted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق","Close"), color = Red) } }
    )
}
