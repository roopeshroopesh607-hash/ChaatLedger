package com.example.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTypes
import com.example.ui.components.ChaatDatePickerDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.FullScreenImageViewerDialog
import com.example.ui.components.PaymentBadge
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.theme.CreditAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHistoryScreen(
    viewModel: ExpensesViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedExpenseForAction by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToMarkAsPaid by remember { mutableStateOf<Expense?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenImageTitle by remember { mutableStateOf("Bill Photo") }

    val sheetState = rememberModalBottomSheetState()

    // Full screen bill image viewer
    if (fullScreenImageUri != null) {
        FullScreenImageViewerDialog(
            imageUri = fullScreenImageUri!!,
            title = fullScreenImageTitle,
            onDismiss = { fullScreenImageUri = null }
        )
    }

    // Date picker for Mark as Paid
    if (expenseToMarkAsPaid != null) {
        ChaatDatePickerDialog(
            initialDateMillis = System.currentTimeMillis(),
            onDateSelected = { selectedDate ->
                viewModel.markExpenseAsPaid(expenseToMarkAsPaid!!.id, selectedDate)
                expenseToMarkAsPaid = null
            },
            onDismiss = { expenseToMarkAsPaid = null }
        )
    }

    // Delete confirmation
    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense Record",
            message = "Delete expense of ${FormatUtils.formatCurrency(expenseToDelete!!.amount)} from ${expenseToDelete!!.shopName} permanently?",
            onConfirm = {
                viewModel.deleteExpense(expenseToDelete!!)
                expenseToDelete = null
                selectedExpenseForAction = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    // Expense Details Bottom Sheet
    if (selectedExpenseForAction != null) {
        val currentExpense = selectedExpenseForAction!!
        val isPending = currentExpense.paymentStatus.equals(PaymentStatus.PENDING, ignoreCase = true)

        ModalBottomSheet(
            onDismissRequest = { selectedExpenseForAction = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentExpense.shopName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaymentBadge(badgeText = currentExpense.paymentType)
                            PaymentStatusBadge(status = currentExpense.paymentStatus)
                        }
                    }

                    Text(
                        text = FormatUtils.formatCurrency(currentExpense.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                if (currentExpense.description.isNotBlank()) {
                    Text(
                        text = "Description: ${currentExpense.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Purchase Date: ${FormatUtils.formatDate(currentExpense.purchaseDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentExpense.paidDate != null) {
                        Text(
                            text = "Settled: ${FormatUtils.formatDate(currentExpense.paidDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfitGreen
                        )
                    }
                }

                // Bill photo preview in sheet if attached
                if (currentExpense.billImageUri != null) {
                    ElevatedCard(
                        onClick = {
                            fullScreenImageUri = currentExpense.billImageUri
                            fullScreenImageTitle = "Bill: ${currentExpense.shopName}"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_view_bill_card"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = currentExpense.billImageUri,
                                contentDescription = "Attached Bill",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Attached Bill Photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Tap to view full resolution image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Icon(Icons.Default.ZoomIn, contentDescription = "Expand", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                // Action: "Mark as Paid" if pending
                if (isPending) {
                    OutlinedButton(
                        onClick = {
                            val exp = currentExpense
                            selectedExpenseForAction = null
                            expenseToMarkAsPaid = exp
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sheet_mark_as_paid_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ProfitGreen
                        ),
                        border = BorderStroke(1.5.dp, ProfitGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Settled / Paid", fontWeight = FontWeight.Bold)
                    }
                }

                // Edit & Delete actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        onClick = {
                            val id = currentExpense.id
                            selectedExpenseForAction = null
                            onNavigateToEditExpense(id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_edit_expense"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ElevatedCard(
                        onClick = {
                            expenseToDelete = currentExpense
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_delete_expense"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Expense History",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("expenses_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Expense")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips (Today, This Week, This Month, All Time)
            FilterChipsRow(
                selectedFilter = uiState.filter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // Expense Summary Banner with Cash vs Outstanding Dues breakdown
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("expense_history_summary_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.filter.label.uppercase()} EXPENSES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${uiState.totalCount} entries logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = FormatUtils.formatCurrency(uiState.totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("expense_history_total_amount")
                        )
                    }

                    // Cash vs Pending Credit breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Paid in Cash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(uiState.cashAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.pendingCreditAmount > 0) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Unpaid Dues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.pendingCreditAmount > 0) CreditAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (uiState.pendingCreditAmount > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (uiState.pendingCount > 0) {
                                        Text(
                                            text = "(${uiState.pendingCount})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CreditAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(uiState.pendingCreditAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.pendingCreditAmount > 0) CreditAmber else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Expenses List
            if (uiState.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Expenses for ${uiState.filter.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' to record raw materials, rent, or supplies",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onClick = { selectedExpenseForAction = expense },
                            onMarkAsPaid = { expenseToMarkAsPaid = expense },
                            onViewBill = { uri ->
                                fullScreenImageUri = uri
                                fullScreenImageTitle = "Bill: ${expense.shopName}"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onViewBill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = expense.paymentStatus.equals(PaymentStatus.PENDING, ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) Color(0xFFFFFDF5) else MaterialTheme.colorScheme.surface
        ),
        border = if (isPending) BorderStroke(1.dp, CreditAmber.copy(alpha = 0.6f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or Category Icon
                if (expense.billImageUri != null) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onViewBill(expense.billImageUri) }
                            .testTag("bill_thumbnail_${expense.id}")
                    ) {
                        AsyncImage(
                            model = expense.billImageUri,
                            contentDescription = "Bill Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "View Bill",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPending) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPending) Icons.Default.Schedule else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isPending) CreditAmber else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Shop Name & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.shopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${expense.category}${if (expense.description.isNotBlank()) " • ${expense.description}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentBadge(badgeText = expense.paymentType)
                        PaymentStatusBadge(status = expense.paymentStatus)
                        Text(
                            text = "• ${FormatUtils.formatDate(expense.purchaseDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${FormatUtils.formatCurrency(expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    if (expense.enteredBy.isNotBlank()) {
                        Text(
                            text = "by ${expense.enteredBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // If Pending, show prominent "Mark as Paid" action row right on the card
            if (isPending) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7).copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CreditAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Unpaid Due",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CreditAmber
                            )
                        }

                        FilledTonalButton(
                            onClick = onMarkAsPaid,
                            modifier = Modifier.testTag("mark_as_paid_btn_${expense.id}"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark as Paid",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (expense.paymentType == PaymentTypes.CREDIT && expense.paidDate != null) {
                // If settled credit expense, show settlement info
                Text(
                    text = "Settled on ${FormatUtils.formatDate(expense.paidDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProfitGreen,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}
