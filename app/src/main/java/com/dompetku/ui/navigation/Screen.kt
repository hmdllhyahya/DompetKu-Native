package com.dompetku.ui.navigation

// ── Tab enum — used by FanNav + MainScaffold ──────────────────────────────────
enum class NavTab { Home, Profile, Transactions, Accounts, Analytics }

// ── Full-screen route destinations (outside FanNav shell) ─────────────────────
sealed class Screen(val route: String) {

    // ── Pre-auth ──────────────────────────────────────────────────────────────
    object Onboarding    : Screen("onboarding")
    object PinLock       : Screen("pin_lock")
    object PinSetup      : Screen("pin_setup?change={change}") {
        const val ARG_CHANGE = "change"
        fun createRoute(change: Boolean) = "pin_setup?change=$change"
    }

    // ── Main shell (hosts FanNav + all tab content) ───────────────────────────
    object Main          : Screen("main")

    // ── Full-screen destinations pushed on top of Main ────────────────────────
    object AccountDetail : Screen("account_detail/{accountId}") {
        const val ARG = "accountId"
        fun createRoute(accountId: String) = "account_detail/$accountId"
    }

    object MiniGame      : Screen("minigame")
}
