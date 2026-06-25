package com.masar.maintenance.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masar.maintenance.data.UploadFile
import com.masar.maintenance.data.Uploads
import com.masar.maintenance.ui.imageUrl
import com.masar.maintenance.ui.tr
import com.masar.maintenance.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** حقل اختيار صورة/ملف مع معاينة وزر حذف. */
@Composable
fun PhotoPickerField(
    label: String,
    picked: UploadFile?,
    onPicked: (UploadFile?) -> Unit,
    modifier: Modifier = Modifier,
    allowPdf: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    val mime = if (allowPdf) "*/*" else "image/*"

    fun handleUri(uri: android.net.Uri?) {
        if (uri != null) {
            working = true
            scope.launch {
                val uf = withContext(Dispatchers.IO) { Uploads.prepare(context, uri) }
                working = false
                onPicked(uf)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> handleUri(uri) }

    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) handleUri(cameraUri) }

    fun launchCamera() {
        val file = java.io.File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Column(modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Surface(
            color = Ink2,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Line2),
            modifier = Modifier.fillMaxWidth().clickable(enabled = !working) {
                if (picked == null) showChooser = true
            }
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    working -> {
                        CircularProgressIndicator(color = Red, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("جارٍ التجهيز…","Preparing…"), color = Muted)
                    }
                    picked != null -> {
                        if (picked.mime.startsWith("image/")) {
                            AsyncImage(
                                model = picked.file,
                                contentDescription = null,
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("📄", fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(picked.fileName.take(24), color = Txt, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { onPicked(null) }) { Text(tr("حذف","Delete"), color = RedStatus) }
                    }
                    else -> {
                        Text("＋", fontSize = 20.sp, color = Red, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("اضغط لالتقاط صورة أو اختيارها","Tap to capture or choose a photo"), color = Muted)
                    }
                }
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            containerColor = Ink2,
            title = { Text(tr("إضافة صورة","Add photo"), color = Txt) },
            text = { Text(tr("اختر طريقة إضافة الصورة","Choose how to add the photo"), color = Muted) },
            confirmButton = {
                TextButton(onClick = { showChooser = false; launchCamera() }) {
                    Text("📷 " + tr("التقاط صورة","Take a photo"), color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChooser = false; galleryLauncher.launch(mime) }) {
                    Text(if (allowPdf) "🖼 " + tr("من الجهاز (صورة/PDF)","From device (image/PDF)") else "🖼 " + tr("من الجهاز","From device"), color = Txt)
                }
            }
        )
    }
}

/** صورة من الخادم (مسار نسبي uploads/..) مع بديل عند غيابها. */
@Composable
fun RemoteImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: String = "⛍"
) {
    val url = imageUrl(path)
    if (url.isNullOrBlank()) {
        Box(modifier.background(Ink2), contentAlignment = Alignment.Center) {
            Text(placeholder, color = Muted, fontSize = 22.sp)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
