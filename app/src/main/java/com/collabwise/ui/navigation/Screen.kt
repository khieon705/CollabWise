package com.collabwise.ui.navigation

sealed class Screen(val route: String) {

    // ── Splash ───────────────────────────────────────────────────────────────
    data object Splash : Screen("splash")

    // ── Public ───────────────────────────────────────────────────────────────
    data object Login : Screen("login")
    data object Register : Screen("register")

    // ── Authenticated ────────────────────────────────────────────────────────
    data object Dashboard : Screen("dashboard")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")


    // ── Group ────────────────────────────────────────────────────────────────
    data object Group : Screen("group/{groupId}") {

        const val ARG_GROUP_ID = "groupId"

        fun createRoute(groupId: String): String {
            return "group/$groupId"
        }
    }

    // ── Project ──────────────────────────────────────────────────────────────
    data object Project : Screen("project/{groupId}/{projectId}") {

        const val ARG_GROUP_ID = "groupId"
        const val ARG_PROJECT_ID = "projectId"

        fun createRoute(
            groupId: String,
            projectId: String
        ): String {
            return "project/$groupId/$projectId"
        }
    }
}