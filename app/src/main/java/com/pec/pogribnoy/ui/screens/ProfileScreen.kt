package com.pec.pogribnoy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pec.pogribnoy.ui.theme.BackgroundLight
import com.pec.pogribnoy.ui.theme.ButtonTeal
import com.pec.pogribnoy.ui.theme.TextWhite

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.pec.pogribnoy.R

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.pec.pogribnoy.ui.components.InitialsAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    avatarUri: String?,
    onAvatarChange: (Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAvatarChange(it) }
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 100.dp, start = 24.dp, end = 24.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
            } else {
                InitialsAvatar(name = "Погрибной Максим", size = 160.dp)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            text = "nFuvUG6qzp3s",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileInfoField(
                        label = "Образовательная организация",
                        value = "Государственное образовательное учреждение высшего образования московской области Государственный гуманитарно-технологический университет Промышленно-экономический колледж"
                    )
                    ProfileInfoField(label = "ФИО", value = "Погрибной Максим Юрьевич")
                    ProfileInfoField(label = "Дата выдачи студенческого пропуска", value = "01.09.2023")
                    ProfileInfoField(label = "Код специальности", value = "09.02.07 «Информационные системы и программирование»")
                    ProfileInfoField(label = "Курс", value = "3")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "О приложении",
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable { showAboutDialog = true }
            )
        }

        // Top Back Button (placed after Column to be on top)
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
                    Text(text = "Версия: 1.0.2 (Stable)", fontWeight = FontWeight.SemiBold)
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
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            lineHeight = 18.sp
        )
    }
}
