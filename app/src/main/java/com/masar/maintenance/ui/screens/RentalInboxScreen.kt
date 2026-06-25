package com.masar.maintenance.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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

@Composable
fun RentalInboxScreen(nav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        when (val r = Net.repo.myRentals()) {
            is Outcome.Ok -> { rentals = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        res.contents?.takeIf { it.isNotBlank() }?.let { query = it.trim() }
    }

    fun matches(r: Rental): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return (r.carName.lowercase().contains(q)) ||
            ((r.plateFull ?: "").lowercase().contains(q)) ||
            ((r.carCode ?: "").lowercase().contains(q))
    }

    MasarScaffold(title = tr("سيارات للفحص", "Cars to inspect"), onBack = { nav.popBackStack() }) { pad ->
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!)
            rentals.isEmpty() -> EmptyBox(tr("لا توجد سيارات مُسندة إليك حالياً", "No cars assigned to you currently"), "✓")
            else -> {
                val filtered = rentals.filter { matches(it) }
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                MasarField(query, { query = it }, tr("ابحث باسم السيارة أو اللوحة…", "Search by car name or plate…"))
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
                        if (query.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(tr("النتائج: ", "Results: ") + "${filtered.size}", color = Muted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (filtered.isEmpty()) {
                        item { EmptyBox(tr("لا نتائج مطابقة", "No matching results"), "∅") }
                    }
                    items(filtered) { r ->
                        val needsHandover = r.needsHandover == true
                        val kind = if (needsHandover) "handover" else "return"
                        Surface(
                            color = Panel, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                nav.navigate("rentalForm/${r.id}?kind=$kind")
                            }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RemoteImage(r.carPhoto, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.carName, color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(r.plateFull ?: "", color = Muted, fontSize = 12.sp)
                                }
                                Badge(
                                    if (needsHandover) tr("بانتظار تقرير التسليم", "Handover report due")
                                    else tr("تقرير الإرجاع", "Return report"),
                                    if (needsHandover) Yellow else Blue
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(tr("المستأجر: ", "Renter: ") + r.renterName +
                                (r.renterPhone?.let { " · $it" } ?: ""), color = Txt, fontSize = 13.sp)
                            Text(tr("المدة: ", "Duration: ") + "${r.totalDays} " + tr("يوم", "days") +
                                (r.dueAt?.let { " · " + tr("تنتهي: ", "ends: ") + it.take(10) } ?: ""),
                                color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            }
        }
    }
}
