package com.pec.pogribnoy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pec.pogribnoy.R
import com.pec.pogribnoy.network.RetrofitClient
import com.pec.pogribnoy.network.StudentDto
import com.pec.pogribnoy.ui.components.InitialsAvatar
import com.pec.pogribnoy.ui.theme.BackgroundLight
import com.pec.pogribnoy.ui.theme.ButtonTeal
import com.pec.pogribnoy.ui.theme.QrCardBlue
import com.pec.pogribnoy.ui.theme.TextWhite
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File as JavaFile
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uniqueCode: String,
    avatarUri: String?,
    onAvatarChange: (Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    var student by remember { mutableStateOf<StudentDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(uniqueCode) {
        scope.launch {
            try {
                student = RetrofitClient.apiService.getStudent(uniqueCode)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onAvatarChange(selectedUri)
            // Trigger server upload
            scope.launch {
                try {
                    isUploading = true
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val tempFile = JavaFile.createTempFile("avatar", ".jpg", context.cacheDir)
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    
                    val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                    
                    RetrofitClient.apiService.uploadAvatar(uniqueCode, body)
                    
                    // Refresh student data to get new avatar_base64
                    student = RetrofitClient.apiService.getStudent(uniqueCode)
                    isUploading = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    isUploading = false
                }
            }
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Background Watermark
        Image(
            painter = painterResource(id = R.drawable.bg_qr),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.25f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 100.dp, start = 24.dp, end = 24.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar

            // Avatar
            val studentBitmap = remember(student?.avatarBase64) {
                student?.avatarBase64?.let { base64String ->
                    try {
                        val cleanBase64 = if (base64String.contains(",")) {
                            base64String.substringAfter(",")
                        } else {
                            base64String
                        }
                        val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (studentBitmap != null) {
                Image(
                    bitmap = studentBitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
            } else {
                SubcomposeAsyncImage(
                    model = avatarUri ?: "",
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(32.dp)),
                    loading = { InitialsAvatar(name = student?.fullName ?: "Студент", size = 160.dp) },
                    error = { InitialsAvatar(name = student?.fullName ?: "Студент", size = 160.dp) }
                )
            }

            if (isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.width(160.dp), color = ButtonTeal)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonTeal),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(text = "Сменить аватарку", color = TextWhite, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = QrCardBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uniqueCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextWhite.copy(alpha = 0.7f)
                        )
                        
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = TextWhite, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        ProfileInfoField(
                            label = "Образовательная организация",
                            value = student?.organization ?: "Не указано"
                        )
                        ProfileInfoField(label = "ФИО", value = student?.fullName ?: "Не указано")
                        ProfileInfoField(label = "Дата выдачи студенческого пропуска", value = student?.issueDate ?: "Не указано")
                        ProfileInfoField(label = "Код специальности", value = student?.specialty ?: "Не указано")
                        ProfileInfoField(label = "Курс", value = student?.course ?: "Не указано")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "О приложении",
                        color = TextWhite.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true },
                        textAlign = TextAlign.Center
                    )
                }
            }

        }

        // Top Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(48.dp)
                .background(ButtonTeal, RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextWhite
            )
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "О приложении", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Версия: 1.2.0 (Persistent Avatars Build)", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Что нового:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "• Полноценная поддержка аватарок профиля\n" +
                                "• Синхронизация данных с MongoDB Atlas\n" +
                                "• Постоянное хранение данных (не удаляются при перезагрузке)\n" +
                                "• Оптимизация отображения Base64 изображений\n" +
                                "• Исправлены ошибки при смене фото",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Пользовательское соглашение:\n\nИспользуя это приложение, вы соглашаетесь с условиями хранения и обработки ваших данных в соответствии с политикой конфиденциальности учебного заведения.\n\nРазработано для студентов ПЭК ГГТУ.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Понятно", color = ButtonTeal)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun ProfileInfoField(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextWhite.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            lineHeight = 18.sp
        )
    }
}
