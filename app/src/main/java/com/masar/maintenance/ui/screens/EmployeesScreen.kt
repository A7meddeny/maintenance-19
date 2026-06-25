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
import com.masar.maintenance.data.Employee
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.data.UploadFile
import com.masar.maintenance.ui.Labels
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

private val ROLE_OPTIONS = listOf(
    "office" to "موظف المكتب",
    "maintenance" to "قسم الصيانة",
    "purchasing" to "قسم المشتريات",
    "admin" to "الإدارة"
)

@Composable
fun EmployeesScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var editTarget by remember { mutableStateOf<Employee?>(null) }
    var showForm by remember { mutableStateOf(false) }

    MasarScaffold(title = tr("الموظفون والمستخدمون","Employees & users"), onBack = { nav.popBackStack() }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                    MasarField(query, { query = it }, tr("ابحث بالاسم أو الكود أو اسم الدخول…","Search by name, code or username…"))
                }
                RemoteContent(
                    reloadKey = "$query|$reload",
                    load = { Net.repo.employees(q = query) }
                ) { list ->
                    if (list.isEmpty()) {
                        EmptyBox(tr("لا يوجد موظفون مطابقون","No matching employees"), "∅")
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                        ) {
                            items(list, key = { it.id }) { e ->
                                EmployeeCard(e) { editTarget = e; showForm = true }
                            }
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { editTarget = null; showForm = true },
                containerColor = Red, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(tr("إضافة موظف","Add employee")) },
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            )
        }
    }

    if (showForm) {
        EmployeeFormDialog(
            existing = editTarget,
            onDismiss = { showForm = false },
            onSaved = { showForm = false; reload++ }
        )
    }
}

@Composable
private fun EmployeeCard(e: Employee, onEdit: () -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!e.photo.isNullOrBlank()) {
                RemoteImage(e.photo, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)), placeholder = "👤")
            } else {
                Avatar(e.name.take(1).ifBlank { tr("؟","?") }, size = 46)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.name, color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (e.status != "active") {
                        Spacer(Modifier.width(8.dp))
                        Badge(tr("موقوف","Suspended"), RedStatus)
                    }
                }
                Text(
                    buildString {
                        append(Labels.role(e.role))
                        if (e.username.isNotBlank()) append(" · ${e.username}")
                    },
                    color = Muted, fontSize = 13.sp
                )
                if (e.iqamaDays != null) {
                    Spacer(Modifier.height(4.dp))
                    val txt = if (e.iqamaDays < 0) tr("الإقامة منتهية منذ ${-e.iqamaDays} يوم","Iqama expired ${-e.iqamaDays} days ago") else tr("الإقامة تنتهي خلال ${e.iqamaDays} يوم","Iqama expires in ${e.iqamaDays} days")
                    Badge(txt, Labels.daysColor(e.iqamaDays))
                }
            }
            Text(tr("تعديل","Edit") + " ›", color = Red, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmployeeFormDialog(existing: Employee?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val editing = existing != null

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var code by remember { mutableStateOf(existing?.employeeCode ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(existing?.role ?: "office") }
    var iqama by remember { mutableStateOf(existing?.iqamaExpiry ?: "") }
    var active by remember { mutableStateOf((existing?.status ?: "active") == "active") }
    var photo by remember { mutableStateOf<UploadFile?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!submitting && !deleting) onDismiss() },
        containerColor = Ink2,
        title = { Text(if (editing) tr("تعديل موظف","Edit employee") else tr("إضافة موظف","Add employee"), color = Txt) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (error != null) {
                    Text(error!!, color = RedStatus, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                }
                MasarField(name, { name = it }, tr("الاسم *","Name *"))
                Spacer(Modifier.height(10.dp))
                MasarField(code, { code = it }, tr("كود الموظف","Employee code"))
                Spacer(Modifier.height(10.dp))
                MasarField(username, { username = it }, tr("اسم الدخول *","Username *"))
                Spacer(Modifier.height(10.dp))
                MasarField(
                    password, { password = it },
                    if (editing) tr("كلمة المرور (اتركها فارغة لإبقائها)","Password (leave empty to keep)") else tr("كلمة المرور *","Password *"),
                    password = true
                )
                Spacer(Modifier.height(10.dp))
                LabeledDropdown(tr("الدور","Role"), ROLE_OPTIONS.map { it.first to Labels.role(it.first) }, role) { role = it }
                Spacer(Modifier.height(10.dp))
                MasarField(iqama, { iqama = it }, tr("انتهاء الإقامة (YYYY-MM-DD)","Iqama expiry (YYYY-MM-DD)"))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = active, onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Red, checkedTrackColor = Red.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (active) tr("نشِط","Active") else tr("موقوف","Suspended"), color = Txt)
                }
                Spacer(Modifier.height(12.dp))
                PhotoPickerField(
                    if (editing && !existing?.photo.isNullOrBlank()) tr("صورة الموظف (لتغييرها)","Employee photo (to change)") else tr("صورة الموظف","Employee photo"),
                    photo, { photo = it }
                )
                if (editing) {
                    Spacer(Modifier.height(14.dp))
                    if (confirmDelete) {
                        Text(tr("تأكيد حذف الموظف؟","Confirm deleting the employee?"), color = RedStatus, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            TextButton(
                                onClick = {
                                    deleting = true; error = null
                                    scope.launch {
                                        when (val r = Net.repo.deleteEmployee(existing!!.id)) {
                                            is Outcome.Ok -> { deleting = false; onSaved() }
                                            is Outcome.Err -> { deleting = false; error = r.message; confirmDelete = false }
                                        }
                                    }
                                }
                            ) { Text(if (deleting) tr("جارٍ الحذف…","Deleting…") else tr("نعم، احذف","Yes, delete"), color = RedStatus) }
                            TextButton(onClick = { confirmDelete = false }) { Text(tr("تراجع","Back"), color = Muted) }
                        }
                    } else {
                        TextButton(onClick = { confirmDelete = true }) { Text(tr("حذف الموظف","Delete employee"), color = RedStatus) }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButtonCompact(if (editing) tr("حفظ","Save") else tr("إضافة","Add"), loading = submitting) {
                error = null
                when {
                    name.isBlank() -> error = tr("الاسم مطلوب","Name is required")
                    username.isBlank() -> error = tr("اسم الدخول مطلوب","Username is required")
                    !editing && password.isBlank() -> error = tr("كلمة المرور مطلوبة","Password is required")
                    else -> {
                        submitting = true
                        val fields = buildMap<String, String?> {
                            put("action", if (editing) "update" else "create")
                            if (editing) put("id", existing!!.id.toString())
                            put("name", name)
                            put("employee_code", code)
                            put("username", username)
                            if (password.isNotBlank()) put("password", password)
                            put("role", role)
                            put("iqama_expiry", iqama)
                            put("status", if (active) "active" else "inactive")
                        }
                        scope.launch {
                            when (val r = Net.repo.saveEmployee(fields, photo)) {
                                is Outcome.Ok -> { submitting = false; onSaved() }
                                is Outcome.Err -> { submitting = false; error = r.message }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting && !deleting) { Text(tr("إلغاء","Cancel"), color = Muted) }
        }
    )
}
