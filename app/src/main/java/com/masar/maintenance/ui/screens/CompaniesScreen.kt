package com.masar.maintenance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.masar.maintenance.ui.tr
import com.masar.maintenance.data.Company
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.data.UploadFile
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CompaniesScreen(nav: NavController) {
    val role = Net.session.userRole
    var query by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var editTarget by remember { mutableStateOf<Company?>(null) }
    var showForm by remember { mutableStateOf(false) }

    MasarScaffold(title = tr("الشركات والموردون","Companies & suppliers"), onBack = { nav.popBackStack() }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                    MasarField(query, { query = it }, tr("ابحث باسم الشركة…","Search by company name…"))
                }
                RemoteContent(
                    reloadKey = "$query|$reload",
                    load = { Net.repo.companies(q = query) }
                ) { list ->
                    if (list.isEmpty()) {
                        EmptyBox(tr("لا توجد شركات مطابقة","No matching companies"), "∅")
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                        ) {
                            items(list, key = { it.id }) { co ->
                                CompanyCard(co, canEdit = role == "admin") {
                                    editTarget = co; showForm = true
                                }
                            }
                        }
                    }
                }
            }

            if (role == "admin") {
                ExtendedFloatingActionButton(
                    onClick = { editTarget = null; showForm = true },
                    containerColor = Red, contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(tr("إضافة شركة","Add company")) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                )
            }
        }
    }

    if (showForm) {
        CompanyFormDialog(
            existing = editTarget,
            onDismiss = { showForm = false },
            onSaved = { showForm = false; reload++ }
        )
    }
}

@Composable
private fun CompanyCard(co: Company, canEdit: Boolean, onEdit: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().then(if (canEdit) Modifier.clickable(onClick = onEdit) else Modifier)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                co.logo,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
                placeholder = "🏢"
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(co.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!co.contactPrimary.isNullOrBlank()) {
                    Text(co.contactPrimary, color = Muted, fontSize = 13.sp)
                }
                if (!co.unifiedNumber.isNullOrBlank()) {
                    Text(tr("الرقم الموحّد: ","Unified number: ") + "${co.unifiedNumber}", color = Muted, fontSize = 12.sp)
                }
            }
            if (canEdit) Text(tr("تعديل","Edit") + " ›", color = Red, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CompanyFormDialog(existing: Company?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val editing = existing != null

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var contact1 by remember { mutableStateOf(existing?.contactPrimary ?: "") }
    var contact2 by remember { mutableStateOf(existing?.contactSecondary ?: "") }
    var bank by remember { mutableStateOf(existing?.bankDetails ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var unified by remember { mutableStateOf(existing?.unifiedNumber ?: "") }
    var logo by remember { mutableStateOf<UploadFile?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!submitting && !deleting) onDismiss() },
        containerColor = Ink2,
        title = { Text(if (editing) tr("تعديل شركة","Edit company") else tr("إضافة شركة","Add company"), color = Txt) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (error != null) {
                    Text(error!!, color = RedStatus, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                }
                MasarField(name, { name = it }, tr("اسم الشركة *","Company name *"))
                Spacer(Modifier.height(10.dp))
                MasarField(contact1, { contact1 = it }, tr("هاتف/تواصل أساسي","Phone/primary contact"))
                Spacer(Modifier.height(10.dp))
                MasarField(contact2, { contact2 = it }, tr("تواصل ثانوي","Secondary contact"))
                Spacer(Modifier.height(10.dp))
                MasarField(unified, { unified = it }, tr("الرقم الموحّد","Unified number"))
                Spacer(Modifier.height(10.dp))
                MasarField(address, { address = it }, tr("العنوان","Address"), singleLine = false)
                Spacer(Modifier.height(10.dp))
                MasarField(bank, { bank = it }, tr("بيانات الحساب البنكي","Bank account details"), singleLine = false)
                Spacer(Modifier.height(12.dp))
                PhotoPickerField(
                    if (editing && !existing?.logo.isNullOrBlank()) tr("الشعار (لتغييره)","Logo (to change)") else tr("شعار الشركة","Company logo"),
                    logo, { logo = it }
                )
                if (editing) {
                    Spacer(Modifier.height(14.dp))
                    if (confirmDelete) {
                        Text(tr("تأكيد حذف الشركة؟","Confirm deleting the company?"), color = RedStatus, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            TextButton(
                                onClick = {
                                    deleting = true; error = null
                                    scope.launch {
                                        when (val r = Net.repo.deleteCompany(existing!!.id)) {
                                            is Outcome.Ok -> { deleting = false; onSaved() }
                                            is Outcome.Err -> { deleting = false; error = r.message; confirmDelete = false }
                                        }
                                    }
                                }
                            ) { Text(if (deleting) tr("جارٍ الحذف…","Deleting…") else tr("نعم، احذف","Yes, delete"), color = RedStatus) }
                            TextButton(onClick = { confirmDelete = false }) { Text(tr("تراجع","Back"), color = Muted) }
                        }
                    } else {
                        TextButton(onClick = { confirmDelete = true }) { Text(tr("حذف الشركة","Delete company"), color = RedStatus) }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButtonCompact(if (editing) tr("حفظ","Save") else tr("إضافة","Add"), loading = submitting) {
                error = null
                if (name.isBlank()) { error = tr("اسم الشركة مطلوب","Company name is required"); return@PrimaryButtonCompact }
                submitting = true
                val fields = buildMap<String, String?> {
                    put("action", if (editing) "update" else "create")
                    if (editing) put("id", existing!!.id.toString())
                    put("name", name)
                    put("contact_primary", contact1)
                    put("contact_secondary", contact2)
                    put("bank_details", bank)
                    put("address", address)
                    put("unified_number", unified)
                }
                scope.launch {
                    when (val r = Net.repo.saveCompany(fields, logo)) {
                        is Outcome.Ok -> { submitting = false; onSaved() }
                        is Outcome.Err -> { submitting = false; error = r.message }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting && !deleting) { Text(tr("إلغاء","Cancel"), color = Muted) }
        }
    )
}

/** زر أساسي مصغّر للاستخدام داخل الحوارات. */
@Composable
internal fun PrimaryButtonCompact(text: String, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Text(text, fontWeight = FontWeight.Bold)
    }
}
