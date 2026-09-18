package com.example.ui.expenses

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ExpenseCategories
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTypes
import com.example.ui.components.ChaatDatePickerDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.FullScreenImageViewerDialog
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.theme.CreditAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.util.FormatUtils
import com.example.util.ImageStorageHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpensesViewModel,
    expenseId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val isEditMode = expenseId != null && expenseId > 0
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showPaidDatePicker by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var showFullScreenBill by remember { mutableStateOf(false) }

    val photoSheetState = rememberModalBottomSheetState()
    val commonVendors = listOf("Sabzi Mandi", "Gupta Kirana", "Amul Dairy", "Gas Agency", "Wholesale Sev Depot")

    // Activity result launcher for Gallery (Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            if (savedUri != null) {
                viewModel.updateBillImageUri(savedUri)
            }
        }
        showPhotoPickerSheet = false
    }

    // Activity result launcher for Camera Preview
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedUri = ImageStorageHelper.saveBitmapToInternalStorage(context, bitmap)
            if (savedUri != null) {
                viewModel.updateBillImageUri(savedUri)
            }
        }
        showPhotoPickerSheet = false
    }

    LaunchedEffect(expenseId) {
        if (isEditMode) {
            viewModel.loadExpenseForEdit(expenseId!!)
        } else {
            viewModel.initNewExpense()
        }
    }

    if (showDeleteConfirm && expenseId != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "Are you sure you want to delete this expense of ${FormatUtils.formatCurrency(formState.amount.toDoubleOrNull() ?: 0.0)} from ${formState.shopName}?",
            onConfirm = {
                viewModel.deleteExpenseById(expenseId)
                onNavigateBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showPurchaseDatePicker) {
        ChaatDatePickerDialog(
            initialDateMillis = formState.purchaseDate,
            onDateSelected = { selectedDate ->
                viewModel.updatePurchaseDate(selectedDate)
            },
            onDismiss = { showPurchaseDatePicker = false }
        )
    }

    if (showPaidDatePicker) {
        ChaatDatePickerDialog(
            initialDateMillis = formState.paidDate ?: System.currentTimeMillis(),
            onDateSelected = { selectedDate ->
                viewModel.updatePaidDate(selectedDate)
            },
            onDismiss = { showPaidDatePicker = false }
        )
    }

    if (showFullScreenBill && formState.billImageUri != null) {
        FullScreenImageViewerDialog(
            imageUri = formState.billImageUri!!,
            title = "Bill: ${formState.shopName.ifBlank { "Vendor Receipt" }}",
            onDismiss = { showFullScreenBill = false }
        )
    }

    // Photo Source Selection Bottom Sheet
    if (showPhotoPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoPickerSheet = false },
            sheetState = photoSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Attach Bill Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Capture a receipt or choose an existing bill photo from gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    onClick = {
                        cameraLauncher.launch(null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_camera_option"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Take Photo with Camera", fontWeight = FontWeight.Bold)
                            Text("Capture receipt using device camera", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                ElevatedCard(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_gallery_option"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column {
                            Text("Choose from Gallery", fontWeight = FontWeight.Bold)
                            Text("Select an invoice or screenshot from device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Expense" else "New Shop Expense",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("expense_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_expense_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Expense",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vendor Suggestions
            if (!isEditMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Frequent Suppliers / Vendors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commonVendors.forEach { vendor ->
                            FilterChip(
                                selected = formState.shopName == vendor,
                                onClick = { viewModel.updateShopName(vendor) },
                                label = { Text(vendor, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }

            // Shop Name (Vendor/Supplier) Input
            OutlinedTextField(
                value = formState.shopName,
                onValueChange = { viewModel.updateShopName(it) },
                label = { Text("Shop / Vendor Name *") },
                placeholder = { Text("e.g. Sabzi Mandi, Shree Ram Flour Mill") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_shop_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = formState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("expense_category_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    ExpenseCategories.ALL.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.updateCategory(cat)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount (₹) *") },
                placeholder = { Text("e.g. 850") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Description Input
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                placeholder = { Text("e.g. 20kg Potatoes, Coriander, Green Chillies") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_description_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Payment Type Toggle (Cash vs Credit)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Type (Cash vs Credit Dues)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_payment_type_selector")
                ) {
                    PaymentTypes.ALL.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = formState.paymentType == type,
                            onClick = { viewModel.updatePaymentType(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = PaymentTypes.ALL.size)
                        ) {
                            Text(type, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Visual Distinction for Pending Credit Expenses vs Paid
            if (formState.paymentType == PaymentTypes.CREDIT) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_credit_alert_card"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7) // Distinct warm amber banner
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CreditAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "PENDING CREDIT DUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CreditAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            PaymentStatusBadge(status = formState.paymentStatus)
                        }

                        Text(
                            text = if (formState.paymentStatus == PaymentStatus.PENDING) {
                                "This credit expense is marked as Pending. It will be recorded under outstanding vendor dues until settled."
                            } else {
                                "Settled on ${FormatUtils.formatDate(formState.paidDate ?: formState.purchaseDate)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )

                        // Allow toggling status manually if needed (Paid / Pending)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = formState.paymentStatus == PaymentStatus.PENDING,
                                onClick = { viewModel.updatePaymentStatus(PaymentStatus.PENDING) },
                                label = { Text("Pending Due", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("chip_status_pending")
                            )
                            FilterChip(
                                selected = formState.paymentStatus == PaymentStatus.PAID,
                                onClick = { viewModel.updatePaymentStatus(PaymentStatus.PAID) },
                                label = { Text("Marked as Paid", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("chip_status_paid")
                            )
                        }

                        if (formState.paymentStatus == PaymentStatus.PAID) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPaidDatePicker = true }
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Settled Date: ${FormatUtils.formatDate(formState.paidDate ?: System.currentTimeMillis())}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                TextButton(onClick = { showPaidDatePicker = true }) {
                                    Text("Change Date", color = CreditAmber, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Cash Expense Confirmation
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7).copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Payment Status: Paid instantly with Cash",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            // Purchase Date Picker
            OutlinedTextField(
                value = FormatUtils.formatDate(formState.purchaseDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Purchase Date (can be backdated)") },
                trailingIcon = {
                    IconButton(onClick = { showPurchaseDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPurchaseDatePicker = true }
                    .testTag("expense_date_picker_field"),
                shape = RoundedCornerShape(12.dp)
            )

            // Bill Photo Attachment Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bill / Invoice Photo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (formState.billImageUri == null) {
                    OutlinedCard(
                        onClick = { showPhotoPickerSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attach_bill_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Attach Bill Photo (Camera / Gallery)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    // Attached bill photo preview
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attached_bill_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clickable thumbnail to view full screen
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showFullScreenBill = true }
                                    .testTag("bill_photo_thumbnail")
                            ) {
                                AsyncImage(
                                    model = formState.billImageUri,
                                    contentDescription = "Bill Photo",
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
                                        contentDescription = "View Full Screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bill Attached",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Saved in app storage • Tap photo to expand",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showPhotoPickerSheet = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("change_bill_photo_button")
                                    ) {
                                        Text("Replace", style = MaterialTheme.typography.labelSmall)
                                    }

                                    TextButton(
                                        onClick = { viewModel.updateBillImageUri(null) },
                                        modifier = Modifier.testTag("remove_bill_photo_button")
                                    ) {
                                        Text("Remove", color = ExpenseRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Entered By Field
            OutlinedTextField(
                value = formState.enteredBy,
                onValueChange = { viewModel.updateEnteredBy(it) },
                label = { Text("Entered By") },
                placeholder = { Text("Owner / Manager") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_entered_by_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Error message if any
            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage ?: "",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveExpense {
                        onNavigateBack()
                    }
                },
                enabled = formState.isValid && !formState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEditMode) "Update Expense" else "Save Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
