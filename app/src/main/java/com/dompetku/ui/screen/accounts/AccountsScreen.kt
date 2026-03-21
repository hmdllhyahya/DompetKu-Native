package com.dompetku.ui.screen.accounts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.R
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.ui.components.AppHeader
import com.dompetku.ui.components.WhiteCard
import com.dompetku.ui.theme.CardWhite
import com.dompetku.ui.theme.GreenDark
import com.dompetku.ui.theme.GreenLight
import com.dompetku.ui.theme.GreenPrimary
import com.dompetku.ui.theme.PageBg
import com.dompetku.ui.theme.RedExpense
import com.dompetku.ui.theme.RedLight
import com.dompetku.ui.theme.TextDark
import com.dompetku.ui.theme.TextLight
import com.dompetku.util.CurrencyFormatter
import com.dompetku.util.HapticHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    onNavigateToDetail: (accountId: String) -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val hidden by viewModel.hideBalance.collectAsStateWithLifecycle()

    var editMode by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Account?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    var showCurrency by remember { mutableStateOf(false) }
    var draftAccounts by remember { mutableStateOf(accounts) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(accounts) {
        if (draggedId == null) draftAccounts = accounts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        AppHeader(
            title = stringResource(R.string.accounts_title),
            showDate = false,
            trailingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (hidden) GreenLight else Color(0xFFF1F5F9))
                        .clickable { viewModel.toggleHideBalance() },
                ) {
                    Icon(
                        imageVector = if (hidden) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                        contentDescription = null,
                        tint = if (hidden) GreenPrimary else TextLight,
                        modifier = Modifier.size(15.dp),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 100.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .padding(bottom = 14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.accounts_total_title),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (hidden) "Rp ••••••" else CurrencyFormatter.format(totalBalance),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { showCurrency = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CurrencyCircleDollar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(R.string.exchange_rate),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_list_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChipButton(
                        label = if (editMode) stringResource(R.string.done_label) else stringResource(R.string.edit_label),
                        icon = PhosphorIcons.Regular.PencilSimple,
                        onClick = {
                            editMode = !editMode
                            if (!editMode) {
                                draggedId = null
                                dragOffset = Offset.Zero
                                draftAccounts = accounts
                            }
                        },
                    )
                    SmallChipButton(
                        label = stringResource(R.string.transfer_label),
                        icon = PhosphorIcons.Regular.ArrowsLeftRight,
                        onClick = onOpenTransfer,
                        enabled = accounts.size >= 2,
                        color = GreenPrimary,
                    )
                    SmallChipButton(
                        label = stringResource(R.string.add_label),
                        icon = PhosphorIcons.Regular.Plus,
                        onClick = { showAddSheet = true },
                        color = GreenPrimary,
                    )
                }
            }

            if (editMode && draftAccounts.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.accounts_reorder_hint),
                    fontSize = 12.sp,
                    color = TextLight,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (draftAccounts.isEmpty()) {
                WhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.no_accounts_message),
                        fontSize = 13.sp,
                        color = TextLight,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp),
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    userScrollEnabled = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(
                        items = draftAccounts,
                        key = { it.id },
                    ) { account ->
                        val isDragging = draggedId == account.id
                        AccountCard(
                            account = account,
                            accIndex = draftAccounts.indexOf(account),
                            editMode = editMode,
                            hidden = hidden,
                            isDragging = isDragging,
                            dragOffset = if (isDragging) dragOffset else Offset.Zero,
                            modifier = Modifier
                                .animateItemPlacement(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    itemBounds[account.id] = coordinates.boundsInRoot()
                                },
                            onTap = { if (!editMode) onNavigateToDetail(account.id) },
                            onLongPress = {
                                if (!editMode) {
                                    editMode = true
                                    HapticHelper.tapMedium(context)
                                }
                            },
                            onEdit = { editTarget = account },
                            onDelete = { deleteTarget = account },
                            onDragStart = {
                                if (!editMode) return@AccountCard
                                draggedId = account.id
                                dragOffset = Offset.Zero
                                HapticHelper.tapMedium(context)
                            },
                            onDrag = { delta ->
                                if (draggedId != account.id) return@AccountCard
                                dragOffset += delta

                                val currentBounds = itemBounds[account.id] ?: return@AccountCard
                                val dragCenter = currentBounds.center + dragOffset
                                val targetId = draftAccounts.firstOrNull { candidate ->
                                    candidate.id != account.id && (itemBounds[candidate.id]?.contains(dragCenter) == true)
                                }?.id ?: return@AccountCard

                                val currentIndex = draftAccounts.indexOfFirst { it.id == account.id }
                                val targetIndex = draftAccounts.indexOfFirst { it.id == targetId }
                                if (currentIndex == -1 || targetIndex == -1 || currentIndex == targetIndex) return@AccountCard

                                val targetBounds = itemBounds[targetId]
                                val reordered = draftAccounts.toMutableList().apply {
                                    add(targetIndex, removeAt(currentIndex))
                                }
                                draftAccounts = reordered

                                if (targetBounds != null) {
                                    dragOffset += currentBounds.topLeft - targetBounds.topLeft
                                }
                                HapticHelper.tapLight(context)
                            },
                            onDragEnd = {
                                if (draggedId == account.id) {
                                    draggedId = null
                                    dragOffset = Offset.Zero
                                    if (draftAccounts.map(Account::id) != accounts.map(Account::id)) {
                                        viewModel.reorderAccounts(draftAccounts)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AccountFormSheet(
            onDismiss = { showAddSheet = false },
            onSave = { account, _ -> viewModel.addAccount(account) },
        )
    }

    editTarget?.let { target ->
        AccountFormSheet(
            initial = target,
            onDismiss = { editTarget = null },
            onSave = { account, adjustment -> viewModel.updateAccount(account, adjustment) },
        )
    }

    deleteTarget?.let { target ->
        var txnCount by remember(target) { mutableIntStateOf(0) }
        LaunchedEffect(target.id) {
            txnCount = viewModel.txnCountForAccount(target.id)
        }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_account_title),
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_account_message,
                        target.name,
                        txnCount,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel_label))
                }
            },
        )
    }

    if (showCurrency) {
        CurrencySheet(totalBalance = totalBalance, onDismiss = { showCurrency = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(
    account: Account,
    accIndex: Int,
    editMode: Boolean,
    hidden: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val rotation = if (editMode && !isDragging) {
        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
        val jiggleAngle by infiniteTransition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = InfiniteRepeatableSpec(
                animation = tween(100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "jiggleAngle",
        )
        jiggleAngle + if (accIndex % 2 == 0) -0.5f else 0.5f
    } else {
        0f
    }
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "accountScale",
    )

    Column(
        modifier = modifier
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                alpha = if (isDragging) 0.98f else 1f
            }
            .shadow(elevation = if (editMode || isDragging) 8.dp else 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .pointerInput(editMode) {
                if (!editMode) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                )
            }
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(account.gradientStart.toInt()),
                            Color(account.gradientEnd.toInt()),
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(top = 0.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .align(Alignment.TopEnd),
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = account.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (account.balance < 0) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.ewallet -> PhosphorIcons.Regular.DeviceMobile
                            AccountType.emoney -> PhosphorIcons.Regular.WifiHigh
                            AccountType.cash -> PhosphorIcons.Regular.Wallet
                            AccountType.savings -> PhosphorIcons.Regular.PiggyBank
                            else -> PhosphorIcons.Regular.CreditCard
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (hidden) "••••" else CurrencyFormatter.compact(account.balance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (account.balance < 0) Color(0xFFFCA5A5) else Color.White,
                )
                Text(
                    text = if (!account.last4.isNullOrEmpty()) "•••• ${account.last4}" else account.type.name,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
                    .padding(horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.PencilSimple,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.edit_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                    )
                }
            }
            VerticalDivider(color = Color(0xFFF1F5F9))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = null,
                        tint = RedExpense,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.delete_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedExpense,
                    )
                }
            }
            if (editMode) {
                VerticalDivider(color = Color(0xFFF1F5F9))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.55f)
                        .padding(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsSixVertical,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallChipButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = TextDark,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled && color == GreenPrimary) GreenLight
                else if (enabled) Color(0xFFF1F5F9)
                else Color(0xFFE5E7EB),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) color else TextLight,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) color else TextLight,
            )
        }
    }
}
