package com.pec.pogribnoy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pec.pogribnoy.R
import com.pec.pogribnoy.ui.theme.*
import com.pec.pogribnoy.network.RetrofitClient
import com.pec.pogribnoy.network.LoginRequestDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToQr: (String, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf("neutral") }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val moods = listOf(
        "sleepy" to "😴",
        "happy" to "😊",
        "neutral" to "😐",
        "tired" to "😫"
    )

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
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo at Top
            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(140.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // Center Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AuthCardBlue)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Авторизация",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Уникальный код",
                            color = TextWhite,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        TextField(
                            value = code,
                            onValueChange = { code = it },
                            placeholder = { Text("Введите ваш код...", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color.White, RoundedCornerShape(4.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mood Selection
                    Text(
                        text = "Твое настроение сегодня?",
                        color = TextWhite,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moods.forEach { (moodName, emoji) ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        if (selectedMood == moodName) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedMood = moodName },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 28.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val scope = rememberCoroutineScope()
                    var isLoading by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.login(
                                        LoginRequestDto(code, selectedMood)
                                    )
                                    isLoading = false
                                    val safeId = response.id ?: ""
                                    if (safeId.isNotEmpty()) {
                                        onNavigateToQr(safeId, selectedMood)
                                    } else {
                                        errorMessage = "Ошибка сервера: пустой ID пользователя"
                                        showErrorDialog = true
                                    }
                                } catch (e: java.net.ConnectException) {
                                    isLoading = false
                                    errorMessage = "Не удалось подключиться к серверу. Убедитесь, что сервер запущен и адрес IP верен."
                                    showErrorDialog = true
                                } catch (e: retrofit2.HttpException) {
                                    isLoading = false
                                    errorMessage = if (e.code() == 401) "Неверный уникальный код" else "Ошибка сервера: ${e.code()}"
                                    showErrorDialog = true
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Произошла непредвиденная ошибка: ${e.localizedMessage}"
                                    showErrorDialog = true
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonTeal,
                            contentColor = TextWhite
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Войти",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
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
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text(text = "Ошибка", fontWeight = FontWeight.Bold) },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("ОК", color = AuthCardBlue)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
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
                    Text("Понятно", color = AuthCardBlue)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}
