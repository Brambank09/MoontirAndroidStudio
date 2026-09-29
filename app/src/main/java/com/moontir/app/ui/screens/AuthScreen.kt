package com.moontir.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.MoontirLogoView
import com.moontir.app.ui.components.MoontirTextField
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onToggleLanguage: () -> Unit,
    currentLanguage: String,
    busy: Boolean,
    errorMessage: String?,
    isMockMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    var mode by remember { mutableStateOf("login") } // "login" or "register"
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
    ) {
        // Top Right Language Chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceTertiary,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable { onToggleLanguage() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Language",
                    tint = colors.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentLanguage.uppercase(),
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Branding
            MoontirLogoView(height = 42.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.width(28.dp).height(1.dp).background(colors.brand))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tagline.uppercase(),
                color = colors.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            if (isMockMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brandTertiary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Text(
                        text = if (currentLanguage == "id") "MODE STANDALONE (SIAP PAKAI)" else "STANDALONE MODE (READY TO USE)",
                        color = colors.moonGlow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Header Title
            Text(
                text = strings.welcome,
                color = colors.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (mode == "login") {
                    if (currentLanguage == "id") "Masuk untuk melanjutkan perawatan mobilmu." else "Sign in to continue your car's care."
                } else {
                    if (currentLanguage == "id") "Buat akun untuk mengatur perawatan pertamamu." else "Create an account to schedule your first care session."
                },
                color = colors.muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceTertiary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (mode == "register") {
                        MoontirTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = strings.name,
                            leadingIcon = Icons.Outlined.Person
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    MoontirTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = strings.email,
                        leadingIcon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MoontirTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = strings.password,
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = colors.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    MoontirButton(
                        text = if (mode == "login") strings.signIn else strings.create,
                        onClick = {
                            if (mode == "login") onLogin(email, password)
                            else onRegister(name, email, password)
                        },
                        busy = busy
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch Mode Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        mode = if (mode == "login") "register" else "login"
                    }
                    .padding(8.dp)
            ) {
                Text(
                    text = if (mode == "login") "${strings.newHere} " else "${strings.haveAccount} ",
                    color = colors.muted,
                    fontSize = 13.sp
                )
                Text(
                    text = if (mode == "login") strings.create else strings.signIn,
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
