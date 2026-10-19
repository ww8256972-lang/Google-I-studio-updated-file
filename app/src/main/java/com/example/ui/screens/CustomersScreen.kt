package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.WhatsAppManager
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SaleEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var newlyCreatedCustomerForWhatsApp by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { c ->
            searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery) ||
                    c.address.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalUdhar = remember(customers) {
        customers.sumOf { it.outstandingBalance }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Total Udhar Overview with 3D gradient card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Customer Khata Ledger",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${customers.size} Registered Customers",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Udhar Outstanding",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", totalUdhar)}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalUdhar > 0) StatusRed else StatusGreen
                                )
                            }
                        }
                    }
                }
            }

            // Search Box
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customer name, phone, address...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_customers"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Empty State
            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (customers.isEmpty()) "No customers added yet" else "No matching customers found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add customers with photos, phone & WhatsApp numbers to track their purchases and send instant payment reminders.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddCustomerDialog = true },
                                modifier = Modifier.testTag("btn_empty_add_customer")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Customer")
                            }
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val hasUdhar = customer.outstandingBalance > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerForLedger = customer },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Customer Photo or Avatar
                            if (customer.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = customer.imageUri,
                                    contentDescription = customer.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasUdhar) StatusRedContainer else StatusGreenContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = customer.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (hasUdhar) StatusRed else StatusGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Customer Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = customer.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (customer.points > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = GoldContainer
                                        ) {
                                            Text(
                                                "⭐ ${customer.points} pts",
                                                color = OnGoldContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        text = "📞 ${customer.phone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (customer.address.isNotBlank()) {
                                    Text(
                                        text = "📍 ${customer.address}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Balance & Direct Actions (WhatsApp, Receive, Ledger)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (hasUdhar) "Due: ₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}" else "All Settled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasUdhar) StatusRed else StatusGreen
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Direct WhatsApp Icon Button
                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val msg = if (hasUdhar) {
                                                    WhatsAppManager.formatPaymentReminderMessage(shopProfile, customer, customer.outstandingBalance)
                                                } else {
                                                    "Hello ${customer.name}, greetings from ${shopProfile.name}! Thank you for being our valued customer."
                                                }
                                                WhatsAppManager.launchWhatsAppIntent(context, customer.phone, msg)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = "WhatsApp",
                                                tint = Color(0xFF25D366),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (hasUdhar) {
                                        FilledTonalButton(
                                            onClick = { showRecordPaymentDialog = customer },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Receive", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    IconButton(
                                        onClick = { selectedCustomerForLedger = customer },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "View Ledger")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Customer Dialog with Photo and Direct WhatsApp Integration
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var pointsText by remember { mutableStateOf("0") }
        var imageUri by remember { mutableStateOf("") }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { imageUri = it.toString() }
        }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Customer Account", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Customer Photo Picker
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Customer Photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (imageUri.isBlank()) "Add Customer Photo" else "Change Photo", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer Full Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer_name")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("WhatsApp / Mobile Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address / Village / Town") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pointsText,
                                onValueChange = { pointsText = it },
                                label = { Text("Loyalty Points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Remarks / Credit Limit") },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Customer name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val pts = pointsText.toIntOrNull() ?: 0
                        viewModel.addCustomer(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            points = pts,
                            imageUri = imageUri
                        ) { created ->
                            showAddCustomerDialog = false
                            newlyCreatedCustomerForWhatsApp = created
                            Toast.makeText(context, "Customer ${created.name} registered!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Post-Save WhatsApp Welcome Prompt
    newlyCreatedCustomerForWhatsApp?.let { newCust ->
        AlertDialog(
            onDismissRequest = { newlyCreatedCustomerForWhatsApp = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customer Saved!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Would you like to send a welcome and digital khata confirmation message to ${newCust.name} on WhatsApp?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val welcomeMsg = """
                            *${shopProfile.name}*
                            📍 ${shopProfile.address}
                            📞 ${shopProfile.phone}
                            ───────────────────────
                            Hello *${newCust.name}*!
                            Your khata account has been successfully opened at our shop.
                            
                            Thank you for your business!
                            — *${shopProfile.name}*
                        """.trimIndent()
                        WhatsAppManager.launchWhatsAppIntent(context, newCust.phone, welcomeMsg)
                        newlyCreatedCustomerForWhatsApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send on WhatsApp", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { newlyCreatedCustomerForWhatsApp = null }) {
                    Text("Skip")
                }
            }
        )
    }

    // Modal: Record Balance Payment Dialog (Udhar Vasooli) with instant receipt sharing
    showRecordPaymentDialog?.let { customer ->
        var payAmount by remember { mutableStateOf(String.format(Locale.US, "%.2f", customer.outstandingBalance)) }
        var payMethod by remember { mutableStateOf("Cash") }
        var payNotes by remember { mutableStateOf("") }
        var isSavingPay by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRecordPaymentDialog = null },
            title = { Text("Receive Payment: ${customer.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Current Outstanding Due: ₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                            modifier = Modifier.padding(10.dp),
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Payment Received Amount *") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_receive_payment_amount")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Bank").forEach { method ->
                            FilterChip(
                                selected = payMethod == method,
                                onClick = { payMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Payment Notes / Reference") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Enter a valid payment amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSavingPay = true
                        viewModel.recordBalancePayment(
                            customerId = customer.id,
                            customerName = customer.name,
                            amount = amount,
                            paymentMethod = payMethod,
                            notes = payNotes
                        ) { payment ->
                            isSavingPay = false
                            showRecordPaymentDialog = null
                            val updatedBalance = (customer.outstandingBalance - amount).coerceAtLeast(0.0)

                            // Launch WhatsApp payment acknowledgment
                            if (customer.phone.isNotBlank()) {
                                val receiptMsg = """
                                    🧾 *${shopProfile.name} — Payment Received*
                                    Receipt No: *${payment.receiptNumber}*
                                    Customer: *${customer.name}*
                                    Amount Received: *₹${String.format(Locale.US, "%.2f", amount)}* via $payMethod
                                    Remaining Balance: *₹${String.format(Locale.US, "%.2f", updatedBalance)}*
                                    ───────────────────────
                                    Thank you!
                                """.trimIndent()
                                WhatsAppManager.launchWhatsAppIntent(context, customer.phone, receiptMsg)
                            }

                            try {
                                val pdf = PdfGenerator.generatePaymentReceipt(context, payment, updatedBalance, shopProfile)
                                PdfGenerator.sharePdf(context, pdf, "Payment Receipt ${payment.receiptNumber}")
                            } catch (_: Exception) {}
                        }
                    },
                    enabled = !isSavingPay
                ) {
                    Text("Save & Send Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Detailed Customer Ledger Sheet
    selectedCustomerForLedger?.let { customer ->
        val customerSales = remember(sales, customer.id) {
            sales.filter { it.customerId == customer.id }
        }
        val customerPayments = remember(payments, customer.id) {
            payments.filter { it.customerId == customer.id }
        }

        AlertDialog(
            onDismissRequest = { selectedCustomerForLedger = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (customer.phone.isNotBlank()) {
                            Text(customer.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (customer.outstandingBalance > 0) {
                        Text(
                            "₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp reminder action item
                    if (customer.outstandingBalance > 0 && customer.phone.isNotBlank()) {
                        item {
                            Button(
                                onClick = {
                                    val msg = WhatsAppManager.formatPaymentReminderMessage(shopProfile, customer, customer.outstandingBalance)
                                    WhatsAppManager.launchWhatsAppIntent(context, customer.phone, msg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Payment Reminder on WhatsApp", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Text("Ledger History", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (customerSales.isEmpty() && customerPayments.isEmpty()) {
                        item {
                            Text(
                                "No transactions recorded for this customer yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        val allEvents = (customerSales.map { "SALE" to it } + customerPayments.map { "PAYMENT" to it })
                            .sortedByDescending {
                                if (it.first == "SALE") (it.second as SaleEntity).timestamp
                                else (it.second as PaymentEntity).timestamp
                            }

                        items(allEvents) { (type, event) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (type == "SALE") {
                                        val sale = event as SaleEntity
                                        Column {
                                            Text("Invoice: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${String.format(Locale.US, "%.2f", sale.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (sale.outstandingBalance > 0) {
                                                Text("Due: ₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}", fontSize = 10.sp, color = StatusRed)
                                            }
                                        }
                                    } else {
                                        val pay = event as PaymentEntity
                                        Column {
                                            Text("Receipt: ${pay.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusGreen)
                                            Text(
                                                "${pay.paymentMethod} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(pay.timestamp))}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Text("+ ₹${String.format(Locale.US, "%.2f", pay.amount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (customer.outstandingBalance > 0) {
                    Button(
                        onClick = {
                            val target = customer
                            selectedCustomerForLedger = null
                            showRecordPaymentDialog = target
                        }
                    ) {
                        Text("Record Payment")
                    }
                } else {
                    TextButton(onClick = { selectedCustomerForLedger = null }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomerForLedger = null }) {
                    Text("Close")
                }
            }
        )
    }
}
