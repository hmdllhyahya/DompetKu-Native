package com.dompetku.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dompetku.R
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Transaction
import com.dompetku.ui.navigation.FanNav
import com.dompetku.ui.navigation.NavTab
import com.dompetku.ui.theme.TextMedium
import com.dompetku.util.SoundManager
import com.dompetku.ui.screen.transactions.TransactionDetailSheet
import com.dompetku.ui.screen.transactions.TransactionFormSheet
import com.dompetku.ui.screen.transactions.TransferSheet

/**
 * MainScaffold hosts the 5 content screens + FanNav.
 * All global BottomSheets are hoisted here so FanNav can open them
 * regardless of which tab is active.
 */
@Composable
fun MainScaffold(
    initialTab: NavTab = NavTab.Home,
    accounts:   List<Account>     = emptyList(),
    soundEnabled: Boolean          = true,
    onTxnSaved: (Transaction) -> Unit = {},
    onTxnDeleted: (Transaction) -> Unit = {},
    onTxnUpdated: (old: Transaction, new: Transaction) -> Unit = { _, _ -> },
    onTransferSaved: (Transaction) -> Unit = {},
    content: @Composable (
        currentTab: NavTab,
        onTabChange: (NavTab) -> Unit,
        onTxnClick: (Transaction) -> Unit,
        onOpenTransfer: () -> Unit,
    ) -> Unit,
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // ── Global sheet state ────────────────────────────────────────────────────
    var showTxnForm      by remember { mutableStateOf(false) }
    var showTransfer     by remember { mutableStateOf(false) }
    var editingTxn       by remember { mutableStateOf<Transaction?>(null) }
    var detailTxn        by remember { mutableStateOf<Transaction?>(null) }

    BackHandler(enabled = detailTxn != null) {
        detailTxn = null
    }

    BackHandler(enabled = editingTxn != null || showTxnForm) {
        editingTxn = null
        showTxnForm = false
    }

    BackHandler(enabled = showTransfer) {
        showTransfer = false
    }

    BackHandler(enabled = !showExitConfirm) {
        if (currentTab != NavTab.Home) {
            currentTab = NavTab.Home
        } else {
            showExitConfirm = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Content ────────────────────────────────────────────────────────────
        content(
            currentTab,
            { tab -> currentTab = tab },
            { txn -> detailTxn = txn },
            { showTransfer = true },
        )

        // ── FanNav ─────────────────────────────────────────────────────────────
        FanNav(
            currentTab  = currentTab,
            onTabChange = { currentTab = it },
            onQuickAdd  = { showTxnForm = true },
            onTransfer  = { showTransfer = true },
        )
    }

    // ── Transaction form sheet ────────────────────────────────────────────────
    if (showTxnForm || editingTxn != null) {
        TransactionFormSheet(
            initial   = editingTxn,
            accounts  = accounts,
            onDismiss = { showTxnForm = false; editingTxn = null },
            onSave    = { txn ->
                val old = editingTxn
                when {
                    old != null -> {
                        onTxnUpdated(old, txn)
                        SoundManager.playSuccess(soundEnabled)
                    }
                    txn.type == com.dompetku.domain.model.TransactionType.transfer -> {
                        onTransferSaved(txn)
                        SoundManager.playTransfer(soundEnabled)
                    }
                    else -> {
                        onTxnSaved(txn)
                        SoundManager.playSuccess(soundEnabled)
                    }
                }
                showTxnForm = false; editingTxn = null
            },
        )
    }

    // ── Transaction detail sheet ──────────────────────────────────────────────
    detailTxn?.let { txn ->
        TransactionDetailSheet(
            txn       = txn,
            accounts  = accounts,
            onDismiss = { detailTxn = null },
            onEdit    = { t -> editingTxn = t; detailTxn = null },
            onDelete  = { t -> onTxnDeleted(t); SoundManager.playDelete(soundEnabled); detailTxn = null },
        )
    }

    // ── Transfer sheet ────────────────────────────────────────────────────────
    if (showTransfer) {
        TransferSheet(
            accounts  = accounts,
            onDismiss = { showTransfer = false },
            onSave    = { txn -> onTransferSaved(txn); SoundManager.playTransfer(soundEnabled); showTransfer = false },
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.exit_app_title)) },
            text = { Text(stringResource(R.string.exit_app_message)) },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.exit_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.cancel_label), color = TextMedium)
                }
            },
        )
    }
}
