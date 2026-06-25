package com.masar.maintenance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masar.maintenance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasarScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = Txt)
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink2, titleContentColor = Txt, actionIconContentColor = Red
                )
            )
        },
        content = content
    )
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Red)
    }
}

@Composable
fun EmptyBox(text: String, icon: String = "—") {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 40.sp, color = Muted)
        Spacer(Modifier.height(8.dp))
        Text(text, color = Muted)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠", fontSize = 36.sp, color = RedStatus)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Muted)
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onRetry) { Text("إعادة المحاولة") }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SectionTitle(text: String) {
    Row(Modifier.padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(3.dp)).background(Red))
        Spacer(Modifier.width(9.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Txt)
    }
}

@Composable
fun MasarCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Panel, shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun MasarField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Red, unfocusedBorderColor = Line2,
            focusedLabelColor = Red, cursorColor = Red,
            focusedTextColor = Txt, unfocusedTextColor = Txt,
            focusedContainerColor = Ink2, unfocusedContainerColor = Ink2
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Txt),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) { Text(text) }
}

@Composable
fun Avatar(letter: String, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Red),
        contentAlignment = Alignment.Center
    ) { Text(letter, color = Color.White, fontWeight = FontWeight.Bold) }
}

/* ===== حالة تحميل بيانات عامة ===== */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Data<T>(val value: T) : UiState<T>
}

@Composable
fun <T> RemoteContent(
    reloadKey: Any?,
    load: suspend () -> com.masar.maintenance.data.Outcome<T>,
    content: @Composable (T) -> Unit
) {
    var state by remember(reloadKey) { mutableStateOf<UiState<T>>(UiState.Loading) }
    LaunchedEffect(reloadKey) {
        state = UiState.Loading
        state = when (val o = load()) {
            is com.masar.maintenance.data.Outcome.Ok -> UiState.Data(o.data)
            is com.masar.maintenance.data.Outcome.Err -> UiState.Error(o.message)
        }
    }
    when (val s = state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message)
        is UiState.Data -> content(s.value)
    }
}
