package com.collabwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.collabwise.data.repository.AuthState
import com.collabwise.ui.auth.LoginScreen
import com.collabwise.ui.auth.RegisterScreen
import com.collabwise.ui.dashboard.DashboardScreen
import com.collabwise.ui.group.GroupScreen
import com.collabwise.ui.navigation.Screen
import com.collabwise.ui.splash.SplashScreen
import com.collabwise.ui.theme.CollabwiseTheme
import com.collabwise.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CollabwiseTheme {

                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AppNavGraph(
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {

    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {

        when (authState) {

            AuthState.Loading -> Unit

            AuthState.Authenticated -> {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── SPLASH ───────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen()
        }

        // ── LOGIN ───────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // ── REGISTER ────────────────────────────
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ── DASHBOARD ───────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onGroupClick = { groupId ->
                    navController.navigate(
                        Screen.Group.createRoute(groupId)
                    )
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                // 🆕 Drawer navigation actions
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },

                // 🆕 Logout handling
                onLogout = {
                    viewModel.logout()
                }
            )
        }

        // ── GROUP ───────────────────────────
        composable(
            route = Screen.Group.route,
            arguments = listOf(
                navArgument(Screen.Group.ARG_GROUP_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            GroupScreen(
                onBack = {
                    navController.popBackStack()
                },
                onProjectClick = { groupId, projectId ->
                    navController.navigate(
                        Screen.Project.createRoute(groupId, projectId)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Text("Profile Screen", modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Skills") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Text("Skills Screen", modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Tasks") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Text("Tasks Screen", modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationsScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Organizations") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Text("Organizations Screen", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CollabwiseTheme {
        Greeting("Android")
    }
}