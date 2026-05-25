package com.collabwise.ui.auth

import androidx.compose.foundation.background
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

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val appNameFont = FontFamily(Font(R.font.protest_riot_regular))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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

            EmailTextField(
                email = email,
                onEmailChange = { email = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordTextField(
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisible = { passwordVisible = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.login(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )

                } else {

                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold
                    )
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "New to CollabWise? ",
                    color = MaterialTheme.colorScheme.secondary
                )

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
                color = MaterialTheme.colorScheme.secondary
            )
        },

        modifier = Modifier.fillMaxWidth(),

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),

        singleLine = true,

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,

            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.secondary,

            cursorColor = MaterialTheme.colorScheme.primary,

            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),

        shape = RoundedCornerShape(12.dp),

        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
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
                color = MaterialTheme.colorScheme.secondary
            )
        },

        modifier = Modifier.fillMaxWidth(),

        visualTransformation =
            if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),

        trailingIcon = {

            IconButton(
                onClick = {
                    onPasswordVisible(!passwordVisible)
                }
            ) {

                Icon(
                    imageVector =
                        if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,

                    contentDescription = "Toggle password visibility",

                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },

        singleLine = true,

        colors = OutlinedTextFieldDefaults.colors(

            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,

            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.secondary,

            cursorColor = MaterialTheme.colorScheme.primary,

            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),

        shape = RoundedCornerShape(12.dp),

        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    )
}