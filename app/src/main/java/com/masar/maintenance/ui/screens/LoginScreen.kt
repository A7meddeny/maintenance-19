package com.masar.maintenance.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import com.masar.maintenance.ui.tr
import com.masar.maintenance.R
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Outcome
import com.masar.maintenance.ui.components.*
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf(Net.session.baseUrl) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = tr("مسار","MASAR"),
            colorFilter = ColorFilter.tint(Red),
            modifier = Modifier.size(110.dp)
        )
        Text(tr("نظام مسار للصيانة","MASAR Maintenance"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Txt)
        Text(tr("تسجيل الدخول","Sign in"), color = Muted)
        Spacer(Modifier.height(28.dp))

        MasarField(base, { base = it }, tr("رابط الخادم (مثال: https://site.com)","Server URL (e.g. https://site.com)"))
        Spacer(Modifier.height(12.dp))
        MasarField(user, { user = it }, tr("اسم المستخدم","Username"))
        Spacer(Modifier.height(12.dp))
        MasarField(pass, { pass = it }, tr("كلمة المرور","Password"), password = true)

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = RedStatus, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(tr("دخول","Login"), loading = loading) {
            error = null
            if (base.isBlank() || user.isBlank() || pass.isBlank()) {
                error = tr("أدخل رابط الخادم واسم المستخدم وكلمة المرور","Enter the server URL, username and password"); return@PrimaryButton
            }
            loading = true
            scope.launch {
                val device = "Android ${Build.VERSION.RELEASE} - ${Build.MODEL}"
                when (val r = Net.repo.login(base, user.trim(), pass, device)) {
                    is Outcome.Ok -> { loading = false; onLoggedIn() }
                    is Outcome.Err -> { loading = false; error = r.message }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
