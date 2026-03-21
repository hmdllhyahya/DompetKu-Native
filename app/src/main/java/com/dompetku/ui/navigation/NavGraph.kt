package com.dompetku.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.dompetku.ui.MainScaffold
import com.dompetku.ui.RootViewModel
import com.dompetku.ui.screen.MiniGameScreen
import com.dompetku.ui.screen.accounts.AccountDetailScreen
import com.dompetku.ui.screen.accounts.AccountFormSheet
import com.dompetku.ui.screen.accounts.AccountsScreen
import com.dompetku.ui.screen.accounts.AccountsViewModel
import com.dompetku.ui.screen.analytics.AnalyticsScreen
import com.dompetku.ui.screen.home.HomeScreen
import com.dompetku.ui.screen.onboarding.OnboardingScreen
import com.dompetku.ui.screen.pin.PinLockScreen
import com.dompetku.ui.screen.pin.PinMode
import com.dompetku.ui.screen.pin.PinSetupScreen
import com.dompetku.ui.screen.profile.ProfileScreen
import com.dompetku.ui.screen.transactions.TransactionsScreen
import com.dompetku.ui.screen.transactions.TransactionsViewModel

@Composable
fun DompetKuNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel     = hiltViewModel(),
) {
    val startDestination by rootViewModel.startDestination.collectAsStateWithLifecycle()
    val shouldLock       by rootViewModel.shouldLock.collectAsStateWithLifecycle()
    var navStart by rememberSaveable { mutableStateOf<String?>(null) }

    // startDestination should only decide the initial graph route on cold start.
    // If we keep reacting to pinEnabled changes here, the active back stack can be
    // invalidated mid-session (e.g. right after first-time PIN setup).
    LaunchedEffect(startDestination) {
        if (navStart == null && startDestination != null) {
            navStart = startDestination
        }
    }

    val start = navStart ?: return

    // Auto-lock: navigate ke PinLock saat shouldLock = true
    LaunchedEffect(shouldLock) {
        if (shouldLock) {
            navController.navigate(Screen.PinLock.route) {
                popUpTo(Screen.Main.route) { inclusive = false }
            }
            rootViewModel.clearLock()
        }
    }

    NavHost(navController = navController, startDestination = start) {

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── PIN Lock ──────────────────────────────────────────────────────────
        composable(Screen.PinLock.route) {
            val prefs by rootViewModel.prefs.collectAsStateWithLifecycle()
            PinLockScreen(
                mode       = PinMode.UNLOCK,
                bioEnabled = prefs?.bioEnabled ?: false,
                onSuccess  = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.PinLock.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.PinSetup.route,
            arguments = listOf(
                navArgument(Screen.PinSetup.ARG_CHANGE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
        ) { backStack ->
            val isChangePin = backStack.arguments?.getBoolean(Screen.PinSetup.ARG_CHANGE) ?: false
            PinSetupScreen(
                isChangePin = isChangePin,
                onSuccess = {
                    if (isChangePin) {
                        navController.popBackStack()
                    } else {
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }

        // ── Main shell ────────────────────────────────────────────────────────
        composable(Screen.Main.route) {
            val accountsVm: AccountsViewModel    = hiltViewModel()
            val txnVm: TransactionsViewModel     = hiltViewModel()

            val accounts     by accountsVm.accounts.collectAsStateWithLifecycle()
            // allTxns: sourced from TransactionsViewModel — AccountsViewModel no longer holds full txn list
            val rootPrefs    by rootViewModel.prefs.collectAsStateWithLifecycle()

            MainScaffold(
                accounts        = accounts,
                soundEnabled    = rootPrefs?.soundEnabled ?: true,
                onTxnSaved      = { txn -> txnVm.saveTransaction(txn) },
                onTxnUpdated    = { old, new -> txnVm.updateTransaction(old, new) },
                onTxnDeleted    = { txn -> txnVm.deleteTransaction(txn) },
                onTransferSaved = { txn -> txnVm.saveTransfer(txn) },
            ) { currentTab, onTabChange, onTxnClick, onOpenTransfer ->

                // Smooth iOS-like crossfade between tabs
                AnimatedContent(
                    targetState  = currentTab,
                    transitionSpec = {
                        fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(120, easing = FastOutSlowInEasing))
                    },
                    label = "tabContent",
                ) { tab ->
                    when (tab) {
                        NavTab.Home -> HomeScreen(
                            onTabChange    = onTabChange,
                            onTxnClick     = onTxnClick,
                            onAccountClick = { acc ->
                                navController.navigate(Screen.AccountDetail.createRoute(acc.id))
                            },
                        )

                        NavTab.Transactions -> TransactionsScreen(
                            onTxnClick = onTxnClick,
                        )

                        NavTab.Accounts -> AccountsScreen(
                            onNavigateToDetail = { accountId ->
                                navController.navigate(Screen.AccountDetail.createRoute(accountId))
                            },
                            onOpenTransfer = onOpenTransfer,
                        )

                        NavTab.Analytics -> AnalyticsScreen()

                        NavTab.Profile -> ProfileScreen(
                            onNavigateToMiniGame = {
                                navController.navigate(Screen.MiniGame.route)
                            },
                            onNavigateToPinSetup = { isChangePin ->
                                navController.navigate(Screen.PinSetup.createRoute(isChangePin))
                            },
                        )
                    }
                }
            }
        }

        // ── Account Detail ────────────────────────────────────────────────────
        composable(Screen.AccountDetail.route) { backStack ->
            val accountId = backStack.arguments?.getString(Screen.AccountDetail.ARG) ?: return@composable
            val accountsVm: AccountsViewModel = hiltViewModel()
            val txnVm: TransactionsViewModel  = hiltViewModel()
            val accounts  by accountsVm.accounts.collectAsStateWithLifecycle()
            // allTxns for account detail — sourced from TransactionsViewModel
            val accountTxns by txnVm.transactionsForAccount(accountId).collectAsStateWithLifecycle(emptyList())
            var editTarget by remember { mutableStateOf<com.dompetku.domain.model.Account?>(null) }

            val account  = accounts.find { it.id == accountId } ?: return@composable
            val accIndex = accounts.indexOf(account)
            val hidden   by accountsVm.hideBalance.collectAsStateWithLifecycle()

            AccountDetailScreen(
                account      = account,
                accIndex     = accIndex,
                transactions = accountTxns,
                accounts     = accounts,
                hidden       = hidden,
                onBack       = { navController.popBackStack() },
                onEdit       = { editTarget = it },
            )

            editTarget?.let { target ->
                AccountFormSheet(
                    initial   = target,
                    onDismiss = { editTarget = null },
                    onSave    = { acc, adj -> accountsVm.updateAccount(acc, adj); editTarget = null },
                )
            }
        }

        // ── Mini Game ─────────────────────────────────────────────────────────
        composable(Screen.MiniGame.route) {
            MiniGameScreen(onBack = { navController.popBackStack() })
        }
    }
}
