package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------
// MAIN COMPOSABLE COUPLING WORKFLOW
// ---------------------------------------------------------------------

@Composable
fun SmartLandlordApp(viewModel: SmartLandlordViewModel) {
    val context = LocalContext.current
    val currentRoute by viewModel.currentRoute.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val dbOperationMessage by viewModel.dbOperationMessage.collectAsStateWithLifecycle()

    // Listen for DB operations and show simple Toast
    LaunchedEffect(dbOperationMessage) {
        dbOperationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentUser != null && currentRoute != "login" && currentRoute != "register") {
                SmartLandlordBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { viewModel.navigateTo(it) },
                    isDeveloper = currentUser?.email?.lowercase() == "mghulam2006@gmail.com"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                "login" -> LoginScreen(viewModel)
                "register" -> RegisterScreen(viewModel)
                "dashboard" -> DashboardScreen(viewModel)
                "properties" -> PropertiesScreen(viewModel)
                "tenants" -> TenantsScreen(viewModel)
                "tenant_profile" -> TenantProfileScreen(viewModel)
                "payments" -> PaymentsScreen(viewModel)
                "maintenance" -> MaintenanceScreen(viewModel)
                "reports" -> ReportsScreen(viewModel)
                "admin" -> DeveloperAdminScreen(viewModel)
                "subscriptions" -> SubscriptionsScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------
// BOTTOM NAVIGATION
// ---------------------------------------------------------------------
@Composable
fun SmartLandlordBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDeveloper: Boolean
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("app_bottom_nav_bar")
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Properties") },
            label = { Text("Properties", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
            selected = currentRoute == "properties",
            onClick = { onNavigate("properties") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Tenants") },
            label = { Text("Tenants", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
            selected = currentRoute == "tenants" || currentRoute == "tenant_profile",
            onClick = { onNavigate("tenants") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Payment, contentDescription = "Rent") },
            label = { Text("Rent", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
            selected = currentRoute == "payments",
            onClick = { onNavigate("payments") }
        )
        
        // Show More option that opens dynamic secondary menus or shows reports
        NavigationBarItem(
            icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
            label = { Text("Reports", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
            selected = currentRoute == "reports",
            onClick = { onNavigate("reports") }
        )

        if (isDeveloper) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = SaGold) },
                label = { Text("Admin", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, color = SaGold) },
                selected = currentRoute == "admin",
                onClick = { onNavigate("admin") }
            )
        } else {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Star, contentDescription = "Upgrade") },
                label = { Text("SaaS Plans", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                selected = currentRoute == "subscriptions",
                onClick = { onNavigate("subscriptions") }
            )
        }
    }
}

// ---------------------------------------------------------------------
// LOGIN SCREEN
// ---------------------------------------------------------------------
@Composable
fun LoginScreen(viewModel: SmartLandlordViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val resetMessage by viewModel.resetMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showGoogleDialog by remember { mutableStateOf(false) }
    var showFacebookDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SaForestGreen, DarkSlate, DarkSlate),
                    startY = 0f,
                    endY = 1200f
                )
            )
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            // App branding
            Image(
                painter = painterResource(id = com.example.R.drawable.ic_smart_landlord_logo),
                contentDescription = "Smart Landlord Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SMART LANDLORD",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Manage Properties. Track Rent. Grow Smarter.",
                color = SaMint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Landlord Portal",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Error text
                    if (loginError != null) {
                        Surface(
                            color = SaRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            border = BorderStroke(1.dp, SaRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = loginError ?: "",
                                color = SaRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (resetMessage != null) {
                        Surface(
                            color = SaLightGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            border = BorderStroke(1.dp, SaLightGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = resetMessage ?: "",
                                color = SaLightGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaLightGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SaLightGreen) },
                        modifier = Modifier.fillMaxWidth().testTag("username_input").padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaLightGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = SaLightGreen) },
                        modifier = Modifier.fillMaxWidth().testTag("password_input").padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.login(email.trim(), password) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SECURE LOGIN", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                        Text(
                            text = "OR CONTINUE WITH",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Google Sign-In Button
                        OutlinedButton(
                            onClick = { showGoogleDialog = true },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Facebook Sign-In Button
                        Button(
                            onClick = { showFacebookDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_facebook_logo),
                                contentDescription = "Facebook Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Facebook", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithGoogleSimulation(
                                "mghulam2006@gmail.com",
                                "Ghulam Moheuddin"
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaGold),
                        border = BorderStroke(1.dp, SaGold.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = "Developer Account", tint = SaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant Developer Sign-In (mghulam2006@gmail.com)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Forgot Password? Reset.",
                        color = SaLightGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { viewModel.requestPasswordReset(email) }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "New to Smart Landlord? Create an Account",
                color = SaMint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo("register") }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // MANDATORY SAAS BRAND SIGNATURE FOR GHULAM MOHEUDDIN / GHULAM TECH INFO
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SAAS PRINCIPAL DEVELOPER & INTELLECTUAL OWNER",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "GHULAM MOHEUDDIN (GHULAM TECH INFO)",
                        fontSize = 11.sp,
                        color = SaGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Contact: mghulam2006@gmail.com",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            // --- GOOGLE SIGN IN DIALOG SIMULATION ---
            if (showGoogleDialog) {
                var customEmail by remember { mutableStateOf("") }
                var customName by remember { mutableStateOf("") }
                var isConnecting by remember { mutableStateOf(false) }
                var isAddNew by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { if (!isConnecting) showGoogleDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sign in with Google",
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "to continue to Smart Landlord",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                            if (isConnecting) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 32.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF4285F4), modifier = Modifier.size(44.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (customEmail.isNotEmpty()) "Signing in as ${customEmail}..." else "Connecting to Google Account...",
                                        color = Color.DarkGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (!isAddNew) {
                                Text(
                                    text = "Choose an account",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .padding(vertical = 12.dp)
                                )

                                // GM Account
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            isConnecting = true
                                            customEmail = "mghulam2006@gmail.com"
                                            val scope = kotlinx.coroutines.MainScope()
                                            scope.launch {
                                                delay(1500)
                                                viewModel.loginWithGoogleSimulation("mghulam2006@gmail.com", "Ghulam Moheuddin")
                                                showGoogleDialog = false
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E3A8A),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("GM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Ghulam Moheuddin", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("mghulam2006@gmail.com", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                // DL Account
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            isConnecting = true
                                            customEmail = "demo.landlord@gmail.com"
                                            val scope = kotlinx.coroutines.MainScope()
                                            scope.launch {
                                                delay(1500)
                                                viewModel.loginWithGoogleSimulation("demo.landlord@gmail.com", "Demo Landlord")
                                                showGoogleDialog = false
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("DL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Demo Landlord", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("demo.landlord@gmail.com", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                // Add New Account Clickable
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { isAddNew = true }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Account",
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Use another Google account", color = Color(0xFF1A73E8), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (errorMessage.isNotEmpty()) {
                                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                }

                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Full Name", color = Color.DarkGray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = Color(0xFF1A73E8),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = customEmail,
                                    onValueChange = { customEmail = it },
                                    label = { Text("Google Email Address", color = Color.DarkGray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = Color(0xFF1A73E8),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { isAddNew = false }) {
                                        Text("Back", color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (customName.isBlank() || customEmail.isBlank() || !customEmail.contains("@")) {
                                                errorMessage = "Please enter a valid name and Google email address."
                                            } else {
                                                isConnecting = true
                                                val scope = kotlinx.coroutines.MainScope()
                                                scope.launch {
                                                    delay(1500)
                                                    viewModel.loginWithGoogleSimulation(customEmail.trim(), customName.trim())
                                                    showGoogleDialog = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                                    ) {
                                        Text("Sign In")
                                    }
                                }
                            }

                            if (!isConnecting) {
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(
                                    onClick = { showGoogleDialog = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel", color = Color(0xFF1A73E8))
                                }
                            }
                        }
                    }
                }
            }

            // --- FACEBOOK SIGN IN DIALOG SIMULATION ---
            if (showFacebookDialog) {
                var customEmail by remember { mutableStateOf("") }
                var customName by remember { mutableStateOf("") }
                var isConnecting by remember { mutableStateOf(false) }
                var isAddNew by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { if (!isConnecting) showFacebookDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.5f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1877F2))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.ic_facebook_logo),
                                        contentDescription = "Facebook Logo",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Facebook OAuth Login",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Smart Landlord",
                                    color = Color.Black,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "is requesting access to your public profile & email.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                                )

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                                if (isConnecting) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 32.dp)
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF1877F2), modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (customEmail.isNotEmpty()) "Signing in as ${customEmail}..." else "Authenticating with Facebook...",
                                            color = Color.DarkGray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else if (!isAddNew) {
                                    Text(
                                        text = "Login with simulated accounts:",
                                        color = Color.DarkGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .padding(vertical = 12.dp)
                                    )

                                    // GM Facebook Connected Account
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                isConnecting = true
                                                customEmail = "mghulam2006@gmail.com"
                                                val scope = kotlinx.coroutines.MainScope()
                                                scope.launch {
                                                    delay(1500)
                                                    viewModel.loginWithFacebookSimulation("mghulam2006@gmail.com", "Ghulam Moheuddin")
                                                    showFacebookDialog = false
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1A5276),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("GM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Ghulam Moheuddin", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Text("mghulam2006@gmail.com (FB Associated)", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }

                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                    // JS Facebook Connected Account
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                isConnecting = true
                                                customEmail = "jane.smith@gmail.com"
                                                val scope = kotlinx.coroutines.MainScope()
                                                scope.launch {
                                                    delay(1500)
                                                    viewModel.loginWithFacebookSimulation("jane.smith@gmail.com", "Jane Smith")
                                                    showFacebookDialog = false
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFC0392B),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("JS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Jane Smith", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Text("jane.smith@gmail.com (FB Associated)", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }

                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                    // Add Another Facebook Account Clickable
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isAddNew = true }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Account",
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Sign in with another Facebook profile", color = Color(0xFF1877F2), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    if (errorMessage.isNotEmpty()) {
                                        Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    }

                                    OutlinedTextField(
                                        value = customName,
                                        onValueChange = { customName = it },
                                        label = { Text("Profile Name", color = Color.DarkGray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedBorderColor = Color(0xFF1877F2),
                                            unfocusedBorderColor = Color.Gray
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = customEmail,
                                        onValueChange = { customEmail = it },
                                        label = { Text("Facebook Email Address", color = Color.DarkGray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedBorderColor = Color(0xFF1877F2),
                                            unfocusedBorderColor = Color.Gray
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { isAddNew = false }) {
                                            Text("Back", color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (customName.isBlank() || customEmail.isBlank() || !customEmail.contains("@")) {
                                                    errorMessage = "Please enter a valid name and Facebook email address."
                                                } else {
                                                    isConnecting = true
                                                    val scope = kotlinx.coroutines.MainScope()
                                                    scope.launch {
                                                        delay(1500)
                                                        viewModel.loginWithFacebookSimulation(customEmail.trim(), customName.trim())
                                                        showFacebookDialog = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                                        ) {
                                            Text("Authenticate")
                                        }
                                    }
                                }

                                if (!isConnecting) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(
                                        onClick = { showFacebookDialog = false },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Cancel", color = Color(0xFF1877F2))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------------------
// REGISTER SCREEN
// ---------------------------------------------------------------------
@Composable
fun RegisterScreen(viewModel: SmartLandlordViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val registerError by viewModel.registerError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SaForestGreen, DarkSlate, DarkSlate),
                    startY = 0f,
                    endY = 1200f
                )
            )
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "GET STARTED",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Join South Africa's modern rental management platform.",
                color = SaMint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Developer & Landlord Account Setup",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (registerError != null) {
                        Surface(
                            color = SaRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = registerError ?: "",
                                color = SaRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Agency Name / Landlord Name", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaLightGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Business Email", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaLightGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Secure Password", color = Color.White.copy(alpha = 0.6f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaLightGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.register(name, email, password) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaLightGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CREATE FREE ACCOUNT", color = DarkSlate, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Already have an account? Sign In",
                color = SaMint,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { viewModel.navigateTo("login") }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ---------------------------------------------------------------------
// DASHBOARD SCREEN
// ---------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: SmartLandlordViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val maintenanceRequests by viewModel.maintenanceRequests.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Calculations
    val totalProps = properties.size
    val totalTenants = tenants.size
    val occupiedProps = properties.count { it.status == "Occupied" }
    val occupancyRate = if (totalProps > 0) (occupiedProps.toDouble() / totalProps.toDouble() * 100).toInt() else 0
    
    // Monthly Projected Rent Gathered from Active Occupied Properties
    val monthlyRentalIncome = properties.filter { it.status == "Occupied" }.sumOf { it.monthlyRent }
    
    // Total unpaid balances in system log
    val outstandingRent = payments.filter { it.status == "Unpaid" || it.status == "Partial" }.sumOf { it.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header card
        Surface(
            color = SaForestGreen,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_smart_landlord_logo),
                            contentDescription = "Smart Landlord Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SMART LANDLORD",
                                color = SaMint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = currentUser?.name ?: "Welcome Back",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Subscription Plan Badge
                    Surface(
                        color = SaGold,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = (currentUser?.subscriptionPlan ?: "Free").uppercase() + " PLAN",
                            color = DarkSlate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // GHULAM MOHEUDDIN / DEVELOPMENT MASTER NOTIFIER
                if (currentUser?.email?.lowercase() == "mghulam2006@gmail.com") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = SaGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SaGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo("admin") }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = SaGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MASTER DEV PANEL: Active control over annual subscribers & logs.",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Main Scrolling Body containing Stats Grid & lists
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Statistical Cards Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DashboardStatCard(
                            title = "Total Properties",
                            value = "$totalProps",
                            icon = Icons.Default.Home,
                            iconColor = SaForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Total Tenants",
                            value = "$totalTenants",
                            icon = Icons.Default.People,
                            iconColor = SaLightGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DashboardStatCard(
                            title = "Contract Income",
                            value = viewModel.formatZAR(monthlyRentalIncome),
                            icon = Icons.Default.AttachMoney,
                            iconColor = SaLightGreen,
                            modifier = Modifier.weight(1.2f)
                        )
                        DashboardStatCard(
                            title = "Arrears",
                            value = viewModel.formatZAR(outstandingRent),
                            icon = Icons.Default.Warning,
                            iconColor = SaRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DashboardStatCard(
                        title = "Occupancy Rate",
                        value = "$occupancyRate % occupied ($occupiedProps / $totalProps properties)",
                        icon = Icons.Default.HomeWork,
                        iconColor = SaGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Operations Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo("properties") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SaLightGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = SaForestGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Property", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.navigateTo("tenants") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SaLightGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = SaForestGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Lease", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    }
                }
            }

            // High Priority Maintenance list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Urgent Maintenance Tickets", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "See All",
                        fontSize = 12.sp,
                        color = SaLightGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.navigateTo("maintenance") }
                    )
                }
            }

            val highPriTickets = maintenanceRequests.filter { it.priority == "High" && it.status != "Completed" }
            if (highPriTickets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "No high priority maintenance tasks currently logged.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(highPriTickets) { ticket ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, SaRed.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Urgent", tint = SaRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ticket.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("At ${ticket.propertyName}", fontSize = 11.sp, color = MediumGray)
                            }
                            Surface(color = SaRed.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text("HIGH PRIORITY", color = SaRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // Recent rent schedules list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Rental Statements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Manage Ledger",
                        fontSize = 12.sp,
                        color = SaLightGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.navigateTo("payments") }
                    )
                }
            }

            val recentPayments = payments.take(5)
            if (recentPayments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Rent ledger is empty. Click signed leases to populate schedules.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentPayments) { payment ->
                    RentSassRow(payment, viewModel, tenants)
                }
            }

            // SaaS developer branding signature
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Smart Landlord South Africa v2.4 (SaaS Pro Edition)",
                        fontSize = 10.sp,
                        color = MediumGray
                    )
                    Text(
                        text = "Developed & Maintained by GHULAM TECH INFO Software",
                        fontSize = 9.sp,
                        color = SaForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, LightGray)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ---------------------------------------------------------------------
// RENT / LEDGER ROW COMPOSE ITEM
// ---------------------------------------------------------------------
@Composable
fun RentSassRow(
    payment: Payment,
    viewModel: SmartLandlordViewModel,
    tenantsList: List<Tenant>
) {
    val context = LocalContext.current
    val tenantLinked = tenantsList.find { it.id == payment.tenantId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, LightGray)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(payment.tenantName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(payment.propertyName, fontSize = 11.sp, color = MediumGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Due: ${payment.dueDate}  l  Bal: ${viewModel.formatZAR(payment.balance)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(viewModel.formatZAR(payment.amountDue), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val (statusText, badgeColor, textColor) = when (payment.status) {
                    "Paid" -> Triple("PAID", SaMint, SaForestGreen)
                    "Partial" -> Triple("PARTIAL", SaGold.copy(alpha = 0.2f), SaGold)
                    else -> Triple("UNPAID", SaRed.copy(alpha = 0.15f), SaRed)
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = textColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action: Whatsapp Remind or send receipt
            if (payment.status != "Paid" && tenantLinked != null) {
                IconButton(
                    onClick = {
                        val url = viewModel.getWhatsAppReminderUrl(tenantLinked, payment)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.NotificationAdd, contentDescription = "Send Reminder", tint = SaLightGreen)
                }
            } else if (payment.status == "Paid" && tenantLinked != null) {
                IconButton(
                    onClick = {
                        val url = viewModel.getWhatsAppReceiptUrl(tenantLinked, payment)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Receipt", tint = SaForestGreen)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// PROPERTIES SCREEN
// ---------------------------------------------------------------------
@Composable
fun PropertiesScreen(viewModel: SmartLandlordViewModel) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        SmartLandlordScreenHeader(
            title = "Properties",
            subtitle = "SaaS Asset Tracker"
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Property", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Limit banner
        val plan = currentUser?.subscriptionPlan ?: "Free"
        val count = properties.size
        val (limitText, color) = when (plan) {
            "Free" -> "FREE SUBSCRIPTION: 1 Property maximum allowed ($count / 1 created)" to SaGold
            "Basic" -> "BASIC SYSTEM PLAN: 20 Properties maximum ($count / 20 created)" to SaLightGreen
            else -> "ENTERPRISE PRO PLAN: Unlimited properties registered ($count created)" to SaForestGreen
        }

        Surface(
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = limitText,
                color = if (plan == "Free") SaGold else MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }

        if (properties.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.HomeWork, contentDescription = null, tint = MediumGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Properties Registered", fontWeight = FontWeight.Bold)
                    Text("You can register new properties by clicking the add button.", fontSize = 11.sp, color = MediumGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(properties) { prop ->
                    PropertyCard(prop, viewModel)
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddPropertyDialog(viewModel = viewModel) { showAddDialog = false }
    }
}

@Composable
fun PropertyCard(property: Property, viewModel: SmartLandlordViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, LightGray)
    ) {
        Column {
            // Optional image load using AsyncImage
            AsyncImage(
                model = property.imageUri,
                contentDescription = property.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = property.type.uppercase(),
                        color = SaLightGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    
                    val pillColor = if (property.status == "Occupied") SaMint else SaGold.copy(alpha = 0.2f)
                    val pillTextColor = if (property.status == "Occupied") SaForestGreen else SaGold

                    Surface(color = pillColor, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            property.status,
                            color = pillTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(property.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, contentDescription = "Address", tint = MediumGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(property.address, fontSize = 11.sp, color = MediumGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LightGray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Monthly Rent", fontSize = 10.sp, color = MediumGray)
                        Text(viewModel.formatZAR(property.monthlyRent), fontWeight = FontWeight.Bold)
                    }

                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = SaForestGreen)
                        }
                        IconButton(onClick = { viewModel.deletePropertyDetails(property.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete asset", tint = SaRed)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditPropertyDialog(property = property, viewModel = viewModel) { showEditDialog = false }
    }
}

// ---------------------------------------------------------------------
// TENANTS SCREEN
// ---------------------------------------------------------------------
@Composable
fun TenantsScreen(viewModel: SmartLandlordViewModel) {
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmartLandlordScreenHeader(
            title = "Tenants",
            subtitle = "Active Lease Agreements"
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Lease", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (tenants.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MediumGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Tenants Registered", fontWeight = FontWeight.Bold)
                    Text("Sign lease contracts to populate property tenants.", fontSize = 11.sp, color = MediumGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tenants) { tenant ->
                    TenantRow(tenant, viewModel)
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddTenantDialog(viewModel = viewModel, properties = properties) { showAddDialog = false }
    }
}

@Composable
fun TenantRow(tenant: Tenant, viewModel: SmartLandlordViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectTenantAndNavigate(tenant.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, LightGray)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = SaMint),
                modifier = Modifier.size(45.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        tenant.fullName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = SaForestGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(tenant.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Home: ${tenant.propertyName.ifBlank { "Unassigned" }}", fontSize = 11.sp, color = MediumGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Cell: ${tenant.phoneNumber}", fontSize = 11.sp, color = MediumGray)
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = SaForestGreen
            )
        }
    }
}

// ---------------------------------------------------------------------
// TENANT PROFILE DETAILED SCREEN
// ---------------------------------------------------------------------
@Composable
fun TenantProfileScreen(viewModel: SmartLandlordViewModel) {
    val selectedId by viewModel.selectedTenantId.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val tenant = tenants.find { it.id == selectedId }
    
    if (tenant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tenant record not found")
        }
        return
    }

    val linkedProperty = properties.find { it.id == tenant.propertyId }
    val tenantPayments = payments.filter { it.tenantId == tenant.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Simple header back navigation
        Surface(color = SaForestGreen, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("tenants") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tenant Dossier", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card details
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.5.dp, LightGray)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            shape = CircleShape,
                            modifier = Modifier.size(70.dp),
                            colors = CardDefaults.cardColors(containerColor = SaMint)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(tenant.fullName.take(2).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SaForestGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(tenant.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Business Tenant Passport File", fontSize = 11.sp, color = MediumGray)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phoneNumber}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Cell", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${tenant.emailAddress}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Mail, contentDescription = "Email", tint = SaForestGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Dossier items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, LightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dossier & Agreements", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = LightGray)

                        DossierItem(label = "South African ID / Passport", value = tenant.idPassportNumber)
                        DossierItem(label = "Emergency Support Name/Cell", value = tenant.emergencyContact)
                        DossierItem(label = "Lease Start Date", value = tenant.leaseStartDate)
                        DossierItem(label = "Lease Expiry Date", value = tenant.leaseEndDate)
                        DossierItem(label = "Registered Premises", value = tenant.propertyName.ifBlank { "None assigned" })
                    }
                }
            }

            // Payment tracking dossier
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rental Ledger (Past Bills)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            if (tenantPayments.isEmpty()) {
                item {
                    Text("No billing record exists currently.", fontSize = 11.sp, color = MediumGray)
                }
            } else {
                items(tenantPayments) { pay ->
                    RentSassRow(payment = pay, viewModel = viewModel, tenantsList = tenants)
                }
            }

            // Action: Cancel lease agreement
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.deleteTenantRecord(tenant.id)
                        viewModel.navigateTo("tenants")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SaRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CANCEL LEASE & DELETE RECORDS", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DossierItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MediumGray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------
// PAYMENTS & RECEITING LEDGER
// ---------------------------------------------------------------------
@Composable
fun PaymentsScreen(viewModel: SmartLandlordViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    var showRecordDialog by remember { mutableStateOf(false) }
    var selectedPaymentRecord by remember { mutableStateOf<Payment?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmartLandlordScreenHeader(
            title = "Rent Ledger",
            subtitle = "South African Rand (ZAR)"
        ) {
            // Stats summary in header
        }

        if (payments.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = MediumGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Billing Ledger Logs", fontWeight = FontWeight.Bold)
                    Text("Schedules are auto-populated when you register a tenant lease.", fontSize = 11.sp, color = MediumGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(payments) { pay ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (pay.status != "Paid") {
                                selectedPaymentRecord = pay
                                showRecordDialog = true
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, LightGray)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(pay.tenantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Unit: ${pay.propertyName}", fontSize = 11.sp, color = MediumGray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(viewModel.formatZAR(pay.amountDue), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Due: ${pay.dueDate}", fontSize = 11.sp, color = SaRed)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (statusText, color) = when (pay.status) {
                                        "Paid" -> "PAID l receipt ${pay.receiptNo}" to SaLightGreen
                                        "Partial" -> "PARTIALLY PAID" to SaGold
                                        else -> "UNPAID" to SaRed
                                    }
                                    Icon(
                                        if (pay.status == "Paid") Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(statusText, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Received: ${viewModel.formatZAR(pay.amountPaid)}", fontSize = 11.sp)
                                    if (pay.balance > 0) {
                                        Text("Balance: ${viewModel.formatZAR(pay.balance)}", color = SaRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (pay.status != "Paid") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = SaForestGreen.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = SaForestGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("TAP TO RECORD TENANT CASH/EFT PAYMENT", fontWeight = FontWeight.Bold, color = SaForestGreen, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                // Shared receipts
                                val tenantLinked = tenants.find { it.id == pay.tenantId }
                                if (tenantLinked != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val contextLocal = LocalContext.current
                                    OutlinedButton(
                                        onClick = {
                                            val url = viewModel.getWhatsAppReceiptUrl(tenantLinked, pay)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            contextLocal.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Share", tint = SaForestGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SEND RECEIPT TO TENANT BY WHATSAPP", fontSize = 10.sp, color = SaForestGreen)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (showRecordDialog && selectedPaymentRecord != null) {
        RecordPaymentDialog(
            payment = selectedPaymentRecord!!,
            viewModel = viewModel,
            onDismiss = { showRecordDialog = false }
        )
    }
}

// ---------------------------------------------------------------------
// MAINTENANCE SCREEN
// ---------------------------------------------------------------------
@Composable
fun MaintenanceScreen(viewModel: SmartLandlordViewModel) {
    val maintenanceRequests by viewModel.maintenanceRequests.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmartLandlordScreenHeader(
            title = "Maintenance",
            subtitle = "Active repair logs and priority tickets"
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen)
            ) {
                Icon(Icons.Default.Build, contentDescription = "Logger")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (maintenanceRequests.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Handyman, contentDescription = null, tint = MediumGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Maintenance Tickets Logged", fontWeight = FontWeight.Bold)
                    Text("Log repairs, damaged assets, plumbing issues quickly.", fontSize = 11.sp, color = MediumGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(maintenanceRequests) { req ->
                    MaintenanceCard(req, viewModel)
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddMaintenanceDialog(viewModel = viewModel, properties = properties) { showAddDialog = false }
    }
}

@Composable
fun MaintenanceCard(request: MaintenanceRequest, viewModel: SmartLandlordViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, LightGray)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Premises: ${request.propertyName}", color = MediumGray, fontSize = 11.sp)
                }

                val colorPriority = when (request.priority) {
                    "High" -> SaRed
                    "Medium" -> SaGold
                    else -> SaLightGreen
                }
                Surface(color = colorPriority.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        request.priority.uppercase() + " PRIORITY",
                        color = colorPriority,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(request.description, fontSize = 12.sp)

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = LightGray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (request.status) {
                        "Completed" -> SaLightGreen
                        "In Progress" -> SaGold
                        else -> MediumGray
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(request.status, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 12.sp)
                }

                Row {
                    if (request.status != "Completed") {
                        IconButton(onClick = {
                            val updatedStatus = if (request.status == "Open") "In Progress" else "Completed"
                            viewModel.updateMaintenanceDetails(request.copy(status = updatedStatus))
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Step Status", tint = SaForestGreen)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteMaintenanceRecord(request.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete issue", tint = SaRed)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// REPORTS SCREEN (Income statements, Arrears sheets & Excel Export Simulation)
// ---------------------------------------------------------------------
@Composable
fun ReportsScreen(viewModel: SmartLandlordViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Calculated metrics
    val totalProps = properties.size
    val occupiedProps = properties.count { it.status == "Occupied" }
    val totalRentsReceived = payments.filter { it.status == "Paid" || it.status == "Partial" }.sumOf { it.amountPaid }
    val totalArrears = payments.sumOf { it.balance }
    
    val annualProjection = properties.filter { it.status == "Occupied" }.sumOf { it.monthlyRent } * 12.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmartLandlordScreenHeader(
            title = "SaaS Reports",
            subtitle = "South Africa property yield analytical sheet"
        ) {
            Button(
                onClick = {
                    // Export CSV simulated share intent
                    val csvText = "Smart Landlord SaaS Core Audit Sheet\n" +
                            "Registered Properties: $totalProps\n" +
                            "Active Tenants: ${tenants.size}\n" +
                            "Occupancy Rate: l ${if (totalProps > 0) (occupiedProps*100/totalProps) else 0}%\n" +
                            "Total Rental Money Recovered: R $totalRentsReceived\n" +
                            "Outstanding Rents (Arrears): R $totalArrears\n" +
                            "Generated: 2026-06-05 (Smart Landlord Ltd admin)"
                    
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csvText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Export Report to Excel/PDF")
                    context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaLightGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel/CSV", fontSize = 11.sp, color = DarkSlate, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.1.dp, SaLightGreen)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Operational Dashboard Yield", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))

                        FinanceMetricsRow(label = "Direct Rent Money Collected", value = viewModel.formatZAR(totalRentsReceived), color = SaForestGreen)
                        FinanceMetricsRow(label = "Outstanding Tenant Arrears", value = viewModel.formatZAR(totalArrears), color = SaRed)
                        FinanceMetricsRow(label = "SaaS Pro Annual Projection", value = viewModel.formatZAR(annualProjection), color = SaLightGreen)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = LightGray)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Portfolio Occupancy Ratio", fontSize = 12.sp, color = MediumGray)
                            Text("$occupiedProps of $totalProps Occupied", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Arrears Tracking Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            val outstandingLogs = payments.filter { it.balance > 0 }
            if (outstandingLogs.isEmpty()) {
                item {
                    Text("Congratulations! There are no unpaid outstanding tenants rents logged in ledger.", color = SaLightGreen, fontSize = 12.sp)
                }
            } else {
                items(outstandingLogs) { pay ->
                    RentSassRow(payment = pay, viewModel = viewModel, tenantsList = tenants)
                }
            }
        }
    }
}

@Composable
fun FinanceMetricsRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MediumGray)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------------------------------------------------------------------
// DEVELOPER CENTRAL SUBSCRIPTION ADMIN CONTROLLER (GHULAM MOHEUDDIN)
// ---------------------------------------------------------------------
@Composable
fun DeveloperAdminScreen(viewModel: SmartLandlordViewModel) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val systemLogs by viewModel.systemLogs.collectAsStateWithLifecycle()
    
    var selectedSubscriberEmail by remember { mutableStateOf("") }
    var selectedPlanToGrant by remember { mutableStateOf("Pro") }
    var inputYearsToGrant by remember { mutableStateOf("1") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlate)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SaGold)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Developer Core", tint = SaGold, modifier = Modifier.size(50.dp))
                    Text(
                        text = "DEVELOPER SYSTEM ADMIN CONSOLE",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Authorized Signature: GHULAM MOHEUDDIN / GHULAM TECH INFO\nmghulam2006@gmail.com",
                        color = SaGold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Subscriptions Controller Module
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Control Tenant/Landlord Subscriptions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = selectedSubscriberEmail,
                        onValueChange = { selectedSubscriberEmail = it },
                        label = { Text("Landlord Email to Configure", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grant Plan:", color = Color.White, fontSize = 11.sp)
                        listOf("Free", "Basic", "Pro").forEach { plan ->
                            val isSelected = selectedPlanToGrant == plan
                            Button(
                                onClick = { selectedPlanToGrant = plan },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) SaGold else Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(plan, color = if (isSelected) DarkSlate else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputYearsToGrant,
                        onValueChange = { inputYearsToGrant = it },
                        label = { Text("Contract Validity Years (Yearly Sub)", color = Color.White.copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SaGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    Button(
                        onClick = {
                            if (selectedSubscriberEmail.isNotBlank()) {
                                viewModel.adminControlUserSubscription(
                                    selectedSubscriberEmail.trim().lowercase(),
                                    selectedPlanToGrant,
                                    inputYearsToGrant.toIntOrNull() ?: 1
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaGold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("APPLY SYSTEM CONFIG & OVERRIDE SUBSCRIPTION", color = DarkSlate, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // List subscribers in grid
        item {
            Text("Subscribers Registered Records", color = SaGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(allUsers) { subscriber ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.6f)),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().clickable {
                    selectedSubscriberEmail = subscriber.email
                    selectedPlanToGrant = subscriber.subscriptionPlan
                }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SaGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subscriber.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(subscriber.email, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("Active Plan: ${subscriber.subscriptionPlan.uppercase()}", color = SaLightGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Expires: ${viewModel.formatDate(subscriber.subEndDate)}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        }

        // Activities telemetry logs
        item {
            Text("System Database Audit Logs", color = SaGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(systemLogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = log.email, color = SaMint, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(text = log.action, color = Color.White, fontSize = 11.sp)
                    Text(text = viewModel.formatDate(log.timestamp), color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ---------------------------------------------------------------------
// SUBSCRIPTIONS SYSTEM TABS & SAAS PLAN COMPARISON
// ---------------------------------------------------------------------
@Composable
fun SubscriptionsScreen(viewModel: SmartLandlordViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SmartLandlordScreenHeader(
            title = "Upgrade SaaS Services",
            subtitle = "Choose custom packages tailored for South Africa property portfolios"
        ) {}

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Plan COMPARISON 1: FREE
            PlanCard(
                title = "Free Starter Plan",
                price = "R 0 / month",
                duration = "Free Forever",
                features = listOf(
                    "Register 1 Property Assets maximum",
                    "Sign lease records for up to 5 Tenants",
                    "Record basic collection ledger payments",
                    "Simulated receipt exports & logs"
                ),
                isSelected = currentUser?.subscriptionPlan == "Free",
                onSelectPlan = { viewModel.purchaseSelfSaaSPlan("Free") }
            )

            // Plan COMPARISON 2: BASIC
            PlanCard(
                title = "Basic Growth Business",
                price = "R 249 / month",
                duration = "Yearly billing available",
                features = listOf(
                    "Manage up to 20 Properties portfolio",
                    "Unlimited signed Tenants leases",
                    "EFT/Cash/Card payments tracking logs",
                    "Instant WhatsApp Rent Reminders",
                    "Instant WhatsApp Official receipts"
                ),
                isSelected = currentUser?.subscriptionPlan == "Basic",
                onSelectPlan = { viewModel.purchaseSelfSaaSPlan("Basic") }
            )

            // Plan COMPARISON 3: PRO ENTERPRISE
            PlanCard(
                title = "Smart Portfolio Pro",
                price = "R 599 / month",
                duration = "Yearly billing (control available)",
                features = listOf(
                    "Unlimited properties registered",
                    "Unlimited Tenants lease tracking files",
                    "Instant WhatsApp automation messages",
                    "PDF & Excel simulated analytical yields exports",
                    "Urgent maintenance ticket prioritizer",
                    "Developer VIP direct advisory channel"
                ),
                isSelected = currentUser?.subscriptionPlan == "Pro",
                onSelectPlan = { viewModel.purchaseSelfSaaSPlan("Pro") }
            )
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    duration: String,
    features: List<String>,
    isSelected: Boolean,
    onSelectPlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 0.5.dp, if (isSelected) SaForestGreen else LightGray),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(duration, fontSize = 11.sp, color = MediumGray)
                }
                if (isSelected) {
                    Surface(color = SaForestGreen, shape = RoundedCornerShape(8.dp)) {
                        Text("ACTIVE PLAN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(price, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SaForestGreen)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = LightGray)

            features.forEach { feat ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SaLightGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feat, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSelectPlan,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) SaLightGreen else SaForestGreen)
            ) {
                Text(if (isSelected) "CURRENT SUBSCRIBED SERVICE" else "ACTIVATE SaaS PLAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------------
// GENERAL GLOBAL EXPANSIBLE POPUPS & SHEETS
// ---------------------------------------------------------------------
@Composable
fun SmartLandlordScreenHeader(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit = {}
) {
    Surface(
        color = SaForestGreen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(subtitle, color = SaMint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

// Dialog: Add Property
@Composable
fun AddPropertyDialog(
    viewModel: SmartLandlordViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectType by remember { mutableStateOf("Apartment") }
    var rent by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var descNotes by remember { mutableStateOf("") }

    val types = listOf("Apartment", "House", "Commercial", "Townhouse")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Register Property Asset", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Property Name l Apt Number") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Text("Property Asset Type:", fontSize = 11.sp, color = MediumGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { t ->
                        val isSelected = selectType == t
                        Button(
                            onClick = { selectType = t },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) SaForestGreen else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(t, fontSize = 9.sp, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it },
                    label = { Text("Monthly Rental Price (ZAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Physical Asset Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = descNotes, onValueChange = { descNotes = it }, label = { Text("Property Notes") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && rent.isNotBlank() && address.isNotBlank()) {
                                viewModel.createProperty(
                                    name = name,
                                    type = selectType,
                                    address = address,
                                    monthlyRent = rent.toDoubleOrNull() ?: 1000.0,
                                    imageUri = "",
                                    notes = descNotes
                                ) { success -> if (success) onDismiss() }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

// Dialog: Add Edit Property
@Composable
fun EditPropertyDialog(
    property: Property,
    viewModel: SmartLandlordViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(property.name) }
    var rent by remember { mutableStateOf(property.monthlyRent.toString()) }
    var address by remember { mutableStateOf(property.address) }
    var descNotes by remember { mutableStateOf(property.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Edit Property Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Property Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it },
                    label = { Text("Monthly Rent Val (ZAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Property Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = descNotes, onValueChange = { descNotes = it }, label = { Text("Notes Details") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            viewModel.updatePropertyDetails(
                                property.copy(
                                    name = name,
                                    monthlyRent = rent.toDoubleOrNull() ?: property.monthlyRent,
                                    address = address,
                                    notes = descNotes
                                )
                            ) { onDismiss() }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen)
                    ) { Text("Save") }
                }
            }
        }
    }
}

// Dialog: Add Tenant Lease contract
@Composable
fun AddTenantDialog(
    viewModel: SmartLandlordViewModel,
    properties: List<Property>,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var idDoc by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var leaseStart by remember { mutableStateOf("2026-06-01") }
    var leaseEnd by remember { mutableStateOf("2027-05-31") }
    var emergency by remember { mutableStateOf("") }
    
    var selectedPropertyIndex by remember { mutableStateOf(-1) }

    val vacantProperties = properties.filter { it.status == "Vacant" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Sign New Tenant Lease", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Tenant Full Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = idDoc, onValueChange = { idDoc = it }, label = { Text("Passport / SA National ID") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Cellphone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = leaseStart, onValueChange = { leaseStart = it }, label = { Text("Lease Start (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = leaseEnd, onValueChange = { leaseEnd = it }, label = { Text("Lease Expiry (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = emergency, onValueChange = { emergency = it }, label = { Text("Emergency Name & Cell") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                Text("Allocate Vacant Premises:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaForestGreen)
                if (vacantProperties.isEmpty()) {
                    Text("No vacant properties found in inventory. Register properties first.", color = SaRed, fontSize = 11.sp)
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        vacantProperties.forEachIndexed { i, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPropertyIndex = i }
                                    .background(if (selectedPropertyIndex == i) SaMint.copy(alpha = 0.3f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedPropertyIndex == i, onClick = { selectedPropertyIndex = i })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${p.name} (R ${p.monthlyRent})", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && phone.isNotBlank() && selectedPropertyIndex >= 0) {
                                val prop = vacantProperties[selectedPropertyIndex]
                                viewModel.createTenant(
                                    fullName = fullName,
                                    idPassport = idDoc,
                                    phone = phone,
                                    email = email,
                                    start = leaseStart,
                                    end = leaseEnd,
                                    emergency = emergency,
                                    propertyId = prop.id,
                                    propertyName = prop.name
                                ) { success -> if (success) onDismiss() }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

// Dialog: Add Maintenance ticketing issue
@Composable
fun AddMaintenanceDialog(
    viewModel: SmartLandlordViewModel,
    properties: List<Property>,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var selectedPropIdx by remember { mutableStateOf(0) }

    val priorities = listOf("Low", "Medium", "High")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Text("Log Repair Ticket", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Issue Title (e.g., Leaking plumbing)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Provide Damage Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Text("Priority Level:", fontSize = 11.sp, color = MediumGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    priorities.forEach { pri ->
                        val isSel = priority == pri
                        Button(
                            onClick = { priority = pri },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSel) SaForestGreen else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(pri, fontSize = 10.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Select Property Premise:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (properties.isEmpty()) {
                    Text("No registered premises.", color = SaRed, fontSize = 11.sp)
                } else {
                    Column {
                        properties.forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPropIdx = idx }
                                    .background(if (selectedPropIdx == idx) SaMint.copy(alpha = 0.3f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedPropIdx == idx, onClick = { selectedPropIdx = idx })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(p.name, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && properties.isNotEmpty()) {
                                viewModel.createMaintenanceRequest(
                                    title = title,
                                    desc = desc,
                                    propertyId = properties[selectedPropIdx].id,
                                    propertyName = properties[selectedPropIdx].name,
                                    priority = priority,
                                    imageUri = ""
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                        modifier = Modifier.weight(1f)
                    ) { Text("Create Log") }
                }
            }
        }
    }
}

// Dialog: Rent recording sheet popup
@Composable
fun RecordPaymentDialog(
    payment: Payment,
    viewModel: SmartLandlordViewModel,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(payment.balance.toString()) }
    var paymentMethod by remember { mutableStateOf("EFT") }
    val methods = listOf("EFT", "Cash", "Card")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Record Rent Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Receipt: ${payment.tenantName} - ${payment.propertyName}", fontSize = 11.sp, color = MediumGray)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Paid l Received (ZAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Payment Method Option:", fontSize = 11.sp, color = MediumGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.forEach { m ->
                        val isSelected = paymentMethod == m
                        Button(
                            onClick = { paymentMethod = m },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) SaForestGreen else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(m, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val today = dateFormat.format(Date())
                                viewModel.logTenantRentReceipt(
                                    paymentId = payment.id,
                                    amountPaid = amt,
                                    method = paymentMethod,
                                    date = today
                                ) { onDismiss() }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaForestGreen),
                        modifier = Modifier.weight(1f)
                    ) { Text("Paid") }
                }
            }
        }
    }
}
