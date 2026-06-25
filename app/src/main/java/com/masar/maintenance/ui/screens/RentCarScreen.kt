package com.masar.maintenance.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun RentCarScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Car>>(emptyList()) }
    var selected by remember { mutableStateOf<Car?>(null) }
    var follows by remember { mutableStateOf<List<FollowEmployee>>(emptyList()) }
    var followId by remember { mutableStateOf(0) }

    var renter by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }

    var submitting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    suspend fun search(q: String) {
        when (val r = Net.repo.rentalAvailable(q)) {
            is Outcome.Ok -> results = r.data
            is Outcome.Err -> err = r.message
        }
    }

    LaunchedEffect(Unit) {
        search("")
        when (val r = Net.repo.followEmployees()) { is Outcome.Ok -> follows = r.data; is Outcome.Err -> {} }
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val token = res.contents
        if (!token.isNullOrBlank()) scope.launch {
            when (val r = Net.repo.carByQr(token)) {
                is Outcome.Ok -> {
                    val c = r.data
                    if (c.state == "under_maintenance") err = tr("السيارة تحت الصيانة", "Car is under maintenance")
                    else { selected = c; err = null }
                }
                is Outcome.Err -> err = r.message
            }
        }
    }

    MasarScaffold(title = tr("تأجير سيارة", "Rent a car"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            err?.let {
                Surface(color = RedStatus.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = RedStatus, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // اختيار السيارة
            SectionTitle(tr("السيارة (المتاحة فقط)", "Car (available only)"))
            Spacer(Modifier.height(6.dp))
            selected?.let { c ->
                MasarCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(c.photo, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = Txt, fontWeight = FontWeight.Bold)
                            Text(c.plateFull ?: "", color = Muted, fontSize = 12.sp)
                            val hint = listOfNotNull(
                                c.priceDaily?.takeIf { it.isNotBlank() }?.let { tr("اليومي: ", "Daily: ") + it },
                                c.priceMonthly?.takeIf { it.isNotBlank() }?.let { tr("الشهري: ", "Monthly: ") + it }
                            )
                            Text(
                                if (hint.isEmpty()) tr("لا يوجد سعر محدّد", "No price set")
                                else tr("السعر المقترح — ", "Suggested — ") + hint.joinToString(" · "),
                                color = if (hint.isEmpty()) Muted else Yellow, fontSize = 11.sp
                            )
                        }
                        GhostButton(tr("تغيير", "Change"), onClick = { selected = null })
                    }
                }
                LaunchedEffect(c.id) { if (price.isBlank()) c.priceDaily?.takeIf { it.isNotBlank() }?.let { price = it } }
                Spacer(Modifier.height(10.dp))
            }
            if (selected == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        MasarField(query, {
                            query = it; scope.launch { search(it) }
                        }, tr("ابحث باللوحة أو الاسم أو الكود…", "Search by plate, name or code…"))
                    }
                    GhostButton("⛶ QR", onClick = {
                        qrLauncher.launch(ScanOptions().apply {
                            setPrompt(tr("وجّه الكاميرا نحو رمز QR للسيارة", "Point the camera at the car QR"))
                            setBeepEnabled(true); setOrientationLocked(true)
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setCaptureActivity(com.masar.maintenance.SquareCaptureActivity::class.java)
                        })
                    })
                }
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text(tr("لا توجد سيارات متاحة مطابقة", "No matching available cars"), color = Muted, fontSize = 13.sp)
                } else {
                    results.take(30).forEach { c ->
                        Surface(
                            color = Panel2, shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = c; err = null }
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RemoteImage(c.photo, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(c.plateFull ?: "", color = Muted, fontSize = 12.sp)
                                }
                                if (!c.carCode.isNullOrBlank()) Text(c.carCode!!, color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // بيانات المستأجر
            Spacer(Modifier.height(16.dp))
            SectionTitle(tr("بيانات الإيجار", "Rental details"))
            Spacer(Modifier.height(8.dp))
            MasarField(renter, { renter = it }, tr("اسم المستأجر *", "Renter name *"))
            Spacer(Modifier.height(10.dp))
            MasarField(phone, { phone = it }, tr("رقم جوال المستأجر", "Renter mobile"), keyboard = KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            MasarField(days, { days = it.filter { ch -> ch.isDigit() } }, tr("عدد أيام الإيجار *", "Rental days *"), keyboard = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            MasarField(price, { price = it }, tr("سعر الإيجار الفعلي (ر.س)", "Actual rental price (SAR)"), keyboard = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            MasarField(contract, { contract = it }, tr("رقم العقد (اختياري)", "Contract number (optional)"))

            // موظف المتابعة
            Spacer(Modifier.height(16.dp))
            SectionTitle(tr("موظف متابعة السيارة (للفحص)", "Car-tracking staff (for inspection)"))
            Spacer(Modifier.height(8.dp))
            if (follows.isEmpty()) {
                Text(tr("لا يوجد موظفو متابعة سيارات بعد", "No car-tracking staff yet"), color = Muted, fontSize = 13.sp)
            } else {
                follows.forEach { f ->
                    val sel = followId == f.id
                    Surface(
                        color = if (sel) Red.copy(alpha = 0.16f) else Panel2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { followId = if (sel) 0 else f.id }
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (sel) "◉" else "○", color = if (sel) Red else Muted)
                            Spacer(Modifier.width(10.dp))
                            RemoteImage(f.photo, modifier = Modifier.size(34.dp).clip(CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(f.name, color = Txt, fontSize = 14.sp)
                            f.code?.let { Spacer(Modifier.width(8.dp)); Text(it, color = Muted, fontSize = 12.sp) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(tr("تأجير وإسناد", "Rent & assign"), loading = submitting) {
                err = null
                val c = selected
                if (c == null) { err = tr("اختر السيارة أولاً", "Select the car first"); return@PrimaryButton }
                if (renter.isBlank()) { err = tr("اكتب اسم المستأجر", "Enter the renter name"); return@PrimaryButton }
                val d = days.toIntOrNull() ?: 1
                submitting = true
                scope.launch {
                    when (val r = Net.repo.rentalCreate(c.id, renter.trim(), phone.trim(), d, price.trim(), contract.trim(), followId)) {
                        is Outcome.Ok -> { submitting = false; nav.popBackStack() }
                        is Outcome.Err -> { submitting = false; err = r.message }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
