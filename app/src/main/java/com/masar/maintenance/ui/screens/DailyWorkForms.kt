package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import com.masar.maintenance.ui.tr
import kotlinx.coroutines.launch

private fun carsJson(list: List<SelCar>): String {
    val arr = JsonArray()
    list.forEach { sc ->
        val o = JsonObject()
        o.addProperty("car_id", sc.car.id)
        o.addProperty("driver_self", if (sc.driverSelf) 1 else 0)
        o.addProperty("driver_name", sc.driverName)
        o.addProperty("odometer", sc.odometer)
        arr.add(o)
    }
    return arr.toString()
}

@Composable
private fun ErrorBanner(msg: String?) {
    msg ?: return
    Surface(color = RedStatus.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(msg, color = RedStatus, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
    }
    Spacer(Modifier.height(12.dp))
}

/* ===================== نقل سيارة ===================== */
@Composable
fun DailyTransferScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf(false) }
    val cars = remember { mutableStateListOf<SelCar>() }

    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var companies by remember { mutableStateOf<List<RentalCompany>>(emptyList()) }
    var fromBranch by remember { mutableStateOf(0) }
    var destIsCompany by remember { mutableStateOf(false) }
    var toBranch by remember { mutableStateOf(0) }
    var toCompany by remember { mutableStateOf(0) }

    var note by remember { mutableStateOf("") }
    var mainPhoto by remember { mutableStateOf<UploadFile?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val r = Net.repo.branches()) { is Outcome.Ok -> branches = r.data; else -> {} }
        when (val r = Net.repo.rentalCompanies()) { is Outcome.Ok -> companies = r.data; else -> {} }
    }

    MasarScaffold(title = tr("نقل سيارة", "Transfer a car"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ErrorBanner(err)

            // فردي / مجموعة
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(tr("سيارة واحدة", "Single car"), !group) { group = false; if (cars.size > 1) { val first = cars.first(); cars.clear(); cars.add(first) } }
                ChoiceChip(tr("مجموعة سيارات", "Multiple cars"), group) { group = true }
            }
            Spacer(Modifier.height(14.dp))

            SectionTitle(if (group) tr("اختر السيارات", "Select cars") else tr("اختر السيارة", "Select the car"))
            Spacer(Modifier.height(6.dp))
            CarMultiPicker(cars, multi = group) { err = it }

            cars.forEachIndexed { idx, sc ->
                Spacer(Modifier.height(10.dp))
                CarTransferCard(sc, idx + 1) { cars.remove(sc) }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(tr("من فرع", "From branch"))
            Spacer(Modifier.height(6.dp))
            DropdownPicker(tr("الفرع الحالي للسيارة", "Car's current branch"), branches, fromBranch, { it.id }, { it.name }, tr("— غير محدّد —", "— Not set —")) { fromBranch = it }

            Spacer(Modifier.height(16.dp))
            SectionTitle(tr("الوجهة", "Destination"))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(tr("فرع داخلي", "Internal branch"), !destIsCompany) { destIsCompany = false; toCompany = 0 }
                ChoiceChip(tr("شركة إيجار", "Rental company"), destIsCompany) { destIsCompany = true; toBranch = 0 }
            }
            Spacer(Modifier.height(10.dp))
            if (destIsCompany) {
                DropdownPicker(tr("الشركة", "Company"), companies, toCompany, { it.id }, { it.name }, tr("— اختر شركة —", "— Pick a company —")) { toCompany = it }
                companies.firstOrNull { it.id == toCompany }?.mapsUrl?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp)); MapsLink(it, tr("موقع الشركة", "Company location"))
                }
            } else {
                DropdownPicker(tr("الفرع", "Branch"), branches, toBranch, { it.id }, { it.name }, tr("— اختر فرعاً —", "— Pick a branch —")) { toBranch = it }
                branches.firstOrNull { it.id == toBranch }?.mapsUrl?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp)); MapsLink(it, tr("موقع الفرع", "Branch location"))
                }
            }

            Spacer(Modifier.height(16.dp))
            MasarField(note, { note = it }, tr("ملاحظة التسليم", "Handover note"), singleLine = false)
            Spacer(Modifier.height(10.dp))
            PhotoPickerField(tr("صورة عامة (اختياري)", "General photo (optional)"), mainPhoto, { mainPhoto = it })

            Spacer(Modifier.height(20.dp))
            PrimaryButton(tr("تأكيد النقل", "Confirm transfer"), loading = submitting) {
                err = null
                if (cars.isEmpty()) { err = tr("اختر سيارة واحدة على الأقل", "Select at least one car"); return@PrimaryButton }
                if (!destIsCompany && toBranch == 0) { err = tr("اختر الفرع الوجهة", "Pick the destination branch"); return@PrimaryButton }
                if (destIsCompany && toCompany == 0) { err = tr("اختر الشركة", "Pick the company"); return@PrimaryButton }
                submitting = true
                val fields = buildMap<String, String?> {
                    put("action", "create"); put("kind", "branch_transfer")
                    put("group_mode", if (group) "1" else "0")
                    if (fromBranch > 0) put("from_branch_id", fromBranch.toString())
                    if (!destIsCompany && toBranch > 0) put("to_branch_id", toBranch.toString())
                    if (destIsCompany && toCompany > 0) put("to_company_id", toCompany.toString())
                    put("note", note); put("cars_json", carsJson(cars))
                }
                scope.launch {
                    val r = Net.repo.dailyWorkCreate(
                        fields, mainPhoto,
                        carPhotos = cars.map { it.photo },
                        driverPhotos = cars.map { it.driverPhoto }
                    )
                    submitting = false
                    when (r) { is Outcome.Ok -> nav.popBackStack(); is Outcome.Err -> err = r.message }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CarTransferCard(sc: SelCar, n: Int, onRemove: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(sc.car.photo, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("$n. ${sc.car.name}", color = Txt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(sc.car.plateFull ?: "", color = Muted, fontSize = 12.sp)
                }
                GhostButton(tr("إزالة", "Remove"), onClick = onRemove)
            }
            Spacer(Modifier.height(10.dp))
            MasarField(sc.odometer, { sc.odometer = it.filter { ch -> ch.isDigit() } }, tr("العداد عند التسليم", "Odometer at handover"), keyboard = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            Text(tr("من كان يقود السيارة؟", "Who drove the car?"), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(tr("أنا", "Me"), sc.driverSelf) { sc.driverSelf = true }
                ChoiceChip(tr("شخص آخر", "Someone else"), !sc.driverSelf) { sc.driverSelf = false }
            }
            if (!sc.driverSelf) {
                Spacer(Modifier.height(10.dp))
                MasarField(sc.driverName, { sc.driverName = it }, tr("اسم السائق", "Driver name"))
                Spacer(Modifier.height(10.dp))
                PhotoPickerField(tr("صورة السائق / أمر التسليم", "Driver / handover doc photo"), sc.driverPhoto, { sc.driverPhoto = it })
            }
            Spacer(Modifier.height(10.dp))
            PhotoPickerField(tr("صورة السيارة", "Car photo"), sc.photo, { sc.photo = it })
        }
    }
}

/* ===================== توصيل سيارة لعميل ===================== */
@Composable
fun DailyDeliveryScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val cars = remember { mutableStateListOf<SelCar>() }
    var customer by remember { mutableStateOf("") }
    var destMaps by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var mainPhoto by remember { mutableStateOf<UploadFile?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    MasarScaffold(title = tr("توصيل سيارة لعميل", "Deliver a car"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ErrorBanner(err)
            SectionTitle(tr("السيارة", "Car"))
            Spacer(Modifier.height(6.dp))
            CarMultiPicker(cars, multi = false) { err = it }
            cars.forEach { sc ->
                Spacer(Modifier.height(10.dp))
                Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(sc.car.photo, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sc.car.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(sc.car.plateFull ?: "", color = Muted, fontSize = 12.sp)
                            }
                            GhostButton(tr("إزالة", "Remove"), onClick = { cars.remove(sc) })
                        }
                        Spacer(Modifier.height(10.dp))
                        MasarField(sc.odometer, { sc.odometer = it.filter { ch -> ch.isDigit() } }, tr("العداد عند التسليم", "Odometer at delivery"), keyboard = KeyboardType.Number)
                        Spacer(Modifier.height(10.dp))
                        PhotoPickerField(tr("صورة السيارة عند التسليم", "Car photo at delivery"), sc.photo, { sc.photo = it })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            MasarField(customer, { customer = it }, tr("اسم العميل *", "Customer name *"))
            Spacer(Modifier.height(10.dp))
            MasarField(destMaps, { destMaps = it }, tr("رابط موقع التسليم (قوقل ماب)", "Delivery location link (Google Maps)"))
            Spacer(Modifier.height(10.dp))
            MasarField(note, { note = it }, tr("ملاحظة (اختياري)", "Note (optional)"), singleLine = false)
            Spacer(Modifier.height(10.dp))
            PhotoPickerField(tr("صورة إضافية / أمر التوصيل", "Extra / delivery doc photo"), mainPhoto, { mainPhoto = it })

            Spacer(Modifier.height(20.dp))
            PrimaryButton(tr("تأكيد التوصيل", "Confirm delivery"), loading = submitting) {
                err = null
                if (cars.isEmpty()) { err = tr("اختر السيارة", "Select the car"); return@PrimaryButton }
                if (customer.isBlank()) { err = tr("اكتب اسم العميل", "Enter the customer name"); return@PrimaryButton }
                submitting = true
                val fields = buildMap<String, String?> {
                    put("action", "create"); put("kind", "delivery")
                    put("customer_name", customer.trim()); put("dest_maps_url", destMaps.trim())
                    put("note", note); put("cars_json", carsJson(cars))
                }
                scope.launch {
                    val r = Net.repo.dailyWorkCreate(fields, mainPhoto, carPhotos = cars.map { it.photo })
                    submitting = false
                    when (r) { is Outcome.Ok -> nav.popBackStack(); is Outcome.Err -> err = r.message }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ===================== استلام قطعة غيار ===================== */
@Composable
fun DailyPartPickupScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var place1 by remember { mutableStateOf("") }
    var place1Maps by remember { mutableStateOf("") }
    var place2 by remember { mutableStateOf("") }
    var place2Maps by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var mainPhoto by remember { mutableStateOf<UploadFile?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    MasarScaffold(title = tr("استلام قطعة غيار", "Pick up a part"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ErrorBanner(err)
            SectionTitle(tr("المكان الأول", "First place"))
            Spacer(Modifier.height(6.dp))
            MasarField(place1, { place1 = it }, tr("اسم المكان الأول *", "First place name *"))
            Spacer(Modifier.height(10.dp))
            MasarField(place1Maps, { place1Maps = it }, tr("رابط الموقع (قوقل ماب)", "Location link (Google Maps)"))

            Spacer(Modifier.height(16.dp))
            SectionTitle(tr("المكان الثاني (إن وُجد)", "Second place (if any)"))
            Spacer(Modifier.height(6.dp))
            MasarField(place2, { place2 = it }, tr("اسم المكان الثاني", "Second place name"))
            Spacer(Modifier.height(10.dp))
            MasarField(place2Maps, { place2Maps = it }, tr("رابط الموقع (قوقل ماب)", "Location link (Google Maps)"))

            Spacer(Modifier.height(16.dp))
            MasarField(workType, { workType = it }, tr("ما هو العمل؟ (نقل قطع / مراجعة سيارة / استلام شحنة…)", "What work? (transport parts / review a car / receive shipment…)"))
            Spacer(Modifier.height(10.dp))
            MasarField(note, { note = it }, tr("ملاحظة (اختياري)", "Note (optional)"), singleLine = false)
            Spacer(Modifier.height(10.dp))
            PhotoPickerField(tr("صورة (اختياري)", "Photo (optional)"), mainPhoto, { mainPhoto = it })

            Spacer(Modifier.height(20.dp))
            PrimaryButton(tr("حفظ", "Save"), loading = submitting) {
                err = null
                if (place1.isBlank()) { err = tr("اكتب اسم المكان الأول", "Enter the first place name"); return@PrimaryButton }
                submitting = true
                val fields = buildMap<String, String?> {
                    put("action", "create"); put("kind", "part_pickup")
                    put("place1_name", place1.trim()); put("place1_maps", place1Maps.trim())
                    put("place2_name", place2.trim()); put("place2_maps", place2Maps.trim())
                    put("work_type", workType.trim()); put("note", note)
                }
                scope.launch {
                    val r = Net.repo.dailyWorkCreate(fields, mainPhoto)
                    submitting = false
                    when (r) { is Outcome.Ok -> nav.popBackStack(); is Outcome.Err -> err = r.message }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ===================== ملاحظة / عمل ===================== */
@Composable
fun DailyNoteScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var mainPhoto by remember { mutableStateOf<UploadFile?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    MasarScaffold(title = tr("ملاحظة / عمل", "Note / task"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ErrorBanner(err)
            Text(tr("اكتب العمل الذي قمت به أو أي ملاحظة لهذا اليوم", "Write the work you did or any note for today"), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            MasarField(note, { note = it }, tr("الملاحظة / العمل *", "Note / task *"), singleLine = false)
            Spacer(Modifier.height(10.dp))
            PhotoPickerField(tr("صورة (اختياري)", "Photo (optional)"), mainPhoto, { mainPhoto = it })

            Spacer(Modifier.height(20.dp))
            PrimaryButton(tr("حفظ", "Save"), loading = submitting) {
                err = null
                if (note.isBlank()) { err = tr("اكتب الملاحظة", "Enter the note"); return@PrimaryButton }
                submitting = true
                val fields = buildMap<String, String?> {
                    put("action", "create"); put("kind", "note"); put("note", note)
                }
                scope.launch {
                    val r = Net.repo.dailyWorkCreate(fields, mainPhoto)
                    submitting = false
                    when (r) { is Outcome.Ok -> nav.popBackStack(); is Outcome.Err -> err = r.message }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ===== رابط خرائط قابل للفتح ===== */
@Composable
fun MapsLink(url: String?, label: String? = null) {
    if (url.isNullOrBlank()) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Surface(
        color = Blue.copy(alpha = 0.12f), shape = RoundedCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            runCatching {
                ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url.trim())))
            }
        }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📍", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(label ?: tr("فتح الموقع على الخرائط", "Open location on Maps"),
                color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("↗", color = Blue, fontSize = 14.sp)
        }
    }
}

/* ===== شريحة اختيار ===== */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Red.copy(alpha = 0.16f) else Panel2,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) Red else Line),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label, color = if (selected) Red else Txt, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
