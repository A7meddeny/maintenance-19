package com.masar.maintenance.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import com.masar.maintenance.ui.tr
import kotlinx.coroutines.launch

/* ===== حامل حالة سيارة مختارة داخل عمل يومي ===== */
class SelCar(val car: Car) {
    var driverSelf by mutableStateOf(true)
    var driverName by mutableStateOf("")
    var odometer by mutableStateOf(car.odometer?.toString() ?: "")
    var photo by mutableStateOf<UploadFile?>(null)
    var driverPhoto by mutableStateOf<UploadFile?>(null)
}

/* ===================== الشاشة الرئيسية للأعمال اليومية ===================== */
@Composable
fun DailyWorksScreen(nav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var works by remember { mutableStateOf<List<DailyWork>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        when (val r = Net.repo.dailyWorksList()) {
            is Outcome.Ok -> { works = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }

    MasarScaffold(title = tr("الأعمال اليومية", "Daily works"), onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(tr("سجّل عملك اليوم", "Log your work today"), color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
            }
            item {
                ActionTile("🚗", tr("نقل سيارة", "Transfer a car"),
                    tr("نقل سيارة أو مجموعة بين الفروع أو لشركة", "Move one or many cars between branches or to a company")) {
                    nav.navigate("dwTransfer")
                }
            }
            item {
                ActionTile("📦", tr("استلام قطعة غيار", "Pick up a part"),
                    tr("استلام قطعة من مورد ونقلها", "Receive a part from a supplier and deliver it")) {
                    nav.navigate("dwPart")
                }
            }
            item {
                ActionTile("🧍", tr("توصيل سيارة لعميل", "Deliver a car to a customer"),
                    tr("تسليم سيارة لعميل في موقعه", "Hand a car to a customer at their location")) {
                    nav.navigate("dwDelivery")
                }
            }
            item {
                ActionTile("📝", tr("ملاحظة / عمل", "Note / task"),
                    tr("تسجيل عمل أو ملاحظة مع صورة", "Log any task or note with a photo")) {
                    nav.navigate("dwNote")
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle(tr("أعمالي الأخيرة", "My recent works"))
            }
            when {
                loading -> item { LoadingBox() }
                error != null -> item { ErrorBox(error!!) }
                works.isEmpty() -> item { EmptyBox(tr("لا توجد أعمال مسجّلة بعد", "No works logged yet"), "∅") }
                else -> items(works) { w -> DailyWorkRow(w) }
            }
        }
    }
}

@Composable
private fun ActionTile(icon: String, title: String, sub: String, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sub, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Muted, fontSize = 22.sp)
        }
    }
}

@Composable
private fun DailyWorkRow(w: DailyWork) {
    val dest = w.toCompany?.let { tr("إلى شركة: ", "To company: ") + it }
        ?: w.toBranch?.let { tr("إلى فرع: ", "To branch: ") + it }
        ?: w.customerName?.let { tr("عميل: ", "Customer: ") + it }
        ?: w.place1Name?.let { tr("المكان: ", "Place: ") + it }
        ?: w.workType
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w.kindLabel ?: w.kind, color = Txt, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (w.carCount > 0) Badge("${w.carCount} " + tr("سيارة", "cars"), Blue)
            }
            (w.createdAt ?: w.workDate)?.let {
                Text(it.take(16), color = Muted, fontSize = 11.sp)
            }
            dest?.let { Text(it, color = Txt, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            w.note?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
    }
}

/* ===================== منتقي السيارات (بحث + QR + متعدد) ===================== */
@Composable
fun CarMultiPicker(
    selected: SnapshotStateList<SelCar>,
    multi: Boolean,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Car>>(emptyList()) }

    fun addCar(c: Car) {
        if (selected.any { it.car.id == c.id }) return
        if (!multi) selected.clear()
        selected.add(SelCar(c))
        results = emptyList(); query = ""
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val token = res.contents
        if (!token.isNullOrBlank()) scope.launch {
            when (val r = Net.repo.carByQr(token)) {
                is Outcome.Ok -> addCar(r.data)
                is Outcome.Err -> onError(r.message)
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            MasarField(query, {
                query = it
                scope.launch {
                    if (it.isBlank()) { results = emptyList(); return@launch }
                    when (val r = Net.repo.cars(it, "")) { is Outcome.Ok -> results = r.data; is Outcome.Err -> {} }
                }
            }, tr("ابحث عن سيارة بالاسم أو اللوحة…", "Search car by name or plate…"))
        }
        GhostButton("⛶ QR", onClick = {
            qrLauncher.launch(ScanOptions().apply {
                setPrompt(tr("امسح رمز QR للسيارة", "Scan the car QR"))
                setBeepEnabled(true); setOrientationLocked(true)
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setCaptureActivity(com.masar.maintenance.SquareCaptureActivity::class.java)
            })
        })
    }
    if (results.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        results.take(20).forEach { c ->
            Surface(
                color = Panel2, shape = RoundedCornerShape(9.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { addCar(c) }
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(c.photo, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(7.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = Txt, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(c.plateFull ?: "", color = Muted, fontSize = 12.sp)
                    }
                    Text("＋", color = Red, fontSize = 18.sp)
                }
            }
        }
    }
}

/* ===================== قائمة منسدلة عامة ===================== */
@Composable
fun <T> DropdownPicker(
    label: String,
    items: List<T>,
    selectedId: Int,
    idOf: (T) -> Int,
    nameOf: (T) -> String,
    emptyLabel: String,
    onPick: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Text(label, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Box {
        Surface(
            color = Panel2, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth().clickable { open = true }
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    items.firstOrNull { idOf(it) == selectedId }?.let(nameOf) ?: emptyLabel,
                    color = if (selectedId == 0) Muted else Txt, modifier = Modifier.weight(1f), fontSize = 14.sp
                )
                Text("▾", color = Muted)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { onPick(0); open = false })
            items.forEach { it2 ->
                DropdownMenuItem(text = { Text(nameOf(it2)) }, onClick = { onPick(idOf(it2)); open = false })
            }
        }
    }
}
