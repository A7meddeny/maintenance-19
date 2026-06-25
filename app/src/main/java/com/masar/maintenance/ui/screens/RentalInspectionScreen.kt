package com.masar.maintenance.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.navigation.NavController
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.masar.maintenance.R
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import com.masar.maintenance.ui.tr
import kotlinx.coroutines.launch

/** بنود فورم فحص التأجير (عربي + إنجليزي) — المفتاح المخزَّن عربي */
private val RENTAL_ITEMS = listOf(
    "استمارة السيارة" to "Registration Book",
    "المساحات" to "Wipers",
    "المراية الداخلية" to "Rear-View Mirror",
    "المراية الخارجية" to "Side Mirror",
    "الاستبني + العدة" to "Spare Tire & Tools",
    "رقم اللوحات" to "Number Plates",
    "اقفال الأبواب" to "Door Locks",
    "النور" to "Head Light",
    "الاشارات" to "Indicators",
    "بنزين" to "Gas",
    "جنط" to "Hub",
    "كفرات" to "Tires",
    "كفر أمامي يمين" to "Right Front Tire",
    "كفر خلفي يمين" to "Right Back Tire",
    "كفر أمامي يسار" to "Left Front Tire",
    "كفر خلفي يسار" to "Left Back Tire",
    "مساند الرأس" to "Head Set",
    "نظافة داخلية" to "Clean Interior",
    "نظافة خارجية" to "Clean Exterior",
    "طفايات السجائر" to "Clean Ashtrays",
    "ولاعات السجائر" to "Cigarettes Lighter",
    "الراديو" to "Radio",
    "الأحزمة" to "Seat Belts",
    "المكيف" to "Air Conditioning",
    "الطاسات" to "Hub Caps",
    "زيت الماكينة" to "Engine Oil",
    "فرامل" to "Brakes",
    "عداد السرعة" to "Speedometer",
    "الفراش الداخلي" to "Interior Furniture",
)

