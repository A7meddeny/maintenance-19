package com.masar.maintenance.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.tr
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CarReportScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        when (val r = Net.repo.cars()) {
            is Outcome.Ok -> { cars = r.data; loading = false }
            is Outcome.Err -> { error = r.message; loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val token = res.contents
        if (!token.isNullOrBlank()) {
            scope.launch {
                when (val r = Net.repo.carByQr(token)) {
                    is Outcome.Ok -> nav.navigate("carHistory/${r.data.id}")
                    is Outcome.Err -> error = r.message
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

    val filtered = remember(q, cars) {
        if (q.isBlank()) cars
        else cars.filter {
            it.name.contains(q, true) ||
            (it.plateFull ?: "").contains(q, true) ||
            (it.carCode ?: "").contains(q, true) ||
            it.id.toString().contains(q)
        }
    }

    MasarScaffold(title = tr("تقرير سيارة","Car report"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            GhostButton(tr("🔳 مسح رمز QR للسيارة","🔳 Scan car QR"), onClick = { scanCarQr() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            MasarField(q, { q = it }, tr("ابحث بالاسم أو اللوحة أو الكود…","Search by name, plate or code…"))
            Spacer(Modifier.height(12.dp))
            error?.let { Text(it, color = RedStatus, fontSize = 13.sp); Spacer(Modifier.height(10.dp)) }

            when {
                loading -> LoadingBox()
                filtered.isEmpty() -> EmptyBox(tr("لا توجد سيارات مطابقة","No matching cars"), "∅")
                else -> LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { c ->
                        Surface(
                            color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line),
                            modifier = Modifier.fillMaxWidth().clickable { nav.navigate("carHistory/${c.id}") }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RemoteImage(c.photo, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, color = Txt, fontWeight = FontWeight.Bold)
                                    Text(
                                        buildString {
                                            append(c.plateFull ?: "—")
                                            c.carCode?.let { append(" · " + tr("كود","Code") + ": $it") }
                                        },
                                        color = Muted, fontSize = 12.sp
                                    )
                                }
                                Text("‹", color = Muted, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
