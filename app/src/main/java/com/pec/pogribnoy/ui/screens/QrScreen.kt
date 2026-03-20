package com.pec.pogribnoy.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.clickable
import coil.compose.SubcomposeAsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.pec.pogribnoy.ui.theme.BackgroundLight
import com.pec.pogribnoy.ui.theme.ButtonTeal
import com.pec.pogribnoy.ui.theme.QrCardBlue
import com.pec.pogribnoy.ui.theme.TextWhite
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import androidx.compose.ui.res.painterResource
import com.pec.pogribnoy.R
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext

import com.pec.pogribnoy.ui.theme.MoodSleepy
import com.pec.pogribnoy.ui.theme.MoodHappy
import com.pec.pogribnoy.ui.theme.MoodNeutral
import com.pec.pogribnoy.ui.theme.MoodTired

import com.pec.pogribnoy.ui.components.InitialsAvatar
import com.pec.pogribnoy.network.RetrofitClient
import com.pec.pogribnoy.network.StudentDto
import kotlinx.coroutines.launch

@Composable
fun QrScreen(
    uniqueCode: String,
    avatarUri: String?,
    mood: String,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var student by remember { mutableStateOf<StudentDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uniqueCode) {
        scope.launch {
            try {
                student = RetrofitClient.apiService.getStudent(uniqueCode)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                // Non-fatal error, display will use uniqueCode fallback
            }
        }
    }

    val qrColor = when (mood) {
        "sleepy" -> AndroidColor.parseColor("#1A237E") // Dark Blue
        "happy" -> AndroidColor.parseColor("#E65100")  // Vibrant Orange
        "tired" -> AndroidColor.parseColor("#37474F")  // Slate Gray
        else -> AndroidColor.BLACK
    }
    
    val qrBitmap = remember(student, qrColor) {
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo)
        // Use hash if available, otherwise fallback to code
        val qrContent = student?.hash ?: uniqueCode
        generateQrCodeWithLogo(qrContent, logo, qrColor)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Background Watermark (Widened and Shortened)
        Image(
            painter = painterResource(id = R.drawable.bg_qr),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = QrCardBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Area
                SubcomposeAsyncImage(
                    model = avatarUri ?: "",
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    loading = { InitialsAvatar(name = student?.fullName ?: "Студент", size = 140.dp) },
                    error = { InitialsAvatar(name = student?.fullName ?: "Студент", size = 140.dp) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = TextWhite)
                } else {
                    Text(
                        text = student?.fullName?.split(" ")?.take(2)?.joinToString(" ") ?: "Студент",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code in White Box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(0.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "QR-пропуск",
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonTeal,
                        contentColor = TextWhite
                    )
                ) {
                    Text(
                        text = "Личный кабинет",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "О приложении",
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { showAboutDialog = true }
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "О приложении", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Версия: 1.1.0 (Public Cloud Build)", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Что нового:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "• Поддержка единого облачного бэкенда\n" +
                                "• Переход на API v1 (/api/qr/)\n" +
                                "• Улучшена стабильность сетевых запросов\n" +
                                "• Оптимизация генерации QR-кодов\n" +
                                "• Подготовлен к масштабированию системы",
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
                    Text("Понятно", color = QrCardBlue)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

private fun generateQrCodeWithLogo(text: String, logo: Bitmap?, qrColor: Int): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val size = 512
        val hints = mutableMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        hints[EncodeHintType.MARGIN] = 1

        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) qrColor else AndroidColor.WHITE)
            }
        }

        if (logo != null) {
            val canvas = Canvas(bitmap)
            val logoSize = size / 4
            val left = (size - logoSize) / 2
            val top = (size - logoSize) / 2
            
            // Draw a white square behind the logo for better visibility
            val paint = Paint()
            paint.color = AndroidColor.WHITE
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + logoSize).toFloat(), (top + logoSize).toFloat(), paint)
            
            val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, false)
            canvas.drawBitmap(scaledLogo, left.toFloat(), top.toFloat(), null)
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
