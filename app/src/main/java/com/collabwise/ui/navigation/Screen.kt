package com.collabwise.ui.navigation

sealed class Screen(val route: String) {

    // ── Public ────────────────────────────────────────────────────────────────
    data object Login : Screen("login")
    data object Register : Screen("register")

    // ── Authenticated ─────────────────────────────────────────────────────────
    data object Dashboard : Screen("dashboard")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")

    // ── Organization ──────────────────────────────────────────────────────────
    data object Organization : Screen("organization/{organizationId}") {

        const val ARG_ORGANIZATION_ID = "organizationId"

        fun createRoute(organizationId: String): String {
            return "organization/$organizationId"
        }
    }

    // ── Project ───────────────────────────────────────────────────────────────
    data object Project : Screen("project/{organizationId}/{projectId}") {

        const val ARG_ORGANIZATION_ID = "organizationId"
        const val ARG_PROJECT_ID = "projectId"

        fun createRoute(
            organizationId: String,
            projectId: String
        ): String {
            return "project/$organizationId/$projectId"
        }
    }
}