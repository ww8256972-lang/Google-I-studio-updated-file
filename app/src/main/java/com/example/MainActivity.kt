package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        val repo = (application as? AslamRecordApp)?.repository
            ?: com.example.data.repository.ShopRepository(
                com.example.data.db.AppDatabase.getDatabase(this)
            )
        ShopViewModel.Factory(repo)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Dark Mode and White (Light) Mode State
            var isDarkMode by remember { mutableStateOf(false) }

            // Welcome Animation State
            var showWelcomeAnimation by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                var aiInitialPrompt by remember { mutableStateOf<String?>(null) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Auto-dismiss welcome animation after 2.2 seconds or let user tap to dismiss
                LaunchedEffect(Unit) {
                    delay(2200)
                    showWelcomeAnimation = false
                }

                // Hardware back button handles drawer or returns to Dashboard
                BackHandler(enabled = drawerState.isOpen || currentScreen != Screen.Dashboard) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else {
                        currentScreen = Screen.Dashboard
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(310.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface
                        ) {
                            // Sidebar Drawer Header with 3D gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(50.dp),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Storefront,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "ASLAM RECORD",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Smart Shop & Bahi-Khata",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Sidebar Navigation Options
                            NavigationDrawerItem(
                                label = { Text("Dashboard (Overview)", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                selected = currentScreen == Screen.Dashboard,
                                onClick = {
                                    currentScreen = Screen.Dashboard
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_dashboard")
                            )

                            NavigationDrawerItem(
                                label = { Text("Record New Sale", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.PointOfSale, contentDescription = null, tint = EmeraldPrimary) },
                                selected = currentScreen == Screen.RecordSale,
                                onClick = {
                                    currentScreen = Screen.RecordSale
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_new_sale")
                            )

                            NavigationDrawerItem(
                                label = { Text("Products & Inventory", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                selected = currentScreen == Screen.Products,
                                onClick = {
                                    currentScreen = Screen.Products
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_products")
                            )

                            NavigationDrawerItem(
                                label = { Text("Customer Khata Ledger", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.People, contentDescription = null) },
                                selected = currentScreen == Screen.Customers,
                                onClick = {
                                    currentScreen = Screen.Customers
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_customers")
                            )

                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ASLAM AI Munshi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GoldContainer
                                        ) {
                                            Text(
                                                "GEMINI",
                                                color = GoldAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent) },
                                selected = currentScreen == Screen.AslamAi,
                                onClick = {
                                    aiInitialPrompt = null
                                    currentScreen = Screen.AslamAi
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_aslam_ai")
                            )

                            NavigationDrawerItem(
                                label = { Text("Financial Reports", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                                selected = currentScreen == Screen.Reports,
                                onClick = {
                                    currentScreen = Screen.Reports
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_reports")
                            )

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            // Sidebar Theme Switcher (Dark Mode / White Mode)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isDarkMode) "Dark Mode" else "White Mode",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { isDarkMode = it },
                                    modifier = Modifier.testTag("switch_dark_mode")
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("btn_sidebar_drawer_menu")
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Side Bar Menu"
                                        )
                                    }
                                },
                                title = {
                                    Column {
                                        Text(
                                            text = "ASLAM RECORD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = currentScreen.title,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                actions = {
                                    // Quick theme toggle in top bar
                                    IconButton(
                                        onClick = { isDarkMode = !isDarkMode },
                                        modifier = Modifier.testTag("top_bar_theme_toggle")
                                    ) {
                                        Icon(
                                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme"
                                        )
                                    }

                                    // Direct Aslam AI launcher
                                    IconButton(
                                        onClick = {
                                            aiInitialPrompt = null
                                            currentScreen = Screen.AslamAi
                                        },
                                        modifier = Modifier.testTag("top_app_bar_ai_chat")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "ASLAM AI Munshi",
                                            tint = GoldAccent
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(screen.icon, contentDescription = screen.title)
                                        },
                                        label = {
                                            Text(screen.title, fontSize = 11.sp)
                                        },
                                        modifier = Modifier.testTag("nav_item_${screen.route}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToRecordSale = { currentScreen = Screen.RecordSale },
                                    onNavigateToProducts = { currentScreen = Screen.Products },
                                    onNavigateToCustomers = { currentScreen = Screen.Customers },
                                    onNavigateToReports = { currentScreen = Screen.Reports },
                                    onNavigateToAi = { prompt ->
                                        aiInitialPrompt = prompt
                                        currentScreen = Screen.AslamAi
                                    }
                                )
                                Screen.RecordSale -> RecordSaleScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.Dashboard }
                                )
                                Screen.Products -> ProductsScreen(
                                    viewModel = viewModel
                                )
                                Screen.Customers -> CustomersScreen(
                                    viewModel = viewModel
                                )
                                Screen.Reports -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                Screen.AslamAi -> AslamAiScreen(
                                    viewModel = viewModel,
                                    initialPrompt = aiInitialPrompt
                                )
                            }
                        }
                    }
                }

                // Modern 3D Welcome Animation Overlay on Open
                AnimatedVisibility(
                    visible = showWelcomeAnimation,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.1f, animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { showWelcomeAnimation = false },
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.04f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Card(
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .scale(scale)
                                .padding(24.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(72.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Welcome to",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "ASLAM RECORD",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Smart Digital Bahi-Khata & AI Munshi",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { showWelcomeAnimation = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Shop Ledger", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
