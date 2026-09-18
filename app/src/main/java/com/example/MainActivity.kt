package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AppViewModelProvider
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.expenses.AddExpenseScreen
import com.example.ui.expenses.ExpenseHistoryScreen
import com.example.ui.expenses.ExpensesViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportsViewModel
import com.example.ui.sales.AddSaleScreen
import com.example.ui.sales.SalesHistoryScreen
import com.example.ui.sales.SalesViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChaatLedgerApp()
            }
        }
    }
}

@Composable
fun ChaatLedgerApp() {
    val context = LocalContext.current
    var isSilentAuthChecking by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val app = context.applicationContext as? ChaatLedgerApplication
        val authManager = app?.container?.authManager
        if (authManager != null) {
            // Silently authenticate anonymously before loading Dashboard.
            // Timeout protects offline startup so users are never blocked.
            withTimeoutOrNull(2000L) {
                authManager.silentSignIn()
            }
        }
        isSilentAuthChecking = false
    }

    if (isSilentAuthChecking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ChaatLedger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Daily Aggregate Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute in listOf("dashboard", "sales", "expenses", "reports")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("bottom_nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentRoute == "sales",
                        onClick = {
                            navController.navigate("sales") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Sales") },
                        label = { Text("Sales") },
                        modifier = Modifier.testTag("bottom_nav_sales")
                    )

                    NavigationBarItem(
                        selected = currentRoute == "expenses",
                        onClick = {
                            navController.navigate("expenses") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Expenses") },
                        label = { Text("Expenses") },
                        modifier = Modifier.testTag("bottom_nav_expenses")
                    )

                    NavigationBarItem(
                        selected = currentRoute == "reports",
                        onClick = {
                            navController.navigate("reports") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        modifier = Modifier.testTag("bottom_nav_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                val viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddSale = { navController.navigate("add_sale") },
                    onNavigateToAddExpense = { navController.navigate("add_expense") },
                    onNavigateToEditSale = { saleId -> navController.navigate("edit_sale/$saleId") },
                    onNavigateToEditExpense = { expenseId -> navController.navigate("edit_expense/$expenseId") },
                    onNavigateToSalesHistory = { navController.navigate("sales") },
                    onNavigateToExpenseHistory = { navController.navigate("expenses") }
                )
            }

            composable("sales") {
                val viewModel: SalesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                SalesHistoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddSale = { navController.navigate("add_sale") },
                    onNavigateToEditSale = { saleId -> navController.navigate("edit_sale/$saleId") }
                )
            }

            composable("expenses") {
                val viewModel: ExpensesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ExpenseHistoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = { navController.navigate("add_expense") },
                    onNavigateToEditExpense = { expenseId -> navController.navigate("edit_expense/$expenseId") }
                )
            }

            composable("reports") {
                val viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ReportsScreen(viewModel = viewModel)
            }

            composable("add_sale") {
                val viewModel: SalesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AddSaleScreen(
                    viewModel = viewModel,
                    saleId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_sale/{saleId}",
                arguments = listOf(navArgument("saleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val saleId = backStackEntry.arguments?.getLong("saleId") ?: 0L
                val viewModel: SalesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AddSaleScreen(
                    viewModel = viewModel,
                    saleId = saleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("add_expense") {
                val viewModel: ExpensesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AddExpenseScreen(
                    viewModel = viewModel,
                    expenseId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_expense/{expenseId}",
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                val viewModel: ExpensesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AddExpenseScreen(
                    viewModel = viewModel,
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
