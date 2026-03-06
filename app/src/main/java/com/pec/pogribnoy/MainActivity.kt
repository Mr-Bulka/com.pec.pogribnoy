package com.pec.pogribnoy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pec.pogribnoy.ui.screens.AuthScreen
import com.pec.pogribnoy.ui.screens.ProfileScreen
import com.pec.pogribnoy.ui.screens.QrScreen
import com.pec.pogribnoy.ui.theme.QR_AppTheme
import androidx.navigation.NavType
import androidx.navigation.navArgument

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QR_AppTheme {
                val navController = rememberNavController()
                val sharedPrefs = remember { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                
                var avatarUriString by remember { 
                    mutableStateOf(sharedPrefs.getString("avatar_uri", null)) 
                }

                NavHost(
                    navController = navController,
                    startDestination = "auth"
                ) {
                    composable("auth") {
                        AuthScreen(
                            onNavigateToQr = { uniqueCode, mood ->
                                val encodedCode = URLEncoder.encode(uniqueCode, StandardCharsets.UTF_8.toString())
                                navController.navigate("qr?code=$encodedCode&mood=$mood")
                            }
                        )
                    }

                    composable(
                        route = "qr?code={uniqueCode}&mood={mood}",
                        arguments = listOf(
                            navArgument("uniqueCode") { 
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("mood") {
                                type = NavType.StringType
                                defaultValue = "neutral"
                            }
                        )
                    ) { backStackEntry ->
                        val code = backStackEntry.arguments?.getString("uniqueCode") ?: ""
                        val mood = backStackEntry.arguments?.getString("mood") ?: "neutral"
                        QrScreen(
                            uniqueCode = code,
                            avatarUri = avatarUriString,
                            mood = mood,
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            avatarUri = avatarUriString,
                            onAvatarChange = { newUri ->
                                avatarUriString = newUri.toString()
                                sharedPrefs.edit().putString("avatar_uri", avatarUriString).apply()
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}