package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.masar.maintenance.ui.tr
import com.masar.maintenance.R
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

private val INSPECT_ITEMS = listOf(
    "المحرك", "ناقل الحركة", "نظام التعليق", "الفرامل", "التوجيه (الدركسون)",
    "التكييف", "النظام الكهربائي", "حالة الأنوار", "حالة المقاعد والفرش", "حالة الزجاج", "حالة الطلاء"
)
private val INSPECT_RATINGS = listOf("ممتاز", "جيد", "مقبول", "يحتاج إصلاح")

private data class InsTire(val key: String, val label: String, val fx: Float, val fy: Float)
private val INS_TIRES = listOf(
    InsTire("fl", "أمامي يسار", 0.30f, 0.27f),
    InsTire("fr", "أمامي يمين", 0.66f, 0.27f),
    InsTire("rl", "خلفي يسار", 0.30f, 0.68f),
    InsTire("rr", "خلفي يمين", 0.66f, 0.68f),
)

/* تسميات العرض فقط — القيم المخزَّنة تبقى عربية للحفاظ على توافق البيانات */
private fun inspItemLabel(k: String): String = when (k) {
    "المحرك" -> tr("المحرك", "Engine"); "ناقل الحركة" -> tr("ناقل الحركة", "Transmission")
    "نظام التعليق" -> tr("نظام التعليق", "Suspension"); "الفرامل" -> tr("الفرامل", "Brakes")
    "التوجيه (الدركسون)" -> tr("التوجيه (الدركسون)", "Steering")
    "التكييف" -> tr("التكييف", "A/C"); "النظام الكهربائي" -> tr("النظام الكهربائي", "Electrical system")
    "حالة الأنوار" -> tr("حالة الأنوار", "Lights"); "حالة المقاعد والفرش" -> tr("حالة المقاعد والفرش", "Seats & upholstery")
    "حالة الزجاج" -> tr("حالة الزجاج", "Glass"); "حالة الطلاء" -> tr("حالة الطلاء", "Paint")
    else -> k
}
private fun ratingLabel(r: String): String = when (r) {
    "ممتاز" -> tr("ممتاز", "Excellent"); "جيد" -> tr("جيد", "Good")
    "مقبول" -> tr("مقبول", "Fair"); "يحتاج إصلاح" -> tr("يحتاج إصلاح", "Needs repair")
    else -> r
}
private fun tireLabel(key: String): String = when (key) {
    "fl" -> tr("أمامي يسار", "Front left"); "fr" -> tr("أمامي يمين", "Front right")
    "rl" -> tr("خلفي يسار", "Rear left"); "rr" -> tr("خلفي يمين", "Rear right")
    else -> key
}

