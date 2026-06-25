package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.ui.tr
import com.masar.maintenance.data.Car
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.data.User
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NewRequestScreen(nav: NavController) {
    val role = Net.session.userRole
    val isPeriodic = role == "maintenance"

    MasarScaffold(
        title = if (isPeriodic) tr("صيانة دورية","Periodic maintenance") else tr("طلب صيانة جديد","New maintenance request"),
        onBack = { nav.popBackStack() }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (isPeriodic) PeriodicForm(nav) else OfficeCreateForm(nav)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ============ نموذج المكتب: إنشاء طلب صيانة ============ */
@Composable
private fun OfficeCreateForm(nav: NavController) {
    val scope = rememberCoroutineScope()

    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var staff by remember { mutableStateOf<List<User>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadingLists by remember { mutableStateOf(true) }

    var car by remember { mutableStateOf<Car?>(null) }
    var maintId by remember { mutableStateOf(0) }
    var desc by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<com.masar.maintenance.data.UploadFile?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loadingLists = true
        val cr = Net.repo.cars(state = "available")
        val sr = Net.repo.staff("maintenance")
        when (cr) {
            is Outcome.Ok -> cars = cr.data
            is Outcome.Err -> loadError = cr.message
        }
        when (sr) {
            is Outcome.Ok -> staff = sr.data
            is Outcome.Err -> if (loadError == null) loadError = sr.message
        }
        loadingLists = false
    }

    if (loadingLists) { LoadingBox(); return }
    if (loadError != null) { ErrorBox(loadError!!); return }

    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("بيانات الطلب","Request details"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text(error!!, color = RedStatus, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }

            CarSelector(cars, car) { car = it }
            Spacer(Modifier.height(12.dp))

            if (staff.isEmpty()) {
                Text(tr("لا يوجد موظفو صيانة مسجّلون بعد. أضِفهم من قسم الموظفين.","No maintenance staff yet. Add them from Employees."), color = Yellow, fontSize = 13.sp)
            } else {
                MaintPicker(staff, maintId) { maintId = it }
            }
            Spacer(Modifier.height(12.dp))

            MasarField(desc, { desc = it }, tr("وصف العطل","Fault description"), singleLine = false)
            Spacer(Modifier.height(12.dp))
            MasarField(odometer, { odometer = it }, tr("قراءة العداد عند التسليم (اختياري)","Odometer at handover (optional)"), keyboard = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            PhotoPickerField(tr("صورة العطل (اختياري)","Fault photo (optional)"), photo, { photo = it })
            Spacer(Modifier.height(16.dp))

            PrimaryButton(tr("إنشاء الطلب","Create request"), loading = submitting) {
                error = null
                when {
                    car == null -> error = tr("اختر السيارة أولاً","Select the car first")
                    maintId <= 0 -> error = tr("اختر موظف الصيانة المسؤول","Select the responsible maintenance staff")
                    desc.isBlank() -> error = tr("اكتب وصف العطل","Enter the fault description")
                    else -> {
                        submitting = true
                        scope.launch {
                            when (val r = Net.repo.officeCreate(car!!.id, maintId, desc, odometer, photo)) {
                                is Outcome.Ok -> { submitting = false; nav.popBackStack() }
                                is Outcome.Err -> { submitting = false; error = r.message }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ============ نموذج الصيانة: بدء صيانة دورية ============ */
@Composable
private fun PeriodicForm(nav: NavController) {
    val scope = rememberCoroutineScope()

    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadingLists by remember { mutableStateOf(true) }

    var car by remember { mutableStateOf<Car?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loadingLists = true
        when (val cr = Net.repo.cars(state = "available")) {
            is Outcome.Ok -> cars = cr.data
            is Outcome.Err -> loadError = cr.message
        }
        loadingLists = false
    }

    if (loadingLists) { LoadingBox(); return }
    if (loadError != null) { ErrorBox(loadError!!); return }

    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("بدء صيانة دورية لسيارة","Start periodic maintenance for a car"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(tr("اختر السيارة وسيتم فتح طلب صيانة دورية باسمك مباشرةً.","Select the car; a periodic request will open under your name."), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))

            if (error != null) {
                Text(error!!, color = RedStatus, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }

            CarSelector(cars, car) { car = it }
            Spacer(Modifier.height(16.dp))

            PrimaryButton(tr("بدء الصيانة الدورية","Start periodic maintenance"), loading = submitting) {
                error = null
                if (car == null) { error = tr("اختر السيارة أولاً","Select the car first"); return@PrimaryButton }
                submitting = true
                scope.launch {
                    when (val r = Net.repo.periodicSelf(car!!.id)) {
                        is Outcome.Ok -> { submitting = false; nav.popBackStack() }
                        is Outcome.Err -> { submitting = false; error = r.message }
                    }
                }
            }
        }
    }
}

/* ============ منتقي السيارة (بحث + قائمة) ============ */
@Composable
internal fun CarSelector(cars: List<Car>, selected: Car?, onSelect: (Car) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var qrError by remember { mutableStateOf<String?>(null) }
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val token = res.contents
        if (!token.isNullOrBlank()) {
            scope.launch {
                when (val r = Net.repo.carByQr(token)) {
                    is Outcome.Ok -> { onSelect(r.data); open = false; qrError = null }
                    is Outcome.Err -> { qrError = r.message }
                }
            }
        }
    }
    fun scanCarQr() {
        qrLauncher.launch(ScanOptions().apply {
            setPrompt(tr("وجّه الكاميرا نحو رمز QR للسيارة","Point the camera at the car QR"))
            setBeepEnabled(true); setOrientationLocked(true)
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setCaptureActivity(com.masar.maintenance.SquareCaptureActivity::class.java)
        })
    }

    Column {
        Text(tr("السيارة","Car"), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        GhostButton(
            text = selected?.let { "${it.name} — ${it.plateFull ?: ""}" } ?: tr("— اختر السيارة —","— Select car —"),
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (open) {
        var q by remember { mutableStateOf("") }
        val filtered = remember(q, cars) {
            if (q.isBlank()) cars
            else cars.filter {
                it.name.contains(q, true) ||
                (it.plateFull ?: "").contains(q, true) ||
                (it.carCode ?: "").contains(q, true) ||
                it.id.toString().contains(q)
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = Ink2,
            title = { Text(tr("اختر السيارة","Select car"), color = Txt) },
            text = {
                Column {
                    GhostButton(tr("🔳 مسح رمز QR للسيارة","🔳 Scan car QR"), onClick = { scanCarQr() }, modifier = Modifier.fillMaxWidth())
                    qrError?.let { Spacer(Modifier.height(6.dp)); Text(it, color = RedStatus, fontSize = 12.sp) }
                    Spacer(Modifier.height(10.dp))
                    MasarField(q, { q = it }, tr("ابحث بالاسم أو اللوحة أو الكود…","Search by name, plate or code…"))
                    Spacer(Modifier.height(10.dp))
                    if (filtered.isEmpty()) {
                        EmptyBox(tr("لا توجد سيارات متاحة مطابقة","No matching available cars"), "∅")
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id }) { c ->
                                Surface(
                                    color = Panel, shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Line),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onSelect(c); open = false
                                    }
                                ) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RemoteImage(c.photo, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(c.name, color = Txt, fontWeight = FontWeight.Bold)
                                            Text(
                                                buildString {
                                                    append(c.plateFull ?: "—")
                                                    c.carCode?.let { append(" · " + tr("كود","Code") + ": $it") }
                                                },
                                                color = Muted, fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Text(tr("إغلاق","Close"), color = Red) } }
        )
    }
}

/* ============ منتقي موظف الصيانة (بالصور) ============ */
@Composable
internal fun MaintPicker(staff: List<User>, selectedId: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val sel = staff.firstOrNull { it.id == selectedId }
    Column {
        Text(tr("موظف الصيانة المسؤول","Responsible maintenance staff"), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        GhostButton(
            text = sel?.name ?: tr("— اختر الموظف —","— Select staff —"),
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = Ink2,
            title = { Text(tr("اختر موظف الصيانة","Select maintenance staff"), color = Txt) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(staff, key = { it.id }) { e ->
                        Surface(
                            color = Panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(e.id); open = false }
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!e.photo.isNullOrBlank()) {
                                    RemoteImage(e.photo, modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape))
                                } else {
                                    Avatar(e.name.take(1).ifBlank { tr("م","C") })
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(e.name, color = Txt, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Text(tr("إغلاق","Close"), color = Red) } }
        )
    }
}
