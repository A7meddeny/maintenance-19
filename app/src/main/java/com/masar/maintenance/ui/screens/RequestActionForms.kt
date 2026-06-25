package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.masar.maintenance.ui.tr
import com.masar.maintenance.data.*
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

private val gson = Gson()

/* ============ قائمة منسدلة بسيطة ============ */
@Composable
internal fun LabeledDropdown(
    label: String,
    options: List<Pair<String, String>>, // value to display
    selected: String,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second ?: ""
    Column {
        Text(label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Box {
            GhostButton(text = display, onClick = { open = true }, modifier = Modifier.fillMaxWidth())
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (v, t) ->
                    DropdownMenuItem(text = { Text(t) }, onClick = { onSelect(v); open = false })
                }
            }
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(error, color = RedStatus, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
    }
}

/* ============ 1) نموذج إضافة البنود (الصيانة) ============ */

private class ItemRowState {
    var kind by mutableStateOf("general")
    var name by mutableStateOf("")
    var note by mutableStateOf("")
    var odoCurrent by mutableStateOf("")
    var odoNext by mutableStateOf("")
    var tireCount by mutableStateOf("")
    var tirePos by mutableStateOf("front")
    var serviceCompany by mutableStateOf("")
    var photo by mutableStateOf<UploadFile?>(null)
}

@Composable
internal fun ItemsForm(id: Int, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val rows = remember { mutableStateListOf(ItemRowState()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val kinds = listOf(
        "general" to tr("عام","General"), "oil_change" to tr("تغيير زيت","Oil change"),
        "tire_change" to tr("تغيير إطارات","Tire change"), "other_periodic" to tr("دوري آخر","Other periodic")
    )

    FormCard(tr("إضافة البنود وإرسالها للمشتريات","Add items & send to purchasing")) {
        ErrorText(error)
        rows.forEachIndexed { i, row ->
            Surface(
                color = Panel2, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Line2), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("بند","Item") + " #${i + 1}", color = Txt, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (rows.size > 1) {
                            TextButton(onClick = { rows.removeAt(i) }) { Text(tr("حذف","Delete"), color = RedStatus) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LabeledDropdown(tr("النوع","Type"), kinds, row.kind) { row.kind = it }
                    Spacer(Modifier.height(8.dp))
                    MasarField(row.name, { row.name = it }, tr("اسم البند (مثال: فلتر/اصطدام)","Item name (e.g. filter/collision)"))
                    Spacer(Modifier.height(8.dp))
                    MasarField(row.note, { row.note = it }, tr("ملاحظة","Note"), singleLine = false)

                    when (row.kind) {
                        "oil_change" -> {
                            Spacer(Modifier.height(8.dp))
                            MasarField(row.odoCurrent, { row.odoCurrent = it }, tr("العداد الحالي","Current odometer"), keyboard = KeyboardType.Number)
                            Spacer(Modifier.height(8.dp))
                            MasarField(row.odoNext, { row.odoNext = it }, tr("عداد الزيت القادم","Next oil odometer"), keyboard = KeyboardType.Number)
                            Spacer(Modifier.height(8.dp))
                            MasarField(row.serviceCompany, { row.serviceCompany = it }, tr("شركة الخدمة","Service company"))
                        }
                        "tire_change" -> {
                            Spacer(Modifier.height(8.dp))
                            MasarField(row.tireCount, { row.tireCount = it }, tr("عدد الإطارات","Tire count"), keyboard = KeyboardType.Number)
                            Spacer(Modifier.height(8.dp))
                            LabeledDropdown(
                                tr("الموضع","Position"),
                                listOf("front" to tr("أمامي","Front"), "rear" to tr("خلفي","Rear"), "both" to tr("الكل","All")),
                                row.tirePos
                            ) { row.tirePos = it }
                            Spacer(Modifier.height(8.dp))
                            MasarField(row.serviceCompany, { row.serviceCompany = it }, tr("شركة الخدمة","Service company"))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PhotoPickerField(tr("صورة البند (اختياري)","Item photo (optional)"), row.photo, { row.photo = it })
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        GhostButton("＋ " + tr("بند آخر","Another item"), onClick = { rows.add(ItemRowState()) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        PrimaryButton(tr("إرسال للمشتريات","Send to purchasing"), loading = loading) {
            error = null; loading = true
            val items = rows.map { r ->
                val m = linkedMapOf<String, Any?>(
                    "kind" to r.kind,
                    "name" to r.name.ifBlank { kinds.firstOrNull { it.first == r.kind }?.second ?: r.kind },
                    "note" to r.note
                )
                if (r.kind == "oil_change") {
                    m["odometer_current"] = r.odoCurrent.ifBlank { null }
                    m["odometer_next"] = r.odoNext.ifBlank { null }
                    m["service_company"] = r.serviceCompany.ifBlank { null }
                } else if (r.kind == "tire_change") {
                    m["tire_count"] = r.tireCount.ifBlank { null }
                    m["tire_position"] = r.tirePos
                    m["service_company"] = r.serviceCompany.ifBlank { null }
                }
                m
            }
            val itemsJson = gson.toJson(items)
            val photos = mutableMapOf<String, UploadFile>()
            rows.forEachIndexed { i, r -> r.photo?.let { photos["item_photo_$i"] = it } }
            scope.launch {
                when (val res = Net.repo.maintenanceItems(id, itemsJson, photos)) {
                    is Outcome.Ok -> { loading = false; onDone() }
                    is Outcome.Err -> { loading = false; error = res.message }
                }
            }
        }
    }
}

/* ============ 2) نموذج التسعير (المشتريات) ============ */

@Composable
internal fun PriceForm(d: RequestDetail, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    val prices = remember { mutableStateMapOf<Int, String>() }
    val companyOf = remember { mutableStateMapOf<Int, Int>() }     // itemId -> companyId (0=none)
    val quoteOf = remember { mutableStateMapOf<Int, UploadFile?>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(d.id) {
        if (Net.repo.companies().let { it is Outcome.Ok }) {} // warm-up no-op kept simple below
    }
    LaunchedEffect(d.id) {
        when (val r = Net.repo.companies()) {
            is Outcome.Ok -> companies = r.data
            is Outcome.Err -> {}
        }
        d.items.forEach {
            prices[it.id] = it.price?.let { p -> fmtNum(p) } ?: ""
            companyOf[it.id] = it.companyId ?: 0
        }
    }

    val companyOptions = listOf(0 to tr("— شركة —","— Company —")) + companies.map { it.id to it.name }

    FormCard(tr("تسعير البنود + إرفاق عروض الأسعار","Price items + attach quotes")) {
        ErrorText(error)
        d.items.forEach { item ->
            Surface(
                color = Panel2, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Line2), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.name, color = Txt, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MasarField(prices[item.id] ?: "", { prices[item.id] = it }, tr("السعر (ر.س)","Price (SAR)"), keyboard = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    LabeledDropdown(
                        tr("المورّد","Supplier"),
                        companyOptions.map { it.first.toString() to it.second },
                        (companyOf[item.id] ?: 0).toString()
                    ) { companyOf[item.id] = it.toInt() }
                    Spacer(Modifier.height(10.dp))
                    PhotoPickerField(tr("إرفاق عرض سعر (صورة/PDF)","Attach quote (image/PDF)"), quoteOf[item.id], { quoteOf[item.id] = it }, allowPdf = true)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(tr("إرسال للإدارة","Send to admin"), loading = loading) {
            error = null; loading = true
            val priceList = d.items.map { item ->
                val m = linkedMapOf<String, Any?>("item_id" to item.id)
                m["price"] = (prices[item.id] ?: "").replace(",", "").ifBlank { null }
                val co = companyOf[item.id] ?: 0
                m["company_id"] = if (co > 0) co else null
                m
            }
            val pricesJson = gson.toJson(priceList)
            val quoteParts = mutableListOf<MultipartBody.Part>()
            val extra = mutableMapOf<String, String?>()
            var qi = 0
            d.items.forEach { item ->
                val uf = quoteOf[item.id]
                if (uf != null) {
                    quoteParts.add(Net.repo.quotePart("quote_file_$qi", uf))
                    extra["quote_item_$qi"] = item.id.toString()
                    val co = companyOf[item.id] ?: 0
                    if (co > 0) extra["quote_company_$qi"] = co.toString()
                    qi++
                }
            }
            scope.launch {
                when (val res = Net.repo.purchasingPrice(d.id, pricesJson, quoteParts, extra)) {
                    is Outcome.Ok -> { loading = false; onDone() }
                    is Outcome.Err -> { loading = false; error = res.message }
                }
            }
        }
    }
}

/* ============ 3) نموذج الاعتماد (الإدارة) ============ */

@Composable
internal fun ApproveForm(d: RequestDetail, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    val prices = remember { mutableStateMapOf<Int, String>() }
    val companyOf = remember { mutableStateMapOf<Int, Int>() }
    var note by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }
    var rejecting by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(d.id) {
        when (val r = Net.repo.companies()) {
            is Outcome.Ok -> companies = r.data
            is Outcome.Err -> {}
        }
        d.items.forEach {
            prices[it.id] = it.price?.let { p -> fmtNum(p) } ?: ""
            companyOf[it.id] = it.companyId ?: 0
        }
    }

    val companyOptions = listOf(0 to tr("— كما هو —","— As is —")) + companies.map { it.id to it.name }

    FormCard(tr("اعتماد الأسعار والموردين","Approve prices & suppliers")) {
        ErrorText(error)
        d.items.forEach { item ->
            Surface(
                color = Panel2, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Line2), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.name, color = Txt, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MasarField(prices[item.id] ?: "", { prices[item.id] = it }, tr("السعر المعتمد","Approved price"), keyboard = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    LabeledDropdown(
                        tr("المورّد المعتمد","Approved supplier"),
                        companyOptions.map { it.first.toString() to it.second },
                        (companyOf[item.id] ?: 0).toString()
                    ) { companyOf[item.id] = it.toInt() }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        MasarField(note, { note = it }, tr("ملاحظة الاعتماد (اختياري)","Approval note (optional)"), singleLine = false)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(tr("اعتماد وإرسال للمشتريات","Approve & send to purchasing"), loading = loading) {
            error = null; loading = true
            val approvals = d.items.map { item ->
                val m = linkedMapOf<String, Any?>("item_id" to item.id)
                val p = (prices[item.id] ?: "").replace(",", "")
                if (p.isNotBlank()) m["price"] = p
                val co = companyOf[item.id] ?: 0
                if (co > 0) m["company_id"] = co
                m
            }
            val approvalsJson = gson.toJson(approvals)
            scope.launch {
                when (val res = Net.repo.adminApprove(d.id, approvalsJson, note)) {
                    is Outcome.Ok -> { loading = false; onDone() }
                    is Outcome.Err -> { loading = false; error = res.message }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(14.dp))
        Text(tr("أو رفض الطلب وإعادته للمشتريات","Or reject & return to purchasing"), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        MasarField(rejectReason, { rejectReason = it }, tr("سبب الرفض","Rejection reason"), singleLine = false)
        Spacer(Modifier.height(10.dp))
        GhostButton(tr("رفض وإعادة للمشتريات","Reject & return to purchasing"), onClick = {
            if (rejectReason.isBlank()) { error = tr("اكتب سبب الرفض","Enter the rejection reason"); return@GhostButton }
            error = null; rejecting = true
            scope.launch {
                when (val res = Net.repo.adminReject(d.id, rejectReason)) {
                    is Outcome.Ok -> { rejecting = false; onDone() }
                    is Outcome.Err -> { rejecting = false; error = res.message }
                }
            }
        }, modifier = Modifier.fillMaxWidth())
    }
}

/* ============ 4) نموذج الإنهاء (الصيانة) ============ */

@Composable
internal fun CompleteForm(id: Int, currentOdo: Int?, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var odo by remember { mutableStateOf(currentOdo?.toString() ?: "") }
    var note by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<UploadFile?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    FormCard(tr("إنهاء الصيانة وإعادة السيارة","Finish maintenance & return car")) {
        ErrorText(error)
        Surface(color = Yellow.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("⚠ " + tr("راجع وتأكّد من قراءة عداد السيارة قبل الإغلاق.","Review and confirm the odometer reading before closing.") +
                (currentOdo?.let { " " + tr("العداد المسجّل: ","Recorded odometer: ") + "${fmtNum(it.toDouble())} " + tr("كم","km") } ?: ""),
                color = Yellow, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        MasarField(odo, { odo = it }, tr("العداد الحالي عند الإنهاء (إجباري)","Current odometer at completion (required)"), keyboard = KeyboardType.Number)
        Spacer(Modifier.height(10.dp))
        MasarField(note, { note = it }, tr("ملاحظة (اختياري)","Note (optional)"), singleLine = false)
        Spacer(Modifier.height(10.dp))
        PhotoPickerField(tr("صورة الإغلاق (اختياري)","Closing photo (optional)"), photo, { photo = it })
        Spacer(Modifier.height(14.dp))
        PrimaryButton(tr("إنهاء الصيانة وإعادة السيارة","Finish maintenance & return car"), loading = loading) {
            error = null
            if (odo.isBlank()) { error = tr("اكتب قراءة العداد عند الإنهاء (إجباري)","Enter the odometer reading at completion (required)"); return@PrimaryButton }
            loading = true
            scope.launch {
                when (val res = Net.repo.maintenanceComplete(id, odo, note, photo)) {
                    is Outcome.Ok -> { loading = false; onDone() }
                    is Outcome.Err -> { loading = false; error = res.message }
                }
            }
        }
    }
}

/* ============ 5) نموذج التسليم للصيانة (المشتريات) — الفرع + صورة ============ */

private class DeliverItemState {
    var branchId by mutableStateOf(0)
    var note by mutableStateOf("")
    var mapUrl by mutableStateOf("")
}

@Composable
internal fun DeliverForm(d: RequestDetail, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    val states = remember { mutableStateMapOf<Int, DeliverItemState>() }
    var photo by remember { mutableStateOf<UploadFile?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(d.id) {
        when (val r = Net.repo.companies()) { is Outcome.Ok -> companies = r.data; is Outcome.Err -> {} }
        d.items.forEach { if (states[it.id] == null) states[it.id] = DeliverItemState() }
    }

    FormCard(tr("تحديد موقع القطعة وتسليمها للصيانة","Set part location & deliver to maintenance")) {
        ErrorText(error)
        d.items.forEach { item ->
            val st = states[item.id] ?: DeliverItemState().also { states[item.id] = it }
            // فروع المورّد المعتمد لهذا البند
            val comp = companies.firstOrNull { it.id == (item.companyId ?: -1) }
            val branchOpts = listOf("0" to tr("— خارج الفروع (ملاحظة يدوية) —","— Outside branches (manual note) —")) +
                (comp?.branches ?: emptyList()).map { it.id.toString() to it.name }
            Surface(
                color = Panel2, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Line2), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.name + (item.companyName?.let { " — $it" } ?: ""), color = Txt, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LabeledDropdown(tr("الفرع","Branch"), branchOpts, st.branchId.toString()) { st.branchId = it.toIntOrNull() ?: 0 }
                    if (st.branchId == 0) {
                        Spacer(Modifier.height(8.dp))
                        MasarField(st.note, { st.note = it }, tr("اسم فرع/منطقة الاستلام","Branch/pickup area name"))
                        Spacer(Modifier.height(8.dp))
                        MasarField(st.mapUrl, { st.mapUrl = it }, tr("رابط موقع (خرائط قوقل)","Location link (Google Maps)"))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        PhotoPickerField(tr("صورة من المشتريات للصيانة (اختياري)","Purchasing-to-maintenance photo (optional)"), photo, { photo = it })
        Spacer(Modifier.height(14.dp))
        PrimaryButton(tr("تسليم القطعة للصيانة","Deliver part to maintenance"), loading = loading) {
            error = null; loading = true
            val branches = d.items.map { item ->
                val st = states[item.id]
                linkedMapOf<String, Any?>(
                    "item_id" to item.id,
                    "branch_id" to ((st?.branchId ?: 0).takeIf { it > 0 }),
                    "branch_note" to (st?.note?.ifBlank { null }),
                    "branch_map_url" to (st?.mapUrl?.ifBlank { null })
                )
            }
            val branchesJson = gson.toJson(branches)
            scope.launch {
                when (val res = Net.repo.purchasingDeliverFull(d.id, branchesJson, photo)) {
                    is Outcome.Ok -> { loading = false; onDone() }
                    is Outcome.Err -> { loading = false; error = res.message }
                }
            }
        }
    }
}