/* ===================== قائمة تقارير الفحص ===================== */
@Composable
fun InspectionsListScreen(nav: NavController) {
    var rows by remember { mutableStateOf<List<InspectionRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        when (val r = Net.repo.inspections()) {
            is Outcome.Ok -> { rows = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }

    MasarScaffold(title = tr("تقارير الفحص","Inspection reports"), onBack = { nav.popBackStack() }) { pad ->
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { reload++ }
            rows.isEmpty() -> EmptyBox(tr("لا توجد تقارير فحص","No inspection reports"), "📋")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pending = rows.filter { it.status == "pending" }
                val done = rows.filter { it.status == "done" }
                if (pending.isNotEmpty()) {
                    item { Text(tr("بانتظار تعبئتك","Awaiting your input"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    items(pending, key = { "p${it.id}" }) { InspRow(it) { nav.navigate("inspection/${it.id}") } }
                }
                if (done.isNotEmpty()) {
                    item { Spacer(Modifier.height(4.dp)); Text(tr("تقارير مكتملة","Completed reports"), color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    items(done, key = { "d${it.id}" }) { InspRow(it) { nav.navigate("inspection/${it.id}") } }
                }
            }
        }
    }
}

@Composable
private fun InspRow(x: InspectionRow, onClick: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(x.carPhoto, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(x.reportNo, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(x.carName ?: "—", color = Txt, fontWeight = FontWeight.Bold)
                if (!x.description.isNullOrBlank()) Text(x.description ?: "", color = Muted, fontSize = 12.sp, maxLines = 1)
            }
            Badge(
                if (x.status == "done") tr("مكتمل","Completed") else tr("بانتظار التعبئة","Pending"),
                if (x.status == "done") Green else Yellow
            )
        }
    }
}

/* ===================== تعبئة / عرض تقرير ===================== */
@Composable
fun InspectionScreen(nav: NavController, id: Int) {
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf<InspectionDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // الحالة العامة
    val general = remember { mutableStateMapOf<String, String>() }
    var generalNote by remember { mutableStateOf("") }
    // الكفرات
    val tireSel = remember { mutableStateListOf<String>() }
    var tireNote by remember { mutableStateOf("") }
    // الهيكل
    val bodyPts = remember { mutableStateListOf<Offset>() }
    var bodyNote by remember { mutableStateOf("") }
    // عام
    var note by remember { mutableStateOf("") }
    var photoGeneral by remember { mutableStateOf<UploadFile?>(null) }
    var photoTire by remember { mutableStateOf<UploadFile?>(null) }
    val bodyPhotos = remember { mutableStateListOf<UploadFile>() }

    var submitting by remember { mutableStateOf(false) }
    var submitErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        loading = true
        when (val r = Net.repo.inspectionGet(id)) {
            is Outcome.Ok -> { d = r.data; error = null }
            is Outcome.Err -> error = r.message
        }
        loading = false
    }

    MasarScaffold(title = tr("تقرير فحص","Inspection report"), onBack = { nav.popBackStack() }) { pad ->
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!)
            d == null -> EmptyBox(tr("غير موجود","Not found"), "∅")
            d!!.status == "done" -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Badge(tr("تم إرسال هذا التقرير","This report has been submitted"), Green)
                    Spacer(Modifier.height(10.dp))
                    Text(tr("يمكن مراجعة تفاصيله من لوحة التحكم.","Details can be reviewed from the dashboard."), color = Muted, fontSize = 13.sp)
                }
            }
            else -> Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                val c = d!!
                submitErr?.let {
                    Surface(color = RedStatus.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = RedStatus, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                // رأس
                Text("${c.reportNo} — ${c.carName ?: ""}", color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!c.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp)); Text(tr("المطلوب: ","Requested: ") + "${c.description}", color = Muted, fontSize = 13.sp)
                }
                if (!c.carPhoto.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    RemoteImage(c.carPhoto, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)))
                }

                // (1) الحالة العامة
                Spacer(Modifier.height(16.dp))
                SectionCard(tr("تقييم الحالة العامة","General condition assessment")) {
                    INSPECT_ITEMS.forEach { item ->
                        Text(inspItemLabel(item), color = Txt, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            INSPECT_RATINGS.forEach { rt ->
                                val sel = general[item] == rt
                                Surface(
                                    color = if (sel) ratingColor(rt).copy(alpha = 0.85f) else Panel2,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (sel) ratingColor(rt) else Line2),
                                    modifier = Modifier.weight(1f).clickable { general[item] = rt }
                                ) {
                                    Text(ratingLabel(rt), color = if (sel) Ink else Txt, fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    MasarField(generalNote, { generalNote = it }, tr("ملاحظة الحالة العامة (اختياري)","General condition note (optional)"), singleLine = false)
                    Spacer(Modifier.height(8.dp))
                    PhotoPickerField(tr("صورة (اختياري)","Photo (optional)"), photoGeneral, { photoGeneral = it })
                }

                // (2) الكفرات
                Spacer(Modifier.height(16.dp))
                SectionCard(tr("تقرير الكفرات — حدّد ما يحتاج تغيير","Tires report — select what needs changing")) {
                    InsTirePicker(tireSel)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("المحدد: ","Selected: ") + (if (tireSel.isEmpty()) tr("لا شيء","None") else tireSel.mapNotNull { k -> INS_TIRES.firstOrNull { it.key == k }?.let { tireLabel(it.key) } }.joinToString(tr("، ",", "))),
                        color = if (tireSel.isEmpty()) Muted else Yellow, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    MasarField(tireNote, { tireNote = it }, tr("ملاحظة الكفرات (اختياري)","Tires note (optional)"), singleLine = false)
                    Spacer(Modifier.height(8.dp))
                    PhotoPickerField(tr("صورة الكفرات (اختياري)","Tires photo (optional)"), photoTire, { photoTire = it })
                }

                // (3) الهيكل
                Spacer(Modifier.height(16.dp))
                SectionCard(tr("تقرير الهيكل — اضغط على مواقع المشاكل","Body report — tap the problem locations")) {
                    Box(Modifier.fillMaxWidth(0.62f).aspectRatio(702f / 1201f).align(Alignment.CenterHorizontally)) {
                        Image(painterResource(R.drawable.car_body), contentDescription = tr("هيكل السيارة","Car body"), modifier = Modifier.fillMaxSize())
                        Canvas(Modifier.matchParentSize().pointerInput(Unit) {
                            detectTapGestures { o -> bodyPts.add(Offset(o.x / size.width.toFloat(), o.y / size.height.toFloat())) }
                        }) {
                            bodyPts.forEach { p ->
                                drawCircle(color = Red, radius = 26f, center = Offset(p.x * size.width, p.y * size.height), style = Stroke(width = 7f))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(tr("عدد المواقع: ","Locations: ") + "${bodyPts.size}", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    GhostButton(tr("تراجع عن آخر دائرة","Undo last mark"), onClick = { if (bodyPts.isNotEmpty()) bodyPts.removeAt(bodyPts.size - 1) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    MasarField(bodyNote, { bodyNote = it }, tr("ملاحظة الهيكل (اختياري)","Body note (optional)"), singleLine = false)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("صور الهيكل (يمكن إضافة أكثر من صورة)","Body photos (you can add more than one)"), color = Muted, fontSize = 13.sp)
                    bodyPhotos.forEachIndexed { idx, uf ->
                        Spacer(Modifier.height(8.dp))
                        PhotoPickerField(tr("صورة الهيكل ","Body photo ") + "${idx + 1}", uf, { newUf ->
                            if (newUf == null) bodyPhotos.removeAt(idx) else bodyPhotos[idx] = newUf
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    PhotoPickerField("＋ " + tr("أضف صورة هيكل","Add body photo"), null, { newUf -> if (newUf != null) bodyPhotos.add(newUf) })
                }

                // ملاحظة عامة + إرسال
                Spacer(Modifier.height(16.dp))
                MasarField(note, { note = it }, tr("ملاحظة عامة (اختياري)","General note (optional)"), singleLine = false)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(tr("إرسال التقرير للإدارة","Send report to admin"), loading = submitting) {
                    submitErr = null
                    if (general.size < INSPECT_ITEMS.size) { submitErr = tr("أكمل تقييم كل بنود الحالة العامة","Complete the rating for all general items"); return@PrimaryButton }
                    submitting = true
                    scope.launch {
                        val root = JsonObject()
                        val gen = JsonObject(); general.forEach { (k, v) -> gen.addProperty(k, v) }
                        root.add("general", gen)
                        root.addProperty("general_note", generalNote)
                        val tires = JsonObject()
                        val corners = JsonArray(); tireSel.forEach { corners.add(it) }
                        tires.add("corners", corners); tires.addProperty("note", tireNote)
                        root.add("tires", tires)
                        val body = JsonObject()
                        val pts = JsonArray()
                        bodyPts.forEach { p -> val o = JsonObject(); o.addProperty("x", p.x); o.addProperty("y", p.y); pts.add(o) }
                        body.add("points", pts); body.addProperty("note", bodyNote)
                        root.add("body", body)
                        root.addProperty("note", note)
                        val photos = listOfNotNull(photoGeneral, photoTire) + bodyPhotos.toList()
                        when (val r = Net.repo.inspectionSubmit(id, root.toString(), photos)) {
                            is Outcome.Ok -> { submitting = false; nav.popBackStack() }
                            is Outcome.Err -> { submitting = false; submitErr = r.message }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private fun ratingColor(r: String) = when (r) {
    "ممتاز" -> Green; "جيد" -> Blue; "مقبول" -> Yellow; else -> RedStatus
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Txt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InsTirePicker(selected: MutableList<String>) {
    val size = 220.dp
    Box(Modifier.size(size)) {
        Image(painter = painterResource(R.drawable.tire_layout), contentDescription = tr("الكفرات","Tires"), modifier = Modifier.fillMaxSize())
        INS_TIRES.forEach { spot ->
            val checked = spot.key in selected
            val mk = 44.dp
            Box(
                Modifier.offset(x = size * spot.fx - mk / 2, y = size * spot.fy - mk / 2)
                    .size(mk).clip(CircleShape)
                    .background(if (checked) Red.copy(alpha = 0.85f) else Red.copy(alpha = 0.15f))
                    .clickable { if (checked) selected.remove(spot.key) else selected.add(spot.key) },
                contentAlignment = Alignment.Center
            ) {
                Text(if (checked) "✕" else "+", color = if (checked) Ink else Txt, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
