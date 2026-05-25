package com.collabwise.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collabwise.viewmodel.AuthViewModel
import com.collabwise.R
import androidx.compose.runtime.getValue

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {

    // ── FORM STATE ─────────────────────────────────────────────
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── VIEWMODEL STATE ────────────────────────────────────────
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // ── NAVIGATION IS HANDLED BY NAVHOST ───────────────────────
    // (so we do NOTHING here)

    // Optional: just clear error display
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val appNameFont = FontFamily(Font(R.font.protest_riot_regular))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "CollabWise",
                fontFamily = appNameFont,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(50.dp))

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PASSWORD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector =
                                if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LOGIN BUTTON
            Button(
                onClick = {
                    viewModel.login(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Forgot password?",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            // REGISTER NAVIGATION ONLY (allowed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("New to CollabWise? ")

                Text(
                    text = "Sign up",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onNavigateToRegister()
                    }
                )
            }
        }
    }
}

@Composable
fun EmailTextField(
    email: String,
    onEmailChange: (String) -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = {
            Text(
                text = "Email",
                color = MaterialTheme.colorScheme.tertiary
            )
        },
        modifier = Modifier
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF008C8C),
            unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
            focusedTextColor = MaterialTheme.colorScheme.tertiary,
            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
            cursorColor = MaterialTheme.colorScheme.tertiary
        ),
        shape = RoundedCornerShape(4.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary
        )
    )
}

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisible: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = {
            Text(
                text = "Password",
                color = MaterialTheme.colorScheme.tertiary
            )
        },
        modifier = Modifier
            .fillMaxWidth(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(
                onClick = { onPasswordVisible(!passwordVisible) }
            ) {
                Icon(
                    imageVector = if (passwordVisible)
                        Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Toggle password visibility",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF008C8C),
            unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
            focusedTextColor = MaterialTheme.colorScheme.tertiary,
            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
            cursorColor = MaterialTheme.colorScheme.tertiary
        ),
        shape = RoundedCornerShape(4.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary
        )
    )
}