package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import com.masar.maintenance.ui.tr
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(nav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tasks by remember { mutableStateOf<List<MaintTask>>(emptyList()) }
    var showDone by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<MaintTask?>(null) }

    LaunchedEffect(reloadKey, showDone) {
        loading = true
        when (val r = Net.repo.myTasks(if (showDone) "done" else "pending")) {
            is Outcome.Ok -> { tasks = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }

    val sel = selected
    if (sel != null) {
        TaskDetailSheet(sel, onClose = { selected = null }, onDone = { selected = null; reloadKey++ })
        return
    }

    MasarScaffold(title = tr("المهام الموكلة", "Assigned tasks"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(tr("قيد التنفيذ", "Pending"), !showDone) { showDone = false }
                ChoiceChip(tr("المنفّذة", "Done"), showDone) { showDone = true }
            }
            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!) { reloadKey++ }
                tasks.isEmpty() -> EmptyBox(if (showDone) tr("لا مهام منفّذة", "No completed tasks") else tr("لا توجد مهام موكلة لك", "No tasks assigned to you"), "✓")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks) { t -> TaskRow(t) { selected = t } }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(t: MaintTask, onClick: () -> Unit) {
    val where = t.title ?: t.carName?.let { it + (t.plateFull?.let { p -> " · $p" } ?: "") }
        ?: t.customerName ?: t.place1Name ?: "—"
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(taskIcon(t.kind), fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.kindLabel ?: t.kind, color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(where, color = Muted, fontSize = 12.sp)
                t.createdByName?.let { Text(tr("من: ", "From: ") + it, color = Muted, fontSize = 11.sp) }
            }
            if (t.status == "pending") Badge(tr("جديد", "New"), Red) else Badge(tr("منفّذ", "Done"), Green)
        }
    }
}

private fun taskIcon(kind: String) = when (kind) {
    "branch_transfer" -> "🚗"; "part_pickup" -> "📦"
    "customer_delivery" -> "🧍"; "customer_pickup" -> "🔁"
    "periodic_inspection" -> "🔧"; "general" -> "📋"; else -> "📋"
}

@Composable
private fun TaskDetailSheet(t: MaintTask, onClose: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var doneNote by remember { mutableStateOf("") }
    var donePhoto by remember { mutableStateOf<UploadFile?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    MasarScaffold(title = t.kindLabel ?: tr("تفاصيل الطلب", "Task details"), onBack = onClose) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            err?.let {
                Surface(color = RedStatus.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = RedStatus, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            MasarCard {
                t.title?.let { DetailLine(tr("اسم الطلب", "Task name"), it); Spacer(Modifier.height(4.dp)) }
                t.carName?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(t.carPhoto, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(10.dp))
                        Column { Text(it, color = Txt, fontWeight = FontWeight.Bold); Text(t.plateFull ?: "", color = Muted, fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                DetailLine(tr("من فرع", "From branch"), t.fromBranch)
                DetailLine(tr("إلى فرع", "To branch"), t.toBranch)
                DetailLine(tr("إلى شركة", "To company"), t.toCompany?.let { it + (t.companyBranch?.let { b -> " — $b" } ?: "") })
                DetailLine(tr("العميل", "Customer"), t.customerName)
                DetailLine(tr("المكان الأول", "First place"), t.place1Name)
                DetailLine(tr("المكان الثاني", "Second place"), t.place2Name)
                DetailLine(tr("العمل", "Work"), t.workType)
                DetailLine(tr("ملاحظة", "Note"), t.note)
                t.createdByName?.let { DetailLine(tr("من", "From"), it) }
            }

            // روابط الخرائط القابلة للفتح
            t.place1Maps?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(10.dp)); MapsLink(it, tr("موقع المكان الأول", "First place location")) }
            t.place2Maps?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(8.dp)); MapsLink(it, tr("موقع المكان الثاني", "Second place location")) }
            t.destMapsBest?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(8.dp)); MapsLink(it, tr("موقع الوجهة", "Destination location")) }

            t.photo?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                Text(tr("مرفق من الإدارة:", "Attached by admin:"), color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                RemoteImage(it, modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp)))
            }

            if (t.status == "pending") {
                Spacer(Modifier.height(18.dp))
                SectionTitle(tr("تنفيذ الطلب", "Complete the task"))
                Spacer(Modifier.height(8.dp))
                MasarField(doneNote, { doneNote = it }, tr("ملاحظة التنفيذ (اختياري)", "Completion note (optional)"), singleLine = false)
                Spacer(Modifier.height(10.dp))
                PhotoPickerField(tr("صورة بعد التنفيذ (اختياري)", "Photo after completion (optional)"), donePhoto, { donePhoto = it })
                Spacer(Modifier.height(16.dp))
                PrimaryButton(tr("تم التنفيذ ✓", "Mark as done ✓"), loading = submitting) {
                    submitting = true; err = null
                    scope.launch {
                        val r = Net.repo.taskComplete(t.id, doneNote, donePhoto)
                        submitting = false
                        when (r) { is Outcome.Ok -> onDone(); is Outcome.Err -> err = r.message }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("$label: ", color = Muted, fontSize = 13.sp)
        Text(value, color = Txt, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}