@Composable
fun RentalInspectionScreen(nav: NavController, rentalId: Int, kind: String) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val isReturn = kind == "return"
    val maxPhotos = if (isReturn) 5 else 10

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rental by remember { mutableStateOf<Rental?>(null) }

    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val bodyPts = remember { mutableStateListOf<Offset>() }
    val photos = remember { mutableStateListOf<UploadFile>() }
    var km by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hasIssues by remember { mutableStateOf(false) }

    var submitting by remember { mutableStateOf(false) }
    var submitErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rentalId) {
        loading = true
        when (val r = Net.repo.rentalGet(rentalId)) {
            is Outcome.Ok -> { rental = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }

    MasarScaffold(
        title = if (isReturn) tr("تقرير إرجاع", "Return report") else tr("تقرير تسليم", "Handover report"),
        onBack = { nav.popBackStack() }
    ) { pad ->
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!)
            rental == null -> EmptyBox(tr("العقد غير موجود", "Rental not found"), "∅")
            else -> {
                val r = rental!!
                Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    submitErr?.let {
                        Surface(color = RedStatus.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(it, color = RedStatus, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    // رأس العقد
                    MasarCard {
                        Text("${r.carName} — ${r.plateFull ?: ""}", color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(tr("المستأجر: ", "Renter: ") + r.renterName + (r.renterPhone?.let { " · $it" } ?: ""), color = Muted, fontSize = 12.sp)
                        r.model?.let { Text(tr("الموديل: ", "Model: ") + it, color = Muted, fontSize = 12.sp) }
                    }

                    // التقرير السابق (التسليم) عند عمل تقرير الإرجاع — للمقارنة
                    val prev = if (isReturn) r.handover else null
                    if (prev != null) {
                        Spacer(Modifier.height(12.dp))
                        val okItems = prev.checklist?.filterValues { it }?.keys?.toList().orEmpty()
                        Surface(color = Panel2, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(13.dp)) {
                                Text(tr("التقرير السابق (التسليم) — للمقارنة", "Previous report (handover) — for comparison"),
                                    color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                prev.followName?.let { Text(tr("بواسطة: ", "By: ") + it + (prev.createdAt?.let { c -> " · " + c.take(16) } ?: ""), color = Muted, fontSize = 11.sp) }
                                prev.km?.takeIf { it.isNotBlank() }?.let { Text(tr("الكيلومتر: ", "KM: ") + it, color = Txt, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                                Text(tr("بنود سليمة وقت التسليم (", "OK items at handover (") + "${okItems.size}): " +
                                    if (okItems.isEmpty()) "—" else okItems.joinToString("، "), color = Txt, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                prev.note?.takeIf { it.isNotBlank() }?.let { Text(tr("ملاحظة: ", "Note: ") + it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
                                if ((prev.photos?.size ?: 0) > 0) Text(tr("الصور: ", "Photos: ") + "${prev.photos!!.size}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    // زر فتح/طباعة PDF إن وُجد تقرير لهذا النوع مسبقاً
                    val existing = if (isReturn) r.returnInsp else r.handover
                    if (existing != null) {
                        Spacer(Modifier.height(10.dp))
                        GhostButton(tr("⎙ فتح/طباعة PDF", "⎙ Open/print PDF"), onClick = {
                            val base = Net.session.baseUrl
                            val tok = Net.session.token ?: ""
                            val url = base + "rental_print.php?id=" + rentalId + "&kind=" + kind + "&t=" + tok
                            runCatching {
                                ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                        }, modifier = Modifier.fillMaxWidth())
                    }

                    // (1) قائمة الفحص
                    Spacer(Modifier.height(16.dp))
                    SectionCard(tr("قائمة الفحص — اضغط على البنود السليمة", "Checklist — tap the items that are OK")) {
                        RENTAL_ITEMS.forEach { (ar, en) ->
                            val on = checked[ar] == true
                            Surface(
                                color = if (on) Green.copy(alpha = 0.16f) else Panel2,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { checked[ar] = !on }
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (on) "☑" else "☐", color = if (on) Green else Muted, fontSize = 18.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(if (com.masar.maintenance.ui.I18n.isAr) ar else en, color = Txt, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(tr("المحدد: ", "Selected: ") + "${checked.count { it.value }}/${RENTAL_ITEMS.size}", color = Muted, fontSize = 12.sp)
                    }

                    // (2) الهيكل — دوائر حمراء
                    Spacer(Modifier.height(16.dp))
                    SectionCard(tr("الهيكل — اضغط على مواقع الملاحظات/الأضرار", "Body — tap the damage/note locations")) {
                        Box(Modifier.fillMaxWidth(0.62f).aspectRatio(702f / 1201f).align(Alignment.CenterHorizontally)) {
                            Image(painterResource(R.drawable.car_body), contentDescription = tr("هيكل السيارة", "Car body"), modifier = Modifier.fillMaxSize())
                            Canvas(Modifier.matchParentSize().pointerInput(Unit) {
                                detectTapGestures { o -> bodyPts.add(Offset(o.x / size.width.toFloat(), o.y / size.height.toFloat())) }
                            }) {
                                bodyPts.forEach { p ->
                                    drawCircle(color = Red, radius = 26f, center = Offset(p.x * size.width, p.y * size.height), style = Stroke(width = 7f))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(tr("عدد المواقع: ", "Locations: ") + "${bodyPts.size}", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        GhostButton(tr("تراجع عن آخر دائرة", "Undo last mark"), onClick = { if (bodyPts.isNotEmpty()) bodyPts.removeAt(bodyPts.size - 1) }, modifier = Modifier.fillMaxWidth())
                    }

                    // (3) كيلومتر + ملاحظات + صور
                    Spacer(Modifier.height(16.dp))
                    SectionCard(tr("بيانات إضافية", "Additional data")) {
                        MasarField(km, { km = it }, tr("الكيلومتر", "Kilometers"), keyboard = KeyboardType.Number)
                        Spacer(Modifier.height(10.dp))
                        MasarField(note, { note = it }, tr("ملاحظات (اختياري)", "Notes (optional)"), singleLine = false)
                        if (isReturn) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = if (hasIssues) RedStatus.copy(alpha = 0.14f) else Panel2,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.fillMaxWidth().clickable { hasIssues = !hasIssues }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (hasIssues) "☑" else "☐", color = if (hasIssues) RedStatus else Muted, fontSize = 18.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(tr("توجد مشاكل/أضرار عند الإرجاع", "There are problems/damages on return"), color = Txt, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            tr("صور السيارة (حتى ", "Car photos (up to ") + "$maxPhotos" + tr(" صور)", ")"),
                            color = Muted, fontSize = 13.sp
                        )
                        photos.forEachIndexed { idx, uf ->
                            Spacer(Modifier.height(8.dp))
                            PhotoPickerField(tr("صورة ", "Photo ") + "${idx + 1}", uf, { newUf ->
                                if (newUf == null) photos.removeAt(idx) else photos[idx] = newUf
                            })
                        }
                        if (photos.size < maxPhotos) {
                            Spacer(Modifier.height(8.dp))
                            PhotoPickerField("＋ " + tr("أضف صورة", "Add photo"), null, { newUf -> if (newUf != null) photos.add(newUf) })
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        if (isReturn) tr("اعتماد وإقفال العقد", "Approve & close rental")
                        else tr("اعتماد التقرير", "Approve report"),
                        loading = submitting
                    ) {
                        submitErr = null
                        submitting = true
                        scope.launch {
                            val cl = JsonObject()
                            RENTAL_ITEMS.forEach { (ar, _) -> cl.addProperty(ar, checked[ar] == true) }
                            val pts = JsonArray()
                            bodyPts.forEach { p -> val o = JsonObject(); o.addProperty("x", p.x); o.addProperty("y", p.y); pts.add(o) }
                            when (val res = Net.repo.rentalSubmitInspection(
                                rentalId, kind, cl.toString(), pts.toString(), km.trim(), note.trim(),
                                if (isReturn) hasIssues else false, photos.toList()
                            )) {
                                is Outcome.Ok -> { submitting = false; nav.popBackStack() }
                                is Outcome.Err -> { submitting = false; submitErr = res.message }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
