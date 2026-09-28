package ir.mahroch.tapekhash.ui.screens.tape

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ir.mahroch.tapekhash.data.ApiClient
import ir.mahroch.tapekhash.data.ApiException
import ir.mahroch.tapekhash.data.Session
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@Composable
fun TapeAddTab() {
    var boxCode by remember { mutableStateOf("") }
    var tapeCode by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    fun submit() {
        if (boxCode.isBlank() || tapeCode.isBlank()) {
            error = "کد باکس و کد تپه را وارد کنید."
            return
        }
        if (!Regex("^[BGbg][0-9]+$").matches(tapeCode.trim())) {
            error = "کد تپه باید مانند G1 یا B22 باشد."
            return
        }
        loading = true; error = null; message = null

        scope.launch {
            try {
                var imageCode = ""

                if (imageUri != null) {
                    // ۱. فایل انتخاب‌شده را به یک فایل موقت کپی کن
                    val mime = context.contentResolver.getType(imageUri!!) ?: "image/jpeg"
                    val ext = when {
                        mime.contains("png") -> "png"
                        mime.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(imageUri!!)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }

                    // ۲. آپلود از طریق API اصلی (با توکن نشست کاربر، نه کلید مخفی جدا)
                    val uploadRes = ApiClient.uploadMultipart(
                        url = Session.baseUrl + "?action=uploadTapeImage",
                        fileField = "file",
                        file = tempFile,
                        mimeType = mime,
                        auth = true
                    )
                    imageCode = uploadRes.optString("name")
                    tempFile.delete()
                }

                // ۴. ثبت/به‌روزرسانی تپه با کد عکس (اگر عکسی انتخاب نشده بود، امکان دارد سرور
                // به‌صورت خودکار عکس موجود در هاست GOL/BON را بعداً پیدا کند)
                val body = JSONObject()
                    .put("boxCode", boxCode.trim())
                    .put("tapeCode", tapeCode.trim())
                if (imageCode.isNotBlank()) body.put("imageCode", imageCode)
                else body.put("imageCode", tapeCode.trim()) // برای Gxxx/Bxxx خودش کد عکس معتبر است

                val res = ApiClient.call("addTape", body)
                message = if (res.optString("mode") == "image-added")
                    "عکس جدید به تپه‌ی موجود اضافه شد."
                else
                    "تپه با موفقیت ثبت شد."

                boxCode = ""; tapeCode = ""; imageUri = null
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "خطا در ارتباط با سرور."
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ثبت تپه‌ی جدید یا افزودن عکس", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = boxCode, onValueChange = { boxCode = it },
            label = { Text("کد باکس") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = tapeCode, onValueChange = { tapeCode = it },
            label = { Text("کد تپه (مثل G12 یا B34)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (imageUri == null) "انتخاب عکس از گالری (اختیاری)" else "تغییر عکس انتخاب‌شده")
        }

        imageUri?.let { uri ->
            Spacer(Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(message!!, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { submit() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("ثبت")
        }
    }
}
