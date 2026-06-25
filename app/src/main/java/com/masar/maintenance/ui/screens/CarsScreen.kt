package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.data.Car
import com.masar.maintenance.data.Net
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.tr
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*

@Composable
fun CarsScreen(nav: NavController) {
    val role = Net.session.userRole
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    MasarScaffold(title = tr("سجل السيارات","Cars"), onBack = { nav.popBackStack() }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                    MasarField(query, { query = it }, tr("ابحث بالاسم أو اللوحة أو الكود…","Search by name, plate or code…"))
                    Spacer(Modifier.height(8.dp))
                    StateFilter(state) { state = it }
                }

                RemoteContent(
                    reloadKey = "$query|$state",
                    load = { Net.repo.cars(q = query, state = state) }
                ) { cars ->
                    if (cars.isEmpty()) {
                        EmptyBox(tr("لا توجد سيارات مطابقة","No matching cars"), "∅")
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                        ) {
                            items(cars, key = { it.id }) { c ->
                                CarCard(c) { nav.navigate("carHistory/${c.id}") }
                            }
                        }
                    }
                }
            }

            if (role == "admin") {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate("carForm?id=0") },
                    containerColor = Red, contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(tr("إضافة سيارة","Add car")) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StateFilter(state: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val opts = listOf(
        "" to tr("كل الحالات","All states"),
        "available" to tr("متاحة","Available"),
        "under_maintenance" to tr("تحت الصيانة","Under maintenance")
    )
    val label = opts.firstOrNull { it.first == state }?.second ?: tr("كل الحالات","All states")
    Box {
        GhostButton(text = tr("الحالة","State") + ": $label", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            opts.forEach { (v, t) ->
                DropdownMenuItem(text = { Text(t) }, onClick = { onChange(v); open = false })
            }
        }
    }
}

@Composable
private fun CarCard(c: Car, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                c.photo,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                placeholder = "🚗"
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Labels.carColor(c.color)))
                    Spacer(Modifier.width(7.dp))
                    Text(c.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    buildString {
                        append(c.plateFull ?: "—")
                        c.carCode?.let { append(" · " + tr("كود","Code") + ": $it") }
                    },
                    color = Muted, fontSize = 13.sp
                )
                Row(Modifier.padding(top = 4.dp)) {
                    val stateLabel = when (c.state) {
                        "available" -> tr("متاحة","Available"); "under_maintenance" -> tr("تحت الصيانة","Under maintenance"); else -> c.state
                    }
                    val stateColor = if (c.state == "available") Green else Yellow
                    Badge(stateLabel, stateColor)
                    if (c.openSerial != null) {
                        Spacer(Modifier.width(6.dp))
                        Badge(tr("طلب","Req") + ": ${c.openSerial}", Blue)
                    }
                }
            }
        }
    }
}
