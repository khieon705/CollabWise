package com.collabwise.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collabwise.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    fun isValid(): Boolean {
        return name.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.length >= 8
    }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(100.dp))

            Text(
                text = "Create Account",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(40.dp))

            RegisterNameTextField(
                name = name,
                onNameChange = { name = it }
            )

            Spacer(Modifier.height(12.dp))

            RegisterEmailTextField(
                email = email,
                onEmailChange = { email = it }
            )

            Spacer(Modifier.height(12.dp))

            RegisterPasswordTextField(
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisible = { passwordVisible = it }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!isValid()) return@Button
                    viewModel.register(name, email, password)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
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
                        text = "Create Account",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onNavigateToLogin()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterEmailTextField(
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

        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email Icon",
                tint = MaterialTheme.colorScheme.primary
            )
        },

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
fun RegisterPasswordTextField(
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

        singleLine = true,

        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Password,
                contentDescription = "Password Icon",
                tint = MaterialTheme.colorScheme.primary
            )
        },

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
fun RegisterNameTextField(
    name: String,
    onNameChange: (String) -> Unit
) {

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,

        label = {
            Text(
                text = "Name",
                color = MaterialTheme.colorScheme.secondary
            )
        },

        modifier = Modifier.fillMaxWidth(),

        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),

        singleLine = true,

        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Person Icon",
                tint = MaterialTheme.colorScheme.primary
            )
        },

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